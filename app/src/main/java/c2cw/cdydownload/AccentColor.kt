package c2cw.cdydownload

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

internal const val ACCENT_MODE = "accentMode"
internal const val ACCENT_CUSTOM = "accentCustom"
internal const val ACCENT_RANDOM_LAST = "accentRandomLast"

enum class AccentMode(val label: String) {
    WALLPAPER("跟随壁纸"),
    RANDOM("随机取色"),
    CUSTOM("自定义")
}

/** Used when the system cannot provide wallpaper colors, and as the first custom color. */
internal const val FALLBACK_ACCENT = 0xFF3B6FE0.toInt()

/** Seeds for random accent. Each launch picks one, never repeating the previous launch. */
internal val RANDOM_ACCENTS: List<Int> = listOf(
    0xFF3B6FE0, 0xFF00897B, 0xFF43A047, 0xFF7E57C2,
    0xFFD81B60, 0xFF1E88E5, 0xFF5C6BC0, 0xFF00ACC1, 0xFFEC407A, 0xFF607D8B
).map { it.toInt() }

/** Preset swatches shown in the custom color picker. */
internal val PRESET_ACCENTS: List<Int> = listOf(
    0xFF3B6FE0, 0xFF1E88E5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342,
    0xFFC0CA33, 0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF546E7A
).map { it.toInt() }

/** Old values: ORANGE and DAILY no longer exist. Unknown values fall back to wallpaper colors. */
internal fun accentModeOf(raw: String?): AccentMode = when(raw) {
    "ORANGE" -> AccentMode.WALLPAPER
    "DAILY" -> AccentMode.RANDOM
    else -> AccentMode.entries.firstOrNull { it.name == raw } ?: AccentMode.WALLPAPER
}

/** Picks a palette index different from the previous one when the palette has more than one color. */
internal fun pickRandomAccent(previous: Int, size: Int = RANDOM_ACCENTS.size, random: Random = Random.Default): Int {
    if(size <= 1) return 0
    if(previous !in 0 until size) return random.nextInt(size)
    val next = random.nextInt(size - 1)
    return if(next >= previous) next + 1 else next
}

/** Derived colors for one seed, as ARGB ints so they can be tested without Compose. */
internal data class AccentTones(
    val primary: Int, val onPrimary: Int, val container: Int, val onContainer: Int,
    val subtle: Int
)

internal fun relativeLuminance(argb: Int): Double {
    fun channel(v: Int): Double { val c = v / 255.0; return if(c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4) }
    return 0.2126 * channel(argb shr 16 and 0xFF) + 0.7152 * channel(argb shr 8 and 0xFF) + 0.0722 * channel(argb and 0xFF)
}

internal fun contrastRatio(a: Int, b: Int): Double {
    val x = relativeLuminance(a); val y = relativeLuminance(b)
    return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
}

internal fun accentTones(seed: Int, dark: Boolean): AccentTones {
    val (h, s, _) = rgbToHsl(seed)
    val sat = s.coerceIn(0.28f, 0.9f)
    // Greens and cyans look much brighter than blues at the same lightness, so walk the
    // lightness until the text on top of the accent stays readable.
    if(!dark) {
        var l = 0.44f
        while(l > 0.2f && contrastRatio(hslToRgb(h, sat, l), 0xFFFFFFFF.toInt()) < 4.0) l -= 0.02f
        return AccentTones(
            primary = hslToRgb(h, sat, l),
            onPrimary = 0xFFFFFFFF.toInt(),
            container = hslToRgb(h, sat * 0.75f, 0.91f),
            onContainer = hslToRgb(h, sat, 0.2f),
            subtle = hslToRgb(h, sat * 0.4f, 0.95f)
        )
    }
    val onPrimary = hslToRgb(h, sat, 0.14f)
    var l = 0.72f
    while(l < 0.9f && contrastRatio(hslToRgb(h, sat.coerceAtMost(0.75f), l), onPrimary) < 4.5) l += 0.02f
    return AccentTones(
        primary = hslToRgb(h, sat.coerceAtMost(0.75f), l),
        onPrimary = onPrimary,
        container = hslToRgb(h, sat * 0.55f, 0.26f),
        onContainer = hslToRgb(h, sat, 0.88f),
        subtle = hslToRgb(h, sat * 0.3f, 0.18f)
    )
}

/** Hue in degrees, saturation and lightness 0..1. */
internal fun rgbToHsl(argb: Int): Triple<Float, Float, Float> {
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    if(d == 0f) return Triple(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when(max) {
        r -> 60f * (((g - b) / d) % 6f)
        g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    return Triple((h + 360f) % 360f, s.coerceIn(0f, 1f), l)
}

internal fun hslToRgb(h: Float, s: Float, l: Float): Int {
    val c = (1f - abs(2f * l - 1f)) * s.coerceIn(0f, 1f)
    val hp = ((h % 360f) + 360f) % 360f / 60f
    val x = c * (1f - abs(hp % 2f - 1f))
    val (r1, g1, b1) = when(hp.toInt()) {
        0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    fun channel(v: Float) = ((v + m).coerceIn(0f, 1f) * 255f).roundToInt()
    return (0xFF shl 24) or (channel(r1) shl 16) or (channel(g1) shl 8) or channel(b1)
}

internal fun hsvToRgb(h: Float, s: Float, v: Float): Int {
    val c = v * s
    val hp = ((h % 360f) + 360f) % 360f / 60f
    val x = c * (1f - abs(hp % 2f - 1f))
    val (r1, g1, b1) = when(hp.toInt()) {
        0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
    }
    val m = v - c
    fun channel(f: Float) = ((f + m).coerceIn(0f, 1f) * 255f).roundToInt()
    return (0xFF shl 24) or (channel(r1) shl 16) or (channel(g1) shl 8) or channel(b1)
}

internal fun rgbToHsv(argb: Int): Triple<Float, Float, Float> {
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    return Triple((h + 360f) % 360f, if(max == 0f) 0f else d / max, max)
}

internal fun hexOf(argb: Int) = "#%06X".format(argb and 0xFFFFFF)

internal object AccentPreference {
    var mode by mutableStateOf(AccentMode.WALLPAPER)
    var custom by mutableIntStateOf(FALLBACK_ACCENT)
    /** Chosen once per process start, so the color stays the same while the app is open. */
    var randomSeed by mutableIntStateOf(RANDOM_ACCENTS.first())
        private set
    private var loaded = false

    fun load(prefs: SharedPreferences) {
        val raw = prefs.getString(ACCENT_MODE, null)
        mode = accentModeOf(raw)
        if(raw != null && raw != mode.name) prefs.edit().putString(ACCENT_MODE, mode.name).apply()
        custom = prefs.getInt(ACCENT_CUSTOM, FALLBACK_ACCENT)
        if(!loaded) {
            loaded = true
            val index = pickRandomAccent(prefs.getInt(ACCENT_RANDOM_LAST, -1))
            randomSeed = RANDOM_ACCENTS[index]
            prefs.edit().putInt(ACCENT_RANDOM_LAST, index).apply()
        }
    }
}
