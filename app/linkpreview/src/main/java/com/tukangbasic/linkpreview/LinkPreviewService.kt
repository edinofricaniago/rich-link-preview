package com.tukangbasic.linkpreview

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface LinkPreviewService {
    @GET
    fun getLinkPreview(@Url url:String): Call<LinkPreviewModel>
}