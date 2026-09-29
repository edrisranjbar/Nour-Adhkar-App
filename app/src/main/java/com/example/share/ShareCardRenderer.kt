package com.example.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.example.R

/**
 * Content of a shareable image card. The layout is fixed-size so it looks the same on every
 * device and reads well as a WhatsApp/Instagram status (4:5).
 */
data class ShareCardSpec(
    /** Small gold label at the top, e.g. the collection name. */
    val eyebrow: String,
    /** Main text: an Arabic dhikr or an achievement title. */
    val headline: String,
    /** Renders [headline] with the Quranic Arabic face instead of the UI face. */
    val headlineIsArabic: Boolean = false,
    /** Secondary text beneath the headline, e.g. a translation. */
    val body: String? = null,
    /** Muted line at the end of the content, e.g. a hadith reference. */
    val caption: String? = null,
    /** Large gold number shown above the headline (streak days). */
    val bigNumber: String? = null,
    /** Circular artwork shown above the headline (achievement badge). */
    @param:DrawableRes val badgeRes: Int? = null,
    val appName: String,
    val callToAction: String
)

object ShareCardRenderer {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val SIDE = 110f
    private const val FOOTER_TOP = HEIGHT - 290f

    private const val BG_TOP = 0xFF102A14.toInt()
    private const val BG_BOTTOM = 0xFF2F5A28.toInt()
    private const val GOLD = 0xFFE8C872.toInt()
    private const val CREAM = 0xFFF8F3E3.toInt()

    fun render(context: Context, spec: ShareCardSpec): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val regular = font(context, R.font.vazirmatn_regular)
        val bold = font(context, R.font.vazirmatn_bold, Typeface.DEFAULT_BOLD)
        val arabic = font(context, R.font.amiri_quran_regular, Typeface.SERIF)

        drawBackground(canvas)

        var top = 130f
        val eyebrow = layout(spec.eyebrow, textPaint(bold, 38f, GOLD), maxLines = 2)
        top = draw(canvas, eyebrow, top) + 36f

        spec.bigNumber?.let {
            val paint = textPaint(bold, 210f, GOLD)
            val number = layout(it, paint, maxLines = 1, spacing = 1f)
            top = draw(canvas, number, top - 20f) + 10f
        }
        spec.badgeRes?.let { res ->
            val size = 300f
            drawBadge(context, canvas, res, RectF(WIDTH / 2f - size / 2, top, WIDTH / 2f + size / 2, top + size))
            top += size + 44f
        }

