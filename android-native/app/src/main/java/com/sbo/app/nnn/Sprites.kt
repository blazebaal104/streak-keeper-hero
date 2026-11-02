package com.sbo.app.nnn

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The protections are pre-rendered in Blender (android-native/blender): every tap state is one image, so on
 * screen they cost a single bitmap draw. Images are decoded off the main thread, kept in a small cache, and
 * the pile simply skips what isn't loaded yet (it is always loaded a few layers ahead).
 */
class NnnSprites(private val ctx: Context, private val maxPx: Int) {
    private val loaded = mutableStateMapOf<String, ImageBitmap>()
    private val missing = HashSet<String>()

    operator fun get(name: String): ImageBitmap? = loaded[name]

    fun name(k: Kind, state: Int, under: Kind?): String =
        if (k == Kind.TAPE) "nnn_tape_${(under ?: Kind.SAFE).id}_${state.coerceAtMost(1)}" else "nnn_${k.id}_$state"

    /** Decodes (off the main thread) every image the pile may need from [layer] on. */
    suspend fun preload(kinds: List<Kind>, layer: Int) {
        val want = ArrayList<String>()
        want += "nnn_tape_tail_0"
        // every remaining layer is drawn (many protections are see-through), so all of them must be ready
        for (i in layer until kinds.size) {
            val k = kinds[i]
            val under = kinds.getOrNull(i + 1)
            val states = if (i <= layer + 1) 0 until (if (k == Kind.TAPE) 2 else k.steps) else 0 until 1
            for (s in states) want += name(k, s, under)
        }
        // decode off the main thread, publish on it
        for (n in want) {
            if (loaded.containsKey(n) || n in missing) continue
            val img = withContext(Dispatchers.IO) { decode(n) }
            if (img == null) missing += n else loaded[n] = img
        }
        // forget what's far behind to keep memory small on low-end phones
        val keep = want.toSet()
        if (loaded.size > 48) loaded.keys.filter { it !in keep }.forEach { loaded.remove(it) }
    }

    private fun decode(n: String): ImageBitmap? {
        val id = ctx.resources.getIdentifier(n, "drawable", ctx.packageName)
        if (id == 0) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        BitmapFactory.decodeResource(ctx.resources, id, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxPx) sample *= 2
        val bmp = runCatching { BitmapFactory.decodeResource(ctx.resources, id, BitmapFactory.Options().apply { inScaled = false; inSampleSize = sample }) }.getOrNull() ?: return null
        bmp.prepareToDraw()
        return bmp.asImageBitmap()
    }

    /** Where the tape's loose end leaves the wrapping, as a fraction of the sprite (rendered next to the sprites). */
    private val anchors = HashMap<String, Offset>()
    fun anchor(under: Kind): Offset = anchors.getOrPut(under.id) {
        runCatching {
            ctx.assets.open("nnn/anchor_${under.id}.txt").bufferedReader().readText().trim().split(" ").let { Offset(it[0].toFloat(), it[1].toFloat()) }
        }.getOrDefault(Offset(0.78f, 0.7f))
    }
}

@Composable
fun rememberSprites(kinds: List<Kind>, layer: Int): NnnSprites {
    val ctx = LocalContext.current
    val maxPx = remember { (ctx.resources.displayMetrics.widthPixels * 0.9f).roundToInt().coerceIn(320, 640) }
    val s = remember { NnnSprites(ctx.applicationContext, maxPx) }
    LaunchedEffect(kinds, layer) { s.preload(kinds, layer) }
    return s
}

/** Smooth 1-D value noise in [-1, 1]: for natural, never-repeating swaying. */
fun noise1(x: Float, seed: Int = 0): Float {
    fun h(i: Int): Float { var n = i * 374761393 + seed * 668265263; n = (n xor (n ushr 13)) * 1274126177; return ((n xor (n ushr 16)) and 0xffff) / 32767.5f - 1f }
    val i = floor(x).toInt(); val f = x - i
    val u = f * f * (3 - 2 * f)
    return h(i) * (1 - u) + h(i + 1) * u
}

/** Draws [img] filling a square of side [side] centred on [c]. */
fun DrawScope.sprite(img: ImageBitmap, c: Offset, side: Float, alpha: Float = 1f) {
    val s = side.roundToInt()
    drawImage(img, srcOffset = IntOffset.Zero, srcSize = IntSize(img.width, img.height), dstOffset = IntOffset((c.x - side / 2).roundToInt(), (c.y - side / 2).roundToInt()), dstSize = IntSize(s, s), alpha = alpha)
}

fun tiltOf(seed: Int, i: Int) = ((seed * 31 + i * 97) % 15 - 7).toFloat()

/**
 * The pile on the button, outermost layer [layer] at tap state [step]: the next two layers peek from under it.
 * When the tape is on top it is drawn over the object it's wound round, and its cut end swings in the wind.
 */
fun DrawScope.drawPile(sp: NnnSprites, kinds: List<Kind>, layer: Int, step: Int, c: Offset, side: Float, t: Float, seed: Int, jitter: Offset = Offset.Zero) {
    if (layer >= kinds.size) return
    val last = kinds.lastIndex
    for (i in last downTo layer) {
        val k = kinds[i]
        val st = if (i == layer) step else 0
        val p = if (i == layer) c + jitter else c
        if (k == Kind.TAPE) {
            val under = kinds.getOrNull(i + 1) ?: continue
            rotate(tiltOf(seed, i + 1), c) { sp[sp.name(k, st, under)]?.let { sprite(it, p, side) } }
            if (st < 2) tapeTail(sp, under, p, side, t, seed, tiltOf(seed, i + 1))
        } else {
            rotate(tiltOf(seed, i), c) { sp[sp.name(k, st, null)]?.let { sprite(it, p, side) } }
        }
    }
}

/**
 * The loose end of the police tape, cut like any roll of tape and left dangling from the wrapping. It hangs,
 * drifts one way or the other on the breeze, bobs a little, and now and then just hangs still.
 */
fun DrawScope.tapeTail(sp: NnnSprites, under: Kind, c: Offset, side: Float, t: Float, seed: Int, tilt: Float) {
    val img = sp["nnn_tape_tail_0"] ?: return
    val a = sp.anchor(under)
    // anchor in the (tilted) sprite of the object under the tape
    val local = Offset((a.x - 0.5f) * side, (a.y - 0.5f) * side)
    val r = Math.toRadians(tilt.toDouble())
    val at = c + Offset((local.x * kotlin.math.cos(r) - local.y * kotlin.math.sin(r)).toFloat(), (local.x * kotlin.math.sin(r) + local.y * kotlin.math.cos(r)).toFloat())
    // breeze: slow random gusts (sometimes calm), each with its own direction; plus a gentle pendulum
    val gust = noise1(t * 0.23f, seed)                       // which way the wind blows now
    val calm = ((noise1(t * 0.11f + 7f, seed + 3) + 1f) / 2f).let { it * it }   // 0 = still, 1 = windy
    val sway = gust * 16f * calm + sin(t * 1.9f + seed) * (2.5f + 4f * calm) + noise1(t * 1.3f, seed + 9) * 3f * calm
    val bob = 1f + 0.035f * noise1(t * 0.8f, seed + 5) * calm  // stretching a touch up and down
    rotate(sway, at) {
        val w = side; val h = side * bob
        drawImage(img, srcOffset = IntOffset.Zero, srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset((at.x - w / 2).roundToInt(), (at.y - h * 0.03f).roundToInt()), dstSize = IntSize(w.roundToInt(), h.roundToInt()))
    }
}
