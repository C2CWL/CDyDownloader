package c2cw.cdydownload

/** What the home page shows about a resolved work, before any download is queued. */
data class MediaPreview(
    val platform: Platform,
    val title: String,
    val author: String,
    val cover: String,
    val images: List<String>,
    val quality: String,
    val hasMusic: Boolean,
    val likes: Int = -1,
    val comments: Int = -1,
    val shares: Int = -1,
    val publishedAt: Long = 0,
    val referer: String = "https://www.douyin.com/",
    val userAgent: String = VideoResolver.MOBILE,
    val imageVideos: List<String?> = emptyList(),
    val authorSecUid: String = "",
    val authorAvatar: String = "",
    val source: String = ""
) {
    val isAlbum get() = images.isNotEmpty()
}

internal fun VideoInfo.toPreview() = MediaPreview(
    platform = source.platform,
    title = title,
    author = author,
    cover = cover.ifBlank { images.firstOrNull().orEmpty() },
    images = images,
    imageVideos = imageVideos,
    quality = quality,
    hasMusic = music != null,
    likes = likes,
    comments = comments,
    shares = shares,
    publishedAt = publishedAt,
    referer = referer,
    userAgent = userAgent,
    authorSecUid = authorSecUid,
    authorAvatar = authorAvatar,
    source = source.url
)
