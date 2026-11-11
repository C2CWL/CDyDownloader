package c2cw.cdydownload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AccentColorTest {
    @Test fun oldModesMigrate() {
        assertEquals(AccentMode.WALLPAPER, accentModeOf("ORANGE"))
        assertEquals(AccentMode.RANDOM, accentModeOf("DAILY"))
        assertEquals(AccentMode.CUSTOM, accentModeOf("CUSTOM"))
        assertEquals(AccentMode.RANDOM, accentModeOf("RANDOM"))
    }

    @Test fun unknownAccentModeFallsBackToWallpaper() {
        assertEquals(AccentMode.WALLPAPER, accentModeOf(null))
        assertEquals(AccentMode.WALLPAPER, accentModeOf("nope"))
    }

    @Test fun randomPickNeverRepeatsThePreviousColor() {
        val random = Random(7)
        repeat(200) {
            val previous = it % RANDOM_ACCENTS.size
            val next = pickRandomAccent(previous, random = random)
            assertNotEquals(previous, next)
            assertTrue(next in RANDOM_ACCENTS.indices)
        }
        assertEquals(0, pickRandomAccent(0, size = 1))
        assertTrue(pickRandomAccent(-1, random = random) in RANDOM_ACCENTS.indices)
    }

    @Test fun noOrangeInPalettes() {
        (RANDOM_ACCENTS + PRESET_ACCENTS + FALLBACK_ACCENT).forEach { color ->
            val hue = rgbToHsl(color).first
            assertTrue("${hexOf(color)} is orange", hue !in 15f..45f)
        }
    }

    @Test fun hslRoundTrip() {
        listOf(0xFF3B6FE0.toInt(), 0xFF00897B.toInt(), 0xFFD81B60.toInt()).forEach { color ->
            val (h, s, l) = rgbToHsl(color)
            val back = hslToRgb(h, s, l)
            for(shift in listOf(16, 8, 0)) {
                assertTrue(kotlin.math.abs((color shr shift and 0xFF) - (back shr shift and 0xFF)) <= 1)
            }
            val (hv, sv, vv) = rgbToHsv(color)
            val backV = hsvToRgb(hv, sv, vv)
            for(shift in listOf(16, 8, 0)) {
                assertTrue(kotlin.math.abs((color shr shift and 0xFF) - (backV shr shift and 0xFF)) <= 1)
            }
        }
    }

    private fun luminance(color: Int): Double {
        fun channel(v: Int): Double { val c = v / 255.0; return if(c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4) }
        return 0.2126 * channel(color shr 16 and 0xFF) + 0.7152 * channel(color shr 8 and 0xFF) + 0.0722 * channel(color and 0xFF)
    }
    private fun contrast(a: Int, b: Int): Double {
        val (x, y) = luminance(a) to luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    @Test fun derivedTonesKeepTextReadable() {
        (PRESET_ACCENTS + 0xFFFFEB3B.toInt() + 0xFF000000.toInt() + 0xFFFFFFFF.toInt()).forEach { seed ->
            for(dark in listOf(false, true)) {
                val t = accentTones(seed, dark)
                assertTrue("${hexOf(seed)} dark=$dark primary", contrast(t.primary, t.onPrimary) >= 3.0)
                assertTrue("${hexOf(seed)} dark=$dark container", contrast(t.container, t.onContainer) >= 4.5)
            }
        }
    }
}
