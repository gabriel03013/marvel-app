package com.example.marvel.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.marvel.R
import com.example.marvel.data.BoosterType

fun ScreenRenderer.boosterArt(
    type: BoosterType,
    opened: Boolean,
    height: Int = 168,
): ImageView {
    val (sealedPack, openedPack) =
        when (type.id) {
            "street" -> R.drawable.booster_street_closed to R.drawable.booster_street_open
            "cosmic" -> R.drawable.booster_cosmic_closed to R.drawable.booster_cosmic_open
            "mutant" -> R.drawable.booster_mutant_closed to R.drawable.booster_mutant_open
            "tech" -> R.drawable.booster_tech_closed to R.drawable.booster_tech_open
            "legendary" -> R.drawable.booster_legendary_closed to R.drawable.booster_legendary_open
            else -> error("Unknown booster type: ${type.id}")
        }

    return ImageView(activity).apply {
        setImageResource(if (opened) openedPack else sealedPack)
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = "${type.title} ${if (opened) "opened" else "sealed"} booster pack"
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height))
    }
}

fun ScreenRenderer.boosterTile(type: BoosterType, inventory: Long) {
    val tile =
        LinearLayout(activity).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    .apply { bottomMargin = dp(16) }
            background = CollageSurface(activity, "paper")
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(boosterArt(type, opened = false, height = 180))
            addView(
                TextView(activity).apply {
                    text = type.title
                    textSize = 24f
                    typeface = activity.resources.getFont(R.font.oswald)
                    setTextColor(color(R.color.ink_black))
                    isAccessibilityHeading = true
                }
            )
            addView(
                TextView(activity).apply {
                    text = type.description
                    textSize = 14f
                    setLineSpacing(dp(3).toFloat(), 1f)
                    setTextColor(color(R.color.muted_ink))
                }
            )
            addView(
                TextView(activity).apply {
                    text =
                        activity.resources.getQuantityString(
                            R.plurals.booster_card_count_owned,
                            type.cardCount,
                            type.cardCount,
                            inventory,
                        )
                    textSize = 14f
                    setTextColor(color(R.color.muted_ink))
                    minHeight = dp(48)
                    gravity = Gravity.CENTER_VERTICAL
                    contentDescription =
                        activity.resources.getQuantityString(
                            R.plurals.booster_card_count_accessibility,
                            type.cardCount,
                            type.cardCount,
                            inventory,
                        )
                }
            )
        }

    content.addView(tile)
    block<Button>(R.layout.block_button, tile).apply {
        text =
            activity.getString(
                if (vm.boosterLoading) R.string.booster_add_wait else R.string.booster_add_free
            )
        isEnabled = !vm.boosterLoading
        setOnClickListener { vm.addBooster(type) }
    }
    block<Button>(R.layout.block_button, tile).apply {
        text = activity.getString(R.string.booster_open, type.title)
        setBackgroundResource(R.drawable.button_red)
        setTextColor(color(R.color.warm_white))
        isEnabled = !vm.boosterLoading && inventory > 0
        alpha = if (isEnabled) 1f else .55f
        setOnClickListener { vm.selectBooster(type) }
    }
}
