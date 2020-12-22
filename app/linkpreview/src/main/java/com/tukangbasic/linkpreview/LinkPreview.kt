package com.tukangbasic.linkpreview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import retrofit2.*
import java.lang.reflect.Type
import java.net.URI
import java.net.URISyntaxException


class LinkPreview(
    private val url: String,
    private val callback: LinkPreviewCallback?,
    private val webView: WebView?
) {
    fun execute() {
        try {
            getLinkPreview()
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    private fun getLinkPreview() {
        val converterFactory = object : Converter.Factory() {
            override fun responseBodyConverter(
                type: Type,
                annotations: Array<Annotation>,
                retrofit: Retrofit
            ): Converter<ResponseBody, *>? {
                return if (type === LinkPreviewModel::class.java)
                    LinkPreviewConverter()
                else
                    null
            }


        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://test.com")
            .addConverterFactory(converterFactory)
            .build()

        val linkPreviewService = retrofit.create(LinkPreviewService::class.java)

        linkPreviewService.getLinkPreview(url).enqueue(object : Callback<LinkPreviewModel> {
            override fun onResponse(
                call: Call<LinkPreviewModel>,
                response: Response<LinkPreviewModel>
            ) {
                response.body()?.content?.let {
                    webView?.loadDataWithBaseURL(
                        null,
                        it, "text/html", "utf-8", null
                    )
                }
            }

            override fun onFailure(call: Call<LinkPreviewModel>, t: Throwable) {
                Throwable(t)
            }
        })
    }

    companion object {
        fun Builder(context: Context): LinkPreviewBuilder {
            return LinkPreviewBuilder(context)
        }


        class LinkPreviewBuilder(private val context: Context) {
            private var callback: LinkPreviewCallback? = null
            private var url: String = ""
            private var webView: WebView? = null

            init {
                setWebView()
            }

            @SuppressLint("SetJavaScriptEnabled")
            private fun setWebView() {
                webView = WebView(context)
                webView?.settings?.javaScriptEnabled = true
                webView?.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(cmsg: ConsoleMessage): Boolean {
                        // check secret prefix
                        if (cmsg.message().startsWith("MAGIC")) {
                            val html = cmsg.message().substring(5) // strip off prefix
                            extractInformation(html)
                            return true
                        }
                        return false
                    }
                }
                webView?.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, address: String) {
                        // have the page spill its guts, with a secret prefix
                        if(address == "about:blank"){
                            return
                        }
                        view.loadUrl("javascript:console.log('MAGIC'+document.getElementsByTagName('html')[0].innerHTML);")
                    }
                }
            }

            fun setCallback(callback: LinkPreviewCallback): LinkPreviewBuilder {
                this.callback = callback
                return this
            }

            fun setUrl(url: String): LinkPreviewBuilder {
                this.url = url
                return this
            }

            fun build(): LinkPreview {
                return LinkPreview(url, callback, webView)
            }


            private fun extractInformation(html: String) {
                val document: Document = Jsoup.parse(html)
                val metaData = LinkPreviewMetaData(
                    url = url
                )
                metaData.title = getTitle(document)
                metaData.description = getDesc(document)
                metaData.mediaType = getMediaType(document)
                metaData.imageUrl = getImageUrl(document, metaData)
                getOther(document, metaData)

                callback?.onSuccess(
                    LinkPreviewEntity(url, metaData, html)
                )
            }

            private fun getOther(document: Document, metaData: LinkPreviewMetaData) {
                val elements: Elements = document.getElementsByTag("meta")
                var src: String = document.select("link[rel=apple-touch-icon]").attr("href")
                if (src.isNotEmpty()) {
                    metaData.favicon = resolveURL(url, src)
                } else {
                    src = document.select("link[rel=icon]").attr("href")
                    if (src.isNotEmpty()) {
                        metaData.favicon = resolveURL(url, src)
                    }
                }

                for (element in elements) {
                    if (element.hasAttr("property")) {
                        val strProperty = element.attr("property").toString().trim { it <= ' ' }
                        if (strProperty == "og:url") {
                            metaData.url = element.attr("content").toString()
                        }
                        if (strProperty == "og:site_name") {
                            metaData.siteName = element.attr("content").toString()
                        }
                    }
                }

                if (metaData.url == "" || metaData.url.isEmpty()) {
                    var uri: URI? = null
                    try {
                        uri = URI(url)
                    } catch (e: URISyntaxException) {
                        e.printStackTrace()
                    }
                    metaData.url = uri?.host.toString()
                }
            }

            private fun getImageUrl(document: Document, metaData: LinkPreviewMetaData): String {
                var imageUrl = ""
                val imageElements: Elements = document.select("meta[property=og:image]")
                if (imageElements.size > 0) {
                    val image = imageElements.attr("content")
                    if (image.isNotEmpty()) {
                        imageUrl = resolveURL(url, image)
                    }
                }
                if (imageUrl.isEmpty()) {
                    var src: String = document.select("link[rel=image_src]").attr("href")
                    if (src.isNotEmpty()) {
                        imageUrl = resolveURL(url, src)
                    } else {
                        src = document.select("link[rel=apple-touch-icon]").attr("href")
                        if (src.isNotEmpty()) {
                            imageUrl = resolveURL(url, src)
                            metaData.favicon = resolveURL(url, src)
                        } else {
                            src = document.select("link[rel=icon]").attr("href")
                            if (src.isNotEmpty()) {
                                imageUrl = resolveURL(url, src)
                                metaData.favicon = resolveURL(url, src)
                            }
                        }
                    }
                }
                return imageUrl
            }

            private fun getMediaType(document: Document): String {
                val mediaTypes: Elements = document.select("meta[name=medium]")
                return if (mediaTypes.size > 0) {
                    val media = mediaTypes.attr("content")
                    if (media == "image") "photo" else media
                } else {
                    document.select("meta[property=og:type]").attr("content")
                }
            }

            private fun getTitle(document: Document): String {
                val title:String? = document.select("meta[property=og:title]").attr("content")
                if(title == null || title.isEmpty()){
                    return document.title()
                }
                return title
            }

            private fun getDesc(document: Document): String {
                var description: String? = document.select("meta[name=description]").attr("content")
                if (description == null || description.isEmpty()) {
                    description = document.select("meta[name=Description]").attr("content")
                }
                if (description == null || description.isEmpty()) {
                    description = document.select("meta[property=og:description]").attr("content")
                }
                if (description == null || description.isEmpty()) {
                    description = ""
                }
                return description
            }

            private fun resolveURL(url: String, part: String): String {
                return if (URLUtil.isValidUrl(part)) {
                    part
                } else {
                    var baseUri: URI? = null
                    try {
                        baseUri = URI(url)
                    } catch (e: URISyntaxException) {
                        e.printStackTrace()
                    }
                    baseUri = baseUri?.resolve(part)
                    baseUri.toString()
                }
            }
        }

    }
}