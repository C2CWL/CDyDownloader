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

internal const val SAVE_TREE_VIDEO = "saveTreeVideo"
internal const val SAVE_TREE_IMAGE = "saveTreeImage"
internal const val SAVE_TREE_MUSIC = "saveTreeMusic"

internal fun saveTreeKey(kind: SaveKind) = when(kind) {
    SaveKind.VIDEO -> SAVE_TREE_VIDEO
    SaveKind.IMAGE -> SAVE_TREE_IMAGE
    SaveKind.MUSIC -> SAVE_TREE_MUSIC
}

internal fun saveFolderKey(kind: SaveKind) = when(kind) {
    SaveKind.VIDEO -> SAVE_FOLDER_VIDEO
    SaveKind.IMAGE -> SAVE_FOLDER_IMAGE
    SaveKind.MUSIC -> SAVE_FOLDER_MUSIC
}

internal fun saveRoot(kind: SaveKind) = when(kind) {
    SaveKind.VIDEO -> "Download"
    SaveKind.IMAGE -> "Pictures"
    SaveKind.MUSIC -> "Music"
}

/** Folder picked with the system file picker (a SAF tree URI), or blank when MediaStore folders are used. */
internal fun readSaveTree(prefs: SharedPreferences, kind: SaveKind): String = prefs.getString(saveTreeKey(kind), null).orEmpty()

/**
 * Readable label for a tree document id such as `primary:Pictures/旅行` or `1A2B-3C4D:DCIM`.
 * Returns null when the id has an unknown shape, so the caller can show a generic label.
 */
internal fun treeDocumentLabel(documentId: String): String? {
    val volume = documentId.substringBefore(':', "")
    if(volume.isBlank() || !documentId.contains(':')) return null
    val path = documentId.substringAfter(':').trim('/').replace("/", " / ")
    val root = if(volume.equals("primary", true)) "内部存储" else "存储卡"
    return if(path.isBlank()) root else "$root / $path"
}

/** Location shown in settings and notifications. */
internal fun saveLocationLabel(kind: SaveKind, folder: String, tree: String, treeLabel: (String) -> String?): String =
    if(tree.isNotBlank()) treeLabel(tree) ?: "所选文件夹" else mediaLocationLabel(saveRoot(kind), folder)

internal const val SAVE_TREE_LOST = "没有所选文件夹的写入权限，可能已被系统收回或文件夹已删除。请到「设置 › 保存位置」重新选择文件夹"
