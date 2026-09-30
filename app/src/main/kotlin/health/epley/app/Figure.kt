package health.epley.app

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

/**
 * The 3D instruction figure (README §7) — the design's own renderer, never a redraw.
 *
 * Still: a held frame pre-rendered by that renderer (`tools/render-figures.mjs`, §7 option B).
 * The camera is fixed, so the held frame is exactly what the live renderer shows at rest.
 * Playing: the same renderer live, in a transparent WebView, from app assets and offline.
 * The held frame stays underneath until the WebView says it has drawn, so there is never a blank.
 *
 * @param size the figure's design size: 248 Find, 208 Welcome, 144 Hold, 136 ear result.
 */
@Composable
fun FigureView(
    step: Int,
    ear: Char,
    night: Boolean,
    playing: Boolean,
    size: Int,
    modifier: Modifier = Modifier,
    description: String,
) {
    val reduced = LocalReducedMotion.current
    val live = playing && !reduced && !LocalInspectionMode.current
    var liveReady by remember(step, ear, night) { mutableStateOf(false) }
    Box(modifier.size(size.dp).semantics { contentDescription = description }) {
        if (!live || !liveReady) {
            HeldFrame(size, step, ear, night)
        }
        if (live) {
            LiveFigure(step, ear, night, onReady = { liveReady = true })
        }
    }
}

private val heldCache = HashMap<String, ImageBitmap>()

@Composable
private fun HeldFrame(size: Int, step: Int, ear: Char, night: Boolean) {
    val context = LocalContext.current
    // Held frames exist per design size; Find and Hold have all five steps, the rest step 1.
    val path = "figure/held/$size/${if (night) "night" else "light"}-$ear-$step.png"
    val bitmap = remember(path) {
        heldCache.getOrPut(path) {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }.asImageBitmap()
        }
    }
    Image(
        bitmap,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.High,
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LiveFigure(step: Int, ear: Char, night: Boolean, onReady: () -> Unit) {
    val theme = if (night) "night" else "light"
    val state = "{step:$step, ear:'$ear', theme:'$theme', playing:true}"
    var view by remember { mutableStateOf<WebView?>(null) }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val loader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()
            WebView(ctx).apply {
                // AndroidView defaults to WRAP_CONTENT, and a WebView told to wrap its content
                // sizes its viewport height to the page — which is 0 until the canvas has a height,
                // so vh resolved to 0 and the figure drew nothing. The box is the figure's square.
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webChromeClient = object : android.webkit.WebChromeClient() {
                    // Renderer errors reach logcat; everything else stays quiet.
                    override fun onConsoleMessage(m: android.webkit.ConsoleMessage): Boolean {
                        if (m.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                            android.util.Log.w("EpleyFigure", "${m.message()} @${m.sourceId()}:${m.lineNumber()}")
                        }
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(v: WebView, request: WebResourceRequest): WebResourceResponse? =
                        loader.shouldInterceptRequest(request.url)
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onReady() {
                            post { onReady() }
                        }
                    },
                    "AndroidFigure",
                )
                loadUrl(
                    "https://appassets.androidplatform.net/assets/figure/figure-view.html" +
                        "#step=$step&ear=$ear&theme=$theme&playing=1",
                )
                view = this
            }
        },
        update = { it.evaluateJavascript("window.setFigure && setFigure($state)", null) },
    )
    DisposableEffect(Unit) {
        onDispose { view?.destroy() }
    }
}
