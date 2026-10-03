package com.example.marvel.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.example.marvel.R

/** Draws individual pieces from the supplied transparent atlases, retaining their real edges. */
private object CollageAssets {
    private val cache = mutableMapOf<Int, Bitmap>()
    fun bitmap(context: Context, id: Int): Bitmap = cache.getOrPut(id) {
        BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })
    }
    fun piece(context: Context, canvas: Canvas, id: Int, crop: RectF, target: RectF, alpha: Int = 255) {
        val bitmap = bitmap(context, id)
        val source = Rect((crop.left * bitmap.width).toInt(), (crop.top * bitmap.height).toInt(),
            (crop.right * bitmap.width).toInt(), (crop.bottom * bitmap.height).toInt())
        canvas.drawBitmap(bitmap, source, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha })
    }
}

class CollageArtView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var variant: Int = 0
        set(value) { field = value; invalidate() }
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        fun piece(id: Int, crop: RectF, target: RectF, rotation: Float = 0f, alpha: Int = 255) {
            canvas.save(); canvas.rotate(rotation, target.centerX(), target.centerY())
            CollageAssets.piece(context, canvas, id, crop, target, alpha); canvas.restore()
        }
        piece(R.drawable.halftone_texture, RectF(0f, 0f, .6f, .6f), RectF(w*.08f,h*.08f,w*.9f,h*.92f), alpha = 28)
        piece(R.drawable.paper_scraps, RectF(.02f,.08f,.49f,.49f), RectF(w*.08f,h*.14f,w*.83f,h*.82f), -5f)
        val mission = variant % 3 == 2
        val crop = when(variant % 3) { 1 -> RectF(.01f,.50f,.49f,.99f); 2 -> RectF(.51f,.50f,.99f,.99f); else -> RectF(.5f,.07f,1f,.49f) }
        piece(R.drawable.paper_scraps, crop, RectF(w*.20f,h*.26f,w*.97f,h*.94f), if(mission) -2f else 4f)
        piece(R.drawable.comic_accents, RectF(0f,0f,.27f,.29f), RectF(w*.02f,0f,w*.32f,h*.62f), -4f)
        piece(R.drawable.dossier_elements, RectF(.58f,.035f,.82f,.21f), RectF(w*.63f,h*.02f,w*.94f,h*.25f), 6f)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(if(mission) R.color.ink_black else R.color.warm_white); strokeWidth = h*.012f; style = Paint.Style.STROKE }
        canvas.drawLine(w*.38f,h*.48f,w*.79f,h*.48f,ink)
        canvas.drawLine(w*.38f,h*.59f,w*.69f,h*.59f,ink)
        canvas.drawLine(w*.38f,h*.70f,w*.75f,h*.70f,ink)
    }
}

/** Opaque reading stock with printing and atlas details confined to its perimeter. */
class CollageSurface(private val context: Context, private val tone: String = "paper", private val variant: Int = 0) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: Canvas) {
        val b = RectF(bounds); val density = context.resources.displayMetrics.density
        val ink = when(tone) { "red" -> R.color.archive_red; "blue" -> R.color.archive_blue; "yellow" -> R.color.comic_yellow; "dark" -> R.color.archive_black; else -> R.color.warm_white }
        paint.color = context.getColor(ink)
        canvas.drawRoundRect(b, 4*density, 4*density, paint)
        canvas.save(); canvas.clipRect(b)
        if(tone == "paper" || tone == "yellow") {
            CollageAssets.piece(context,canvas,R.drawable.paper_texture,RectF(0f,0f,1f,1f),b,30)
        }
        val edge = (18*density).coerceAtMost(b.height()*.18f)
        val strip = RectF(b.left,b.bottom-edge,b.right,b.bottom)
        val crop = if(tone == "blue") RectF(.02f,.50f,.49f,.99f) else if(tone == "red") RectF(.5f,.07f,1f,.49f) else RectF(.02f,.08f,.49f,.49f)
        CollageAssets.piece(context,canvas,R.drawable.paper_scraps,crop,RectF(strip.left,strip.top-edge,strip.right,strip.bottom+edge), if(tone == "dark") 70 else 170)
        if(variant > 0) {
            val s = (52*density).coerceAtMost(b.width()*.22f)
            CollageAssets.piece(context,canvas,R.drawable.halftone_texture,RectF(0f,0f,.5f,.5f),RectF(b.right-s,b.top,b.right,b.top+s),if(tone == "dark") 50 else 35)
            CollageAssets.piece(context,canvas,R.drawable.dossier_elements,RectF(.58f,.035f,.82f,.21f),RectF(b.right-s,b.top,b.right,b.top+14*density),180)
        }
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
