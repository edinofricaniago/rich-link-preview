package com.tukangbasic.linkpreview

data class LinkPreviewEntity(
    val url:String,
    val meta:LinkPreviewMetaData?,
    val rawResponse:String?
)