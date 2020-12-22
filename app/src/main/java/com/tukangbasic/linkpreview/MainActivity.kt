package com.tukangbasic.linkpreview

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso


class MainActivity : AppCompatActivity() {

    val tvText:TextView by lazy {
        findViewById(R.id.tv_text)
    }

    val ivImage:ImageView by lazy {
        findViewById(R.id.iv_img)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val linkPreview:LinkPreview = LinkPreview.Builder(this)
//            .setUrl("https://tokopedia.link/AVIEYtkKpcb")
            .setUrl("https://github.com/square/picasso")
            .setCallback(object : LinkPreviewCallback {
                @SuppressLint("SetTextI18n")
                override fun onSuccess(result: LinkPreviewEntity?) {
                    Picasso.get().load(result?.meta?.imageUrl)
                        .centerCrop()
                        .fit()
                        .into(ivImage)

                    tvText.text = "Title : ${result?.meta?.title}\n\n" +
                            "Desc : ${result?.meta?.description}\n\n" +
                            "MediaType : ${result?.meta?.mediaType}"+
                            "url : ${result?.meta?.url}"
                }

                override fun onError(e: Exception) {
                    e.printStackTrace()
                }

            })
            .build()

        linkPreview.execute()
    }
}