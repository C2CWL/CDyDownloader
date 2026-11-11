package c2cw.cdydownload

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal const val ACCENT_MODE = "accentMode"

enum class AccentMode(val label: String) {
    WALLPAPER("跟随壁纸"),
    DAILY("每天换色"),
    ORANGE("固定橙色")
}

internal data class DailyAccent(
    val name: String,
    val light: Long,
    val dark: Long,
    val lightOn: Long,
    val darkOn: Long,
    val lightContainer: Long,
    val darkContainer: Long
)

internal val DAILY_ACCENTS = listOf(
    DailyAccent("蓝色", 0xFF1565C0, 0xFF90CAF9, 0xFFFFFFFF, 0xFF0D2748, 0xFFD6E6FF, 0xFF1B3A5F),
    DailyAccent("青色", 0xFF00796B, 0xFF80CBC4, 0xFFFFFFFF, 0xFF00332E, 0xFFC8F5EF, 0xFF124E48),
    DailyAccent("绿色", 0xFF2E7D32, 0xFFA5D6A7, 0xFFFFFFFF, 0xFF123216, 0xFFD9F5DC, 0xFF1B4D22),
    DailyAccent("紫色", 0xFF6A3CBC, 0xFFD0BCFF, 0xFFFFFFFF, 0xFF2A1454, 0xFFE9DDFF, 0xFF3B2468),
    DailyAccent("粉色", 0xFFC2185B, 0xFFF48FB1, 0xFFFFFFFF, 0xFF4A1028, 0xFFFFD9E6, 0xFF5C1A36)
)

internal fun dailyAccent(dayOfYear: Int): DailyAccent =
    DAILY_ACCENTS[Math.floorMod(dayOfYear, DAILY_ACCENTS.size)]

internal fun accentModeOf(raw: String?): AccentMode =
    runCatching { AccentMode.valueOf(raw ?: "") }.getOrDefault(AccentMode.WALLPAPER)

internal object AccentPreference {
    var mode by mutableStateOf(AccentMode.WALLPAPER)
    fun load(prefs: SharedPreferences) { mode = accentModeOf(prefs.getString(ACCENT_MODE, null)) }
}
