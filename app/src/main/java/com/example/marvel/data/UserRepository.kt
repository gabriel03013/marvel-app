package com.example.marvel.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun profile(user: FirebaseUser) {
        db.collection("users").document(user.uid).set(mapOf("displayName" to (user.displayName ?: "Agent"), "email" to user.email.orEmpty(), "photoUrl" to user.photoUrl?.toString().orEmpty(), "lastSeenAt" to System.currentTimeMillis()), SetOptions.merge()).awaitResult()
    }
    fun observe(uid: String, collection: String, onResult: (List<Pair<String, Map<String, Any>>>, Boolean, Boolean, String?) -> Unit): ListenerRegistration =
        db.collection("users").document(uid).collection(collection).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) onResult(emptyList(), false, false, "Your collection could not sync. Check your connection or Firestore permissions.")
            else if (snapshot != null) onResult(snapshot.documents.map { it.id to it.data.orEmpty() }, snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites(), null)
        }
    suspend fun favorite(uid: String, item: ArchiveItem, remove: Boolean) {
        val ref = db.collection("users").document(uid).collection("favorites").document(item.id.toString())
        if (remove) ref.delete().awaitResult() else ref.set(item.map()).awaitResult()
    }
    suspend fun saveTeam(uid: String, team: SavedTeam) {
        val root = db.collection("users").document(uid)
        val batch = db.batch()
        batch.set(root.collection("teams").document(team.id), team.map())
        batch.set(root.collection("missions").document(team.id), team.map())
        batch.commit().awaitResult()
    }
    suspend fun deleteTeam(uid: String, id: String) {
        // Mission history retains the completed report, independently of saved teams.
        db.collection("users").document(uid).collection("teams").document(id).delete().awaitResult()
    }
}
