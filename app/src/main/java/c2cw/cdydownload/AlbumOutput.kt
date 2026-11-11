package c2cw.cdydownload

enum class AlbumOutput(val label: String) { STATIC("静图"), VIDEO("动态视频"), MOTION("实况照片") }

internal fun albumOutput(index: Int, videos: List<String?>, outputs: Map<Int, AlbumOutput>): AlbumOutput =
    if(videos.getOrNull(index) == null) AlbumOutput.STATIC else outputs[index] ?: AlbumOutput.MOTION

internal fun albumSummary(indices: List<Int>, videos: List<String?>, outputs: Map<Int, AlbumOutput>): String =
    indices.groupingBy { albumOutput(it, videos, outputs) }.eachCount().entries
        .sortedBy { it.key.ordinal }.joinToString(" · ") { (type, count) ->
            "$count ${if(type == AlbumOutput.VIDEO) "段" else "张"}${type.label}"
        }.ifEmpty { "0 张静图" }
