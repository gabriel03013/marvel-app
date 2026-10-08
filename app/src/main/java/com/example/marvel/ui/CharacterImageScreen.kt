package com.example.marvel.ui

import android.view.View
import com.example.marvel.data.*

fun ScreenRenderer.imageViewer() {
    val item = vm.details["${vm.route.kind}/${vm.route.id}"]?.value
    title(item?.name ?: "Archive image")
    if (item == null) {
        state("Image unavailable", "Open the record again to load its image.")
        actionRow("Return to dossier") { vm.back() }
        return
    }
    val status =
        text("Loading image…").apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
    val retry = button("Retry image", primary = true) {}.apply { visibility = View.GONE }
    val update: (ArchiveImageState) -> Unit = { state ->
        status.visibility = if (state == ArchiveImageState.READY) View.GONE else View.VISIBLE
        status.text =
            when (state) {
                ArchiveImageState.LOADING -> "Loading image…"
                ArchiveImageState.MISSING -> "No image available for this record."
                ArchiveImageState.FAILED ->
                    "Could not load this image. Check your connection and retry."
                ArchiveImageState.READY -> ""
            }
        retry.visibility = if (state == ArchiveImageState.FAILED) View.VISIBLE else View.GONE
    }
    val picture = image(item, onState = update)
    retry.setOnClickListener { loadImage(picture, item, update) }
    note("Image supplied by Comic Vine.")
    actionRow("Return to dossier") { vm.back() }
}
