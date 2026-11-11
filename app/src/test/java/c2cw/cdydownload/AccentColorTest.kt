package c2cw.cdydownload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccentColorTest {
    @Test fun dailyAccentStaysStableForADayAndThenChanges() {
        assertEquals(dailyAccent(12), dailyAccent(12))
        assertEquals(dailyAccent(3).name, dailyAccent(3 + DAILY_ACCENTS.size).name)
        assertNotEquals(dailyAccent(1).name, dailyAccent(2).name)
    }

    @Test fun unknownAccentModeFallsBackToWallpaper() {
        assertEquals(AccentMode.WALLPAPER, accentModeOf(null))
        assertEquals(AccentMode.WALLPAPER, accentModeOf("nope"))
        assertEquals(AccentMode.ORANGE, accentModeOf("ORANGE"))
    }
}
