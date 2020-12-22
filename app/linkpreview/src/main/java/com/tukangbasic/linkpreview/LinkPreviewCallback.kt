package com.tukangbasic.linkpreview

import java.lang.Exception

interface LinkPreviewCallback  {
    fun onSuccess(result:LinkPreviewEntity?)
    fun onError(e:Exception)
}