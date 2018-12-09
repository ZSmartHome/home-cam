package com.github.zeckson.videoview

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.VideoView


class HomeCamActivity : Activity() {

    @SuppressLint("AuthLeak")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cam)
        val vidView = findViewById<View>(R.id.myVideo) as VideoView

        val vidAddress =
            "rtsp://admin:admin@ipcam/12"
        val vidUri = Uri.parse(vidAddress)

        vidView.setVideoURI(vidUri)
        vidView.start()
    }
}
