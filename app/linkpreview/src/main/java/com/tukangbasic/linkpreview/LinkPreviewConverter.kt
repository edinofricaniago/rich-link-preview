package com.tukangbasic.linkpreview

import android.content.Context
import android.webkit.WebView
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Converter


class LinkPreviewConverter() : Converter<ResponseBody, LinkPreviewModel> {
    override fun convert(responseBody: ResponseBody): LinkPreviewModel? {

//        val document: Document = Jsoup.parse(responseBody.string())
//        val value: Element = document.select("script")[0]
//        val content: String = value.html()

        return LinkPreviewModel(responseBody.string())
    }
}