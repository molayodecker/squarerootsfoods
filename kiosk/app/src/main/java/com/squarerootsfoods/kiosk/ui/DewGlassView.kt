package com.squarerootsfoods.kiosk.ui

import android.content.Context
import android.graphics.*
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.*

/**
 * Condensation on glass over a photo: mist, beads, and drops that run down leaving clear trails.
 * Native port of dew-glass.js (screen 01 Attract). Simulation units are dp; drawing is px.
 */
class DewGlassView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    /** Vertical crop focus when the photo is taller than the view: 0 = top, 1 = bottom. */
    var focusY = 0.5f
        set(v) { if (v != field) { field = v; rebuild() } }

    private var src: Bitmap? = null
    fun setImageBitmap(b: Bitmap?) { if (b !== src) { src = b; rebuild() } }

    private class Bead(var x: Float, var y: Float, var r: Float)
    private class Drop(var x: Float, var y: Float, var r: Float) {
        var vy = 0f; var stall = 0f; var last = y; var wob = Random.nextFloat() * TAU; var moving = false
    }
    private object Random { private val r = java.util.Random(); fun nextFloat() = r.nextFloat() }
    private fun rnd() = Random.nextFloat()

    private val d = resources.displayMetrics.density
    private var w = 0f; private var h = 0f
    private var base: Bitmap? = null; private var blur: Bitmap? = null
    private var fog: Bitmap? = null; private var fc: Canvas? = null
    private val beads = ArrayList<Bead>(); private val drops = ArrayList<Drop>()
    private var t = 0f; private var fogAcc = 0f; private var lastNs = 0L; private var running = false

    private val filter = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val beadFill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val beadStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.7f * d; color = Color.argb(82, 8, 18, 32) }
    private val beadHl = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(178, 255, 246, 236) }
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(71, 255, 240, 225) }
    private val glint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(217, 255, 248, 240) }
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val erase = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT); alpha = 140 }
    private val refog = Paint(Paint.FILTER_BITMAP_FLAG)
    private val shadeColors = intArrayOf(Color.argb(0, 6, 14, 28), Color.argb(10, 6, 14, 28), Color.argb(128, 6, 14, 28))
    private val shadeStops = floatArrayOf(0f, 0.7f, 1f)
    private val oval = RectF(); private val oval2 = RectF(); private val dst = RectF(); private val srcR = Rect(); private val clip = Path()

    private val still: Boolean
        get() = try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: RuntimeException) {
            false
        }

    override fun onSizeChanged(nw: Int, nh: Int, ow: Int, oh: Int) { rebuild() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); start() }
    override fun onDetachedFromWindow() { running = false; super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(v: Int) { super.onWindowVisibilityChanged(v); if (v == VISIBLE) start() else running = false }

    private fun rebuild() {
        val s = src ?: return
        val W = width; val H = height
        if (W == 0 || H == 0) return
        w = W / d; h = H / d
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        Canvas(b).apply {
            val ir = s.width.toFloat() / s.height; val cr = W.toFloat() / H
            if (ir > cr) { val dw = H * ir; dst.set((W - dw) / 2, 0f, (W + dw) / 2, H.toFloat()) }
            else { val dh = W / ir; val top = (H - dh) * focusY; dst.set(0f, top, W.toFloat(), top + dh) }
            drawBitmap(s, null, dst, filter)
            drawColor(Color.argb(13, 255, 238, 222))
        }
        // Soft blur: halve 4×, double back 3×, bilinear each step.
        var cur = b; var cw = W; var ch = H; val chain = ArrayList<Bitmap>()
        repeat(4) { cw = max(1, cw / 2); ch = max(1, ch / 2); cur = Bitmap.createScaledBitmap(cur, cw, ch, true); chain += cur }
        repeat(3) { cw *= 2; ch *= 2; cur = Bitmap.createScaledBitmap(cur, cw, ch, true); chain += cur }
        val bl = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        Canvas(bl).apply { drawBitmap(cur, null, Rect(0, 0, W, H), filter); drawColor(Color.argb(26, 255, 238, 222)) }
        chain.forEach { it.recycle() }

        base?.recycle(); blur?.recycle(); fog?.recycle()
        base = b; blur = bl
        fog = bl.copy(Bitmap.Config.ARGB_8888, true).also { fc = Canvas(it) }
        beadFill.shader = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        if (drops.isEmpty()) seed()
        if (still) { running = false; repeat(360) { step(1 / 30f) } } else start()
        invalidate()
    }

    private fun newDrop(y: Float) = Drop(rnd() * w, y, 2.5f + rnd().pow(1.5f) * 5.5f)
    private fun newBead() = Bead(rnd() * w, rnd() * h, 0.5f + rnd().pow(2) * 2.2f)
    private fun seed() {
        repeat((w * h / 520).toInt()) { beads += newBead() }
        repeat((w * h / 7000).toInt()) { drops += newDrop(rnd() * h) }
    }

    private fun step(dt: Float) {
        val fc = fc ?: return; val blur = blur ?: return
        t += dt
        if (beads.size < w * h / 520 && rnd() < dt * 60) beads += newBead()
        for (b in drops) {
            if (!b.moving) {
                b.r += dt * 0.12f * rnd()
                if (b.r > 6 && rnd() < dt * 0.12f * (b.r - 5)) { b.moving = true; b.vy = 8f; b.last = b.y }
                continue
            }
            if (rnd() < dt * 1.1f) b.stall = 0.12f + rnd() * 0.5f
            if (b.stall > 0) { b.stall -= dt; b.vy *= 0.85f } else b.vy += (30 + b.r * 14 - b.vy) * min(1f, dt * 3)
            b.y += b.vy * dt; b.x += sin(t * 2 + b.wob) * dt * 4; b.r -= dt * 0.3f
            if (b.y - b.last > 8 + rnd() * 14) {
                b.last = b.y
                beads += Bead(b.x + (rnd() - 0.5f) * b.r * 0.6f, b.y - b.r * 1.3f, b.r * (0.22f + rnd() * 0.18f))
            }
            oval.set((b.x - b.r * 0.6f) * d, (b.y - b.r * 0.8f) * d, (b.x + b.r * 0.6f) * d, (b.y + b.r * 0.8f) * d)
            fc.drawOval(oval, erase)
            val it = beads.iterator()
            while (it.hasNext()) {
                val s = it.next()
                if (abs(s.x - b.x) < b.r + s.r && abs(s.y - b.y) < b.r + s.r && s.y < b.y + b.r + s.r) {
                    b.r = min(8f, hypot(b.r, s.r * 0.9f)); it.remove()
                }
            }
            for (o in drops) if (o !== b && !o.moving && hypot(o.x - b.x, o.y - b.y) < b.r + o.r) {
                b.r = min(8f, hypot(b.r, o.r))
                o.x = rnd() * w; o.y = rnd() * h * 0.8f; o.last = o.y; o.r = 1.5f + rnd() * 2
            }
            if (b.r < 2.4f) { b.moving = false; b.vy = 0f }
            if (b.y - b.r > h) { b.x = rnd() * w; b.y = rnd() * h * 0.85f; b.r = 1.5f + rnd() * 2; b.moving = false; b.vy = 0f; b.stall = 0f }
        }
        fogAcc += dt * 0.3f
        if (fogAcc >= 0.02f) { refog.alpha = (fogAcc * 255).toInt().coerceIn(1, 255); fc.drawBitmap(blur, 0f, 0f, refog); fogAcc = 0f }
    }

    override fun onDraw(c: Canvas) {
        val b = base ?: return; val f = fog ?: return
        if (running) {
            val now = System.nanoTime()
            val dt = if (lastNs == 0L) 1 / 60f else min(0.05f, (now - lastNs) / 1e9f); lastNs = now
            step(dt)
        }
        c.drawBitmap(b, 0f, 0f, null)
        c.drawBitmap(f, 0f, 0f, null)
        for (s in beads) {
            val x = s.x * d; val y = s.y * d; val r = s.r * d
            c.drawCircle(x, y, r, beadFill); c.drawCircle(x, y, r, beadStroke)
            c.drawCircle(x - r * 0.3f, y - r * 0.35f, r * 0.3f, beadHl)
        }
        val k = 3.2f
        for (q in drops) {
            val x = q.x * d; val y = q.y * d; val r = q.r * d; val ry = r * if (q.moving) 1.2f else 1.06f
            oval.set(x - r, y - ry, x + r, y + ry)
            c.save()
            clip.rewind(); clip.addOval(oval, Path.Direction.CW); c.clipPath(clip)
            c.translate(x, y); c.scale(-1f, -1f)   // refraction: a wider patch, inverted
            dst.set(-r, -ry, r, ry)
            srcR.set(
                (x - r * k).toInt().coerceAtLeast(0),
                (y - r * k).toInt().coerceAtLeast(0),
                (x + r * k).toInt().coerceAtMost(b.width),
                (y + r * k).toInt().coerceAtMost(b.height),
            )
            if (srcR.width() > 0 && srcR.height() > 0) c.drawBitmap(b, srcR, dst, filter)
            c.restore()
            shade.shader = RadialGradient(x, y, max(r, ry), shadeColors, shadeStops, Shader.TileMode.CLAMP)
            c.drawOval(oval, shade)
            oval2.set(x + r * 0.1f - r * 0.45f, y + ry * 0.55f - r * 0.17f, x + r * 0.1f + r * 0.45f, y + ry * 0.55f + r * 0.17f)
            c.drawOval(oval2, sheen)
            val hx = x - r * 0.38f; val hy = y - ry * 0.42f
            c.save(); c.rotate(-34.4f, hx, hy)
            oval2.set(hx - r * 0.22f, hy - r * 0.14f, hx + r * 0.22f, hy + r * 0.14f); c.drawOval(oval2, glint)
            c.restore()
        }
        if (running) postInvalidateOnAnimation()
    }

    private fun start() {
        if (running || still || base == null || !isAttachedToWindow) return
        running = true; lastNs = 0L; postInvalidateOnAnimation()
    }

    private companion object { const val TAU = (PI * 2).toFloat() }
}

/** Compose wrapper.  DewGlass(bitmap = heroBitmap, modifier = Modifier.fillMaxWidth().height(560.dp)) */
@Composable
fun DewGlass(bitmap: ImageBitmap?, modifier: Modifier = Modifier, focusY: Float = 0.5f) {
    AndroidView(
        factory = {
            DewGlassView(it).apply {
                isClickable = false
                isFocusable = false
                contentDescription = "Cold cabinet"
            }
        },
        modifier = modifier,
        update = { v -> v.focusY = focusY; v.setImageBitmap(bitmap?.asAndroidBitmap()) },
    )
}
