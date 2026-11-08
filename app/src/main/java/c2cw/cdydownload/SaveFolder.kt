package c2cw.cdydownload

internal const val SAVE_FOLDER = "saveFolder"
internal const val DEFAULT_SAVE_FOLDER = "逗逼下载器"

/** One-level MediaStore subfolder; separators would create unintended nesting. */
internal fun sanitizedSaveFolder(name: String?): String =
    (name ?: "").replace("/", "").replace("\\", "").trim().take(40).ifBlank { DEFAULT_SAVE_FOLDER }
