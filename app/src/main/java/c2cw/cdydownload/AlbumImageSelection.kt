package c2cw.cdydownload

/** Null keeps legacy tasks' meaning (all images); explicit indices are bounded and ordered. */
internal fun normalizedAlbumImageIndices(imageCount: Int, selected: List<Int>?): List<Int> {
    if(imageCount <= 0) return emptyList()
    return selected?.distinct()?.filter { it in 0 until imageCount }?.sorted() ?: (0 until imageCount).toList()
}
