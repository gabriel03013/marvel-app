package com.example.marvel.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.ImageView
import com.example.marvel.R

class CollageArtView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ImageView(context, attrs) {
    var variant: Int = 0
        set(value) {
            field = value
            setImageResource(
                when (value) {
                    1 -> R.drawable.collage_paper_label
                    2 -> R.drawable.collage_cosmic
                    else -> R.drawable.collage_heroes
                }
            )
        }

    init {
        scaleType = ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        variant = 0
    }
}

class CollageSurface(
    context: Context,
    tone: String = "paper",
    @Suppress("UNUSED_PARAMETER") variant: Int = 0,
) : GradientDrawable() {
    init {
        setColor(
            context.getColor(
                when (tone) {
                    "red" -> R.color.archive_red
                    "blue" -> R.color.archive_blue
                    "yellow" -> R.color.comic_yellow
                    "dark" -> R.color.archive_black
                    else -> R.color.warm_white
                }
            )
        )
        cornerRadius = 8 * context.resources.displayMetrics.density
    }
}
