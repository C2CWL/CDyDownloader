package c2cw.cdydownload

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val FILE_NAME_TEMPLATE = "fileNameTemplate"
internal const val DEFAULT_FILE_NAME_TEMPLATE = "{date}-{id}"

internal val NAMING_TEMPLATE_PRESETS = listOf(
    "{date}-{id}" to "日期加编号",
    "{platform}-{id}" to "平台加编号",
    "{author}-{date}" to "作者加日期",
    "{id}" to "只保留编号"
)

internal val NAMING_INSERTS = listOf(
    "{date}" to "日期",
    "{id}" to "编号",
    "{author}" to "作者",
    "{platform}" to "平台",
    "{title}" to "标题"
)

internal fun sanitizeFileNameSegment(text: String): String {
    return text.replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_").trim(' ', '.')
}

internal fun mediaBaseName(title: String): String {
    val clean = sanitizeFileNameSegment(title)
    val output = StringBuilder()
    var bytes = 0
    val points = clean.codePoints().iterator()
    while(points.hasNext()) {
        val codePoint = points.nextInt()
        val next = String(Character.toChars(codePoint))
        val size = next.toByteArray(Charsets.UTF_8).size
        if(bytes + size > 160) break // Leave room for suffix and MediaStore's pending prefix.
        output.append(next); bytes += size
    }
    return output.toString().trim(' ', '.').ifEmpty { "作品" }
}

internal fun formatFileName(
    template: String?,
    title: String,
    id: String,
    author: String = "",
    platform: Platform? = null,
    dateMillis: Long = System.currentTimeMillis(),
    motion: Boolean = false,
    indexSuffix: String = "",
    extension: String
): String {
    val rawTemplate = template?.trim()?.ifBlank { null } ?: DEFAULT_FILE_NAME_TEMPLATE
    val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(dateMillis))
    val shortId = id.take(12)
    val cleanTitle = sanitizeFileNameSegment(title).ifEmpty { "作品" }
    val cleanAuthor = sanitizeFileNameSegment(author)
    val platformName = platform?.label ?: ""

    var result = rawTemplate
        .replace("{title}", cleanTitle)
        .replace("{id}", shortId)
        .replace("{author}", cleanAuthor.ifEmpty { "未知作者" })
        .replace("{platform}", platformName.ifEmpty { "媒体" })
        .replace("{date}", dateStr)

    result = sanitizeFileNameSegment(result).ifEmpty { "$cleanTitle-$shortId" }

    val output = StringBuilder()
    var bytes = 0
    val points = result.codePoints().iterator()
    while(points.hasNext()) {
        val codePoint = points.nextInt()
        val next = String(Character.toChars(codePoint))
        val size = next.toByteArray(Charsets.UTF_8).size
        if(bytes + size > 170) break
        output.append(next); bytes += size
    }
    val base = output.toString().trim(' ', '.')
    val suffix = StringBuilder()
    if(indexSuffix.isNotEmpty()) suffix.append("-$indexSuffix")
    if(motion) suffix.append("_MP")
    suffix.append(".$extension")

    return "$base$suffix"
}
