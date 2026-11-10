package c2cw.cdydownload

import java.net.URI

internal object XiaohongshuLinks {
    fun fromUri(uri: URI): VideoLink? {
        val host = uri.host?.lowercase() ?: return null
        val path = uri.path.orEmpty().trimEnd('/')
        if(host in setOf("xhslink.com", "www.xhslink.com", "xhslink.cn", "www.xhslink.cn") && Regex("/(?:[a-z]/)?[A-Za-z0-9]+").matches(path))
            return VideoLink(Platform.XIAOHONGSHU, "https://$host$path" + (uri.rawQuery?.let { "?$it" } ?: ""), "xhs-short:$path")
        if(host !in setOf("xiaohongshu.com", "www.xiaohongshu.com", "www.xhs.cn", "rednote.com", "www.rednote.com")) return null
        val id = Regex("/(?:explore|discovery/item)/([0-9a-fA-F]{24})").matchEntire(path)?.groupValues?.get(1) ?: return null
        // Access signatures and source parameters must survive canonicalization.
        return VideoLink(Platform.XIAOHONGSHU, "https://$host$path" + (uri.rawQuery?.let { "?$it" } ?: ""), "xhs:$id")
    }
}
