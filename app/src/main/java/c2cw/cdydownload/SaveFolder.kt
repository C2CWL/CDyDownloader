package c2cw.cdydownload

import android.content.SharedPreferences

internal const val SAVE_FOLDER = "saveFolder"
internal const val SAVE_FOLDER_VIDEO = "saveFolderVideo"
internal const val SAVE_FOLDER_IMAGE = "saveFolderImage"
internal const val SAVE_FOLDER_MUSIC = "saveFolderMusic"
internal const val DEFAULT_SAVE_FOLDER = "CDy下载器"

enum class SaveKind { VIDEO, IMAGE, MUSIC }

/** A blank result means no extra subfolder. Slashes are removed so the name stays one level. */
internal fun optionalSaveFolder(name: String?): String =
    (name ?: "").replace("/", "").replace("\\", "").trim().take(40)

/** Old single-folder setting. Blank still falls back so an existing install keeps its subfolder. */
internal fun sanitizedSaveFolder(name: String?): String =
    optionalSaveFolder(name).ifBlank { DEFAULT_SAVE_FOLDER }

internal fun resolveSaveFolder(hasNew: Boolean, stored: String?, hasLegacy: Boolean, legacy: String?): String = when {
    hasNew -> optionalSaveFolder(stored)
    hasLegacy -> sanitizedSaveFolder(legacy)
    else -> DEFAULT_SAVE_FOLDER
}

internal fun readSaveFolder(prefs: SharedPreferences, kind: SaveKind): String {
    val key = when(kind) {
        SaveKind.VIDEO -> SAVE_FOLDER_VIDEO
        SaveKind.IMAGE -> SAVE_FOLDER_IMAGE
        SaveKind.MUSIC -> SAVE_FOLDER_MUSIC
    }
    return resolveSaveFolder(prefs.contains(key), prefs.getString(key, null), prefs.contains(SAVE_FOLDER), prefs.getString(SAVE_FOLDER, null))
}

internal fun mediaRelativePath(root: String, folder: String): String =
    if(folder.isBlank()) root else "$root/$folder"

internal fun mediaLocationLabel(root: String, folder: String): String =
    if(folder.isBlank()) root else "$root / $folder"
