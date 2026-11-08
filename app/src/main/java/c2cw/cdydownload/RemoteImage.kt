package c2cw.cdydownload

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Covers and album thumbnails are plain CDN images, so decode them once into a small bounded cache
 * instead of pulling an image loading library into the app.
 */
internal object PreviewImages {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun cached(url: String): Bitmap? = if (url.isBlank()) null else cache.get(url)

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        cached(url)?.let { return@withContext it }
        val request = Request.Builder().url(url)
            .header("User-Agent", VideoResolver.MOBILE)
            .header("Referer", "https://www.douyin.com/")
            .build()
        try {
            VideoResolver.client.newCall(request).execute().use { response ->
                if(!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while(bounds.outWidth / (sample * 2) >= 720) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?.also { cache.put(url, it) }
            }
        } catch(_: Exception) {
            null
        }
    }
}

@Composable internal fun RemoteImage(url: String, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop) {
    var bitmap by remember(url) { mutableStateOf(PreviewImages.cached(url)) }
    LaunchedEffect(url) { if(bitmap == null) bitmap = PreviewImages.load(url) }
    val loaded = bitmap
    if(loaded != null) Image(loaded.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    else Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer))
}
