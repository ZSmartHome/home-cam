package com.github.zeckson.videoview

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.VideoView


class HomeCamActivity : Activity() {

    @SuppressLint("AuthLeak")
    private val uri = Uri.parse("rtsp://admin:admin@ipcam/12")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cam)
        val vidView = findViewById<View>(R.id.myVideo) as VideoView

        vidView.setVideoURI(uri)
        vidView.start()
    }
}
