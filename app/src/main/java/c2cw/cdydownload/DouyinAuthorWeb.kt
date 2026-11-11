package c2cw.cdydownload

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume

/**
 * Douyin's web APIs only answer requests signed by the official page script. Instead of
 * reimplementing that signature, open the author's page in an off-screen WebView (sharing the
 * app's Douyin login cookies) and issue same-origin requests from inside the page, so the page's
 * own runtime signs them. The page is kept for a short while so paging and update checks reuse it.
 */
internal object DouyinAuthorWeb {
    private lateinit var context: Context
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var browser: WebView? = null
    private var rendererGone = false
    private var idle: Job? = null
    private var counter = 0

    fun initialize(context: Context) { this.context = context.applicationContext }

    suspend fun get(path: String, secUid: String): String {
        require(path.startsWith("/aweme/v1/web/"))
        return mutex.withLock {
            withContext(Dispatchers.Main) {
                idle?.cancel()
                try {
                    val view = ensure(secUid)
                    request(view, path)
                } catch(e: Exception) {
                    destroy()
                    throw e
                } finally {
                    idle = scope.launch { delay(90_000); mutex.withLock { destroy() } }
                }
            }
        }
    }

    private fun destroy() {
        browser?.let { runCatching { it.stopLoading(); it.destroy() } }
        browser = null
    }

    private suspend fun evaluate(view: WebView, script: String): Any? = suspendCancellableCoroutine { continuation ->
        view.evaluateJavascript(script) { if(continuation.isActive) continuation.resume(runCatching { JSONTokener(it).nextValue() }.getOrNull()) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun ensure(secUid: String): WebView {
        browser?.takeIf { !rendererGone }?.let { return it }
        destroy()
        rendererGone = false
        val view = WebView(context)
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = VideoResolver.DESKTOP
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkImage = true
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, false)
        view.webViewClient = object : WebViewClient() {
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                rendererGone = true
                return true
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.scheme != "https" || request.url.host != "www.douyin.com"
        }
        browser = view
        view.loadUrl(DouyinUserLinks.profileUrl(secUid))
        val loaded = withTimeoutOrNull(25_000) {
            while(true) {
                delay(300)
                check(!rendererGone) { "抖音网页加载失败，请重试" }
                if(evaluate(view, "document.readyState") == "complete") break
            }
            true
        }
        check(loaded == true) { "抖音网页加载超时，请检查网络后重试" }
        // Let the page's security script finish installing before issuing requests.
        delay(1500)
        return view
    }

    private suspend fun request(view: WebView, path: String): String {
        val key = "r${++counter}"
        val start = """(function(){try{window.__cdy=window.__cdy||{};window.__cdy['$key']=null;
            var x=new XMLHttpRequest();x.open('GET',${JSONObject.quote(path)},true);x.withCredentials=true;
            x.setRequestHeader('Accept','application/json, text/plain, */*');
            x.onload=function(){window.__cdy['$key']=JSON.stringify({s:x.status,b:x.responseText||''});};
            x.onerror=function(){window.__cdy['$key']=JSON.stringify({s:-1,b:''});};
            x.ontimeout=x.onerror;x.timeout=15000;x.send();return 1;}catch(e){return 0;}})()"""
        check(evaluate(view, start) == 1) { "抖音网页尚未就绪，请重试" }
        val result = withTimeoutOrNull(18_000) {
            var value: String? = null
            while(value == null) {
                delay(250)
                check(!rendererGone) { "抖音网页加载失败，请重试" }
                value = evaluate(view, "(function(){var c=window.__cdy;if(!c)return null;var r=c['$key'];if(r==null)return null;delete c['$key'];return r;})()") as? String
            }
            value
        } ?: error("抖音响应超时，请稍后重试")
        val response = JSONObject(result)
        val status = response.optInt("s")
        val body = response.optString("b")
        check(status in 200..299) { if(status == -1) "无法连接抖音，请检查网络后重试" else "抖音返回 $status，请稍后重试" }
        check(body.isNotBlank()) { "抖音没有返回内容，可能需要验证" }
        return body
    }
}
