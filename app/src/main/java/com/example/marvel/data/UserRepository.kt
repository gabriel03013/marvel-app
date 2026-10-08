package com.example.marvel.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun updateDisplayName(user: FirebaseUser, displayName: String) {
        val request = UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        user.updateProfile(request).awaitResult()
        user.reload().awaitResult()
        profile(user)
    }

    suspend fun profile(user: FirebaseUser) {
        db.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "displayName" to (user.displayName ?: "Agent"),
                    "email" to user.email.orEmpty(),
                    "photoUrl" to user.photoUrl?.toString().orEmpty(),
                    "lastSeenAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            )
            .awaitResult()
    }

    fun observe(
        uid: String,
        collection: String,
        onResult: (List<Pair<String, Map<String, Any>>>, Boolean, Boolean, String?) -> Unit,
    ): ListenerRegistration =
        db.collection("users").document(uid).collection(collection).addSnapshotListener(
            MetadataChanges.INCLUDE
        ) { snapshot, error ->
            if (error != null)
                onResult(
                    emptyList(),
                    false,
                    false,
                    "Your collection could not sync. Check your connection or Firestore permissions.",
                )
            else if (snapshot != null)
                onResult(
                    snapshot.documents.map { it.id to it.data.orEmpty() },
                    snapshot.metadata.isFromCache,
                    snapshot.metadata.hasPendingWrites(),
                    null,
                )
        }

    suspend fun favorite(uid: String, item: ArchiveItem, remove: Boolean) {
        val ref =
            db.collection("users")
                .document(uid)
                .collection("favorites")
                .document(item.id.toString())
        if (remove) ref.delete().awaitResult() else ref.set(item.map()).awaitResult()
    }

    suspend fun addBooster(uid: String, type: BoosterType) {
        boosterReference(uid, type)
            .set(
                mapOf(
                    "count" to FieldValue.increment(1),
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            )
            .awaitResult()
    }

    suspend fun openBooster(
        uid: String,
        type: BoosterType,
        cards: List<ArchiveItem>,
    ): Map<Int, Long> {
        validateBoosterCards(type, cards)

        val boosterRef = boosterReference(uid, type)
        val cardReferences = cards.associateWith { item -> collectedCharacterReference(uid, item) }
        return db.runTransaction { transaction ->
                val boosterSnapshot = transaction.get(boosterRef)
                val count = boosterSnapshot.getLong("count") ?: 0L
                check(count > 0L) {
                    "No ${type.title} boosters are available. Add a booster and try again."
                }

                val savedCards = cardReferences.mapValues { (_, reference) ->
                    transaction.get(reference)
                }

                val updatedAt = System.currentTimeMillis()
                transaction.set(
                    boosterRef,
                    mapOf("count" to count - 1L, "updatedAt" to updatedAt),
                    SetOptions.merge(),
                )

                cardReferences
                    .map { (item, reference) ->
                        val snapshot = savedCards.getValue(item)
                        val copies = (snapshot.getLong("copies") ?: 0L) + 1L
                        val level = collectionLevel(copies)
                        val firstPackId = snapshot.getString("firstPackId") ?: type.id

                        transaction.set(
                            reference,
                            item.map() +
                                mapOf(
                                    "copies" to copies,
                                    "level" to level,
                                    "firstPackId" to firstPackId,
                                    "updatedAt" to updatedAt,
                                ),
                            SetOptions.merge(),
                        )
                        item.id to level
                    }
                    .toMap()
            }
            .awaitResult()
    }

    private fun validateBoosterCards(type: BoosterType, cards: List<ArchiveItem>) {
        require(cards.size == type.cardCount && cards.all { it.kind == "character" && it.id > 0 }) {
            "The character pull is incomplete. Retry before opening another pack."
        }
        require(cards.map { it.id }.distinct().size == cards.size) {
            "This pull contains duplicate cards. Retry the opening."
        }
    }

    private fun boosterReference(uid: String, type: BoosterType) =
        db.collection("users").document(uid).collection("boosters").document(type.id)

    private fun collectedCharacterReference(uid: String, item: ArchiveItem) =
        db.collection("users")
            .document(uid)
            .collection("collected_characters")
            .document(item.id.toString())

    private fun collectionLevel(copies: Long): Long = 1L + ((copies - 1L) / COPIES_PER_LEVEL)

    suspend fun saveTeam(uid: String, team: SavedTeam) {
        val root = db.collection("users").document(uid)
        val batch = db.batch()
        batch.set(root.collection("teams").document(team.id), team.map())
        batch.set(root.collection("missions").document(team.id), team.map())
        batch.commit().awaitResult()
    }

    suspend fun deleteTeam(uid: String, id: String) {
        db.collection("users").document(uid).collection("teams").document(id).delete().awaitResult()
    }

    private companion object {
        const val COPIES_PER_LEVEL = 3L
    }
}