        drawContent(canvas, spec, top, FOOTER_TOP - 50f, regular, bold, arabic)
        drawFooter(context, canvas, spec, regular, bold)
        return bitmap
    }

    /** Shrinks the text until it fits between [top] and [bottom], then centers it vertically. */
    private fun drawContent(
        canvas: Canvas, spec: ShareCardSpec, top: Float, bottom: Float,
        regular: Typeface, bold: Typeface, arabic: Typeface
    ) {
        val available = bottom - top
        val headlineFace = if (spec.headlineIsArabic) arabic else bold
        val headlineBase = if (spec.headlineIsArabic) 60f else 64f
        var blocks: List<StaticLayout> = emptyList()
        var scale = 1f
        while (scale >= 0.45f) {
            blocks = contentBlocks(spec, scale, headlineFace, headlineBase, regular, includeBody = true)
            if (height(blocks) <= available) break
            scale -= 0.05f
        }
        if (height(blocks) > available) {
            // Very long adhkar: keep the Arabic text and drop the translation rather than clip.
            blocks = contentBlocks(spec, 0.45f, headlineFace, headlineBase, regular, includeBody = false)
            if (height(blocks) > available) {
                val headline = blocks.first()
                val lineHeight = headline.height.toFloat() / headline.lineCount
                val lines = ((available / lineHeight).toInt()).coerceAtLeast(1)
                blocks = listOf(layout(spec.headline, headline.paint, lines, spacing = 1.15f))
            }
        }
        var y = top + (available - height(blocks)).coerceAtLeast(0f) / 2f
        blocks.forEach { y = draw(canvas, it, y) + GAP }
    }

    private fun contentBlocks(
        spec: ShareCardSpec, scale: Float, headlineFace: Typeface, headlineBase: Float,
        regular: Typeface, includeBody: Boolean
    ): List<StaticLayout> = buildList {
        val headlineSpacing = if (spec.headlineIsArabic) 1.15f else 1.25f
        add(layout(spec.headline, textPaint(headlineFace, headlineBase * scale, CREAM), spacing = headlineSpacing))
        if (includeBody) spec.body?.takeIf { it.isNotBlank() }?.let {
            add(layout(it, textPaint(regular, 38f * scale.coerceAtLeast(0.6f), withAlpha(CREAM, 0.86f)), spacing = 1.4f))
        }
        spec.caption?.takeIf { it.isNotBlank() }?.let {
            add(layout(it, textPaint(regular, 28f * scale.coerceAtLeast(0.8f), withAlpha(GOLD, 0.9f)), maxLines = 2))
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)

        paint.shader = RadialGradient(
            WIDTH / 2f, HEIGHT * 0.38f, WIDTH * 0.7f,
            withAlpha(GOLD, 0.16f), withAlpha(GOLD, 0f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)

        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = withAlpha(GOLD, 0.55f)
        }
        canvas.drawRoundRect(RectF(40f, 40f, WIDTH - 40f, HEIGHT - 40f), 48f, 48f, frame)
        frame.strokeWidth = 1.5f
        frame.color = withAlpha(GOLD, 0.25f)
        canvas.drawRoundRect(RectF(56f, 56f, WIDTH - 56f, HEIGHT - 56f), 38f, 38f, frame)
    }

    private fun drawFooter(context: Context, canvas: Canvas, spec: ShareCardSpec, regular: Typeface, bold: Typeface) {
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(GOLD, 0.35f); strokeWidth = 2f }
        canvas.drawLine(WIDTH * 0.3f, FOOTER_TOP, WIDTH * 0.7f, FOOTER_TOP, divider)

        val logoSize = 92f
        val namePaint = textPaint(bold, 44f, CREAM)
        val nameWidth = namePaint.measureText(spec.appName)
        val rowWidth = logoSize + 22f + nameWidth
        val rowTop = FOOTER_TOP + 34f
        val rowLeft = (WIDTH - rowWidth) / 2f
        // RTL row: the name sits on the left of the logo so the logo leads on the right.
        drawBadge(
            context, canvas, R.drawable.ic_nour_adhkar_logo,
            RectF(rowLeft + rowWidth - logoSize, rowTop, rowLeft + rowWidth, rowTop + logoSize), ring = false
        )
        val nameBaseline = rowTop + logoSize / 2f - (namePaint.descent() + namePaint.ascent()) / 2f
        namePaint.textAlign = Paint.Align.LEFT
        canvas.drawText(spec.appName, rowLeft, nameBaseline, namePaint)

        val cta = layout(spec.callToAction, textPaint(regular, 30f, withAlpha(CREAM, 0.85f)), maxLines = 1)
        val ctaBottom = draw(canvas, cta, rowTop + logoSize + 20f)

        val urlPaint = textPaint(regular, 30f, GOLD).apply { textAlign = Paint.Align.CENTER }
        val url = AppLinks.BAZAAR_WEB_URL.removePrefix("https://")
        canvas.drawText(url, WIDTH / 2f, ctaBottom + 10f - urlPaint.ascent(), urlPaint)
    }

    private fun drawBadge(context: Context, canvas: Canvas, @DrawableRes res: Int, bounds: RectF, ring: Boolean = true) {
        val source = BitmapFactory.decodeResource(context.resources, res) ?: return
        if (!ring) {
            // The app logo keeps its own shape; only badges are cropped into a ringed circle.
            canvas.drawBitmap(source, null, bounds, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            source.recycle()
            return
        }
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = maxOf(bounds.width() / source.width, bounds.height() / source.height)
        shader.setLocalMatrix(Matrix().apply {
            setScale(scale, scale)
            postTranslate(
                bounds.left + (bounds.width() - source.width * scale) / 2f,
                bounds.top + (bounds.height() - source.height * scale) / 2f
            )
        })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        canvas.drawOval(bounds, paint)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 8f; color = GOLD
        }
        canvas.drawOval(bounds, stroke)
        source.recycle()
    }

    private fun layout(text: String, paint: TextPaint, maxLines: Int = Int.MAX_VALUE, spacing: Float = 1.2f): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, (WIDTH - 2 * SIDE).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setLineSpacing(0f, spacing)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

    private fun draw(canvas: Canvas, layout: StaticLayout, top: Float): Float {
        canvas.save()
        canvas.translate(SIDE, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }

    private fun height(blocks: List<StaticLayout>) =
        blocks.sumOf { it.height }.toFloat() + GAP * (blocks.size - 1).coerceAtLeast(0)

    private fun textPaint(face: Typeface, size: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face
        textSize = size
        this.color = color
    }

    private fun font(context: Context, res: Int, fallback: Typeface = Typeface.DEFAULT): Typeface =
        runCatching { ResourcesCompat.getFont(context, res) }.getOrNull() ?: fallback

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or ((alpha * 255).toInt().coerceIn(0, 255) shl 24)

    private const val GAP = 34f
}
