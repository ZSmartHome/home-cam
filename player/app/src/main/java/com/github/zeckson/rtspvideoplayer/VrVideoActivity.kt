package com.github.zeckson.rtspvideoplayer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.opengl.Matrix
import android.os.Bundle
import android.support.annotation.MainThread
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.github.zeckson.rtspvideoplayer.renderer.SceneRenderer
import com.google.vr.ndk.base.DaydreamApi
import com.google.vr.sdk.base.*
import com.google.vr.sdk.controller.Controller
import com.google.vr.sdk.controller.ControllerManager
import javax.microedition.khronos.egl.EGLConfig

/**
 * GVR Activity demonstrating a 360 video player.
 *
 * The default intent for this Activity will load a 360 placeholder panorama. For more options on
 * how to load other media using a custom Intent, see [MediaLoader].
 */
class VrVideoActivity : GvrActivity() {

    private var vrView: GvrView? = null
    private var renderer: Renderer? = null

    // Displays the controls for video playback.
    private var uiView: VideoUiView? = null

    // Given an intent with a media file and format, this will load the file and generate the mesh.
    private var mediaLoader: MediaLoader? = null

    // Interfaces with the Daydream controller.
    private var controllerManager: ControllerManager? = null
    private var controller: Controller? = null

    /**
     * Configures the VR system.
     *
     * @param savedInstanceState unused in this sample but it could be used to track video position
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaLoader = MediaLoader(this)

        val vrView = GvrView(this)
        this.vrView = vrView
        // Since the videos have fewer pixels per degree than the phones, reducing the render target
        // scaling factor reduces the work required to render the scene. This factor can be adjusted at
        // runtime depending on the resolution of the loaded video.
        // You can use Eye.getViewport() in the overridden onDrawEye() method to determine the current
        // render target size in pixels.
        vrView.setRenderTargetScale(.5f)

        // Standard GvrView configuration
        renderer = Renderer(vrView)
        vrView.setEGLConfigChooser(
            8, 8, 8, 8, // RGBA bits.
            16, // Depth bits.
            0
        )  // Stencil bits.
        vrView.setRenderer(renderer!!)
        setContentView(vrView)

        // Most Daydream phones can render a 4k video at 60fps in sustained performance mode. These
        // options can be tweaked along with the render target scale.
        if (vrView.setAsyncReprojectionEnabled(true)) {
            AndroidCompat.setSustainedPerformanceMode(this, true)
        }

        // Handle the user clicking on the 'X' in the top left corner. Since this is done when the user
        // has taken the headset out of VR, it should launch the app's exit flow directly rather than
        // using the transition flow.
        vrView.setOnCloseButtonListener { launch2dActivity() }

        // Configure Controller.
        val listener = ControllerEventListener()
        controllerManager = ControllerManager(this, listener)
        controller = controllerManager!!.controller
        controller!!.setEventListener(listener)
        // controller.start() is called in onResume().

        checkPermissionAndInitialize()
    }

    /**
     * Normal apps don't need this. However, since we use adb to interact with this sample, we
     * want any new adb Intents to be routed to the existing Activity rather than launching a new
     * Activity.
     */
    override fun onNewIntent(intent: Intent) {
        // Save the new Intent which may contain a new Uri. Then tear down & recreate this Activity to
        // load that Uri.
        setIntent(intent)
        recreate()
    }

    /** Launches the 2D app with the same extras and data.  */
    private fun launch2dActivity() {
        startActivity(Intent(intent).setClass(this, VideoActivity::class.java))
        // When launching the other Activity, it may be necessary to finish() this Activity in order to
        // free up the MediaPlayer resources. This sample doesn't call mediaPlayer.release() unless the
        // Activities are destroy()ed. This allows the video to be paused and resumed when another app
        // is in the foreground. However, most phones have trouble maintaining sufficient resources for
        // 2 4k videos in the same process. Large videos may fail to play in the second Activity if the
        // first Activity hasn't finish()ed.
        //
        // Alternatively, a single media player instance can be reused across multiple Activities in
        // order to conserve resources.
        finish()
    }

    /**
     * Tries to exit gracefully from VR using a VR transition dialog.
     *
     * @return whether the exit request has started or whether the request failed due to the device
     * not being Daydream Ready
     */
    private fun exitFromVr(): Boolean {
        // This needs to use GVR's exit transition to avoid disorienting the user.
        val api = DaydreamApi.create(this)
        if (api != null) {
            api.exitFromVr(this, EXIT_FROM_VR_REQUEST_CODE, null)
            // Eventually, the Activity's onActivityResult will be called.
            api.close()
            return true
        }
        return false
    }

    /** Initializes the Activity only if the permission has been granted.  */
    private fun checkPermissionAndInitialize() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mediaLoader!!.handleIntent(MediaLoader.webCamIntent, uiView)
        } else {
            exitFromVr()
            // This method will return false on Cardboard devices. This case isn't handled in this sample
            // but it should be handled for VR Activities that run on Cardboard devices.
        }
    }

    /**
     * Handles the result from [DaydreamApi.exitFromVr]. This is called
     * via the uiView.setVrIconClickListener listener below.
     *
     * @param requestCode matches the parameter to exitFromVr()
     * @param resultCode whether the user accepted the exit request or canceled
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, unused: Intent?) {
        if (requestCode == EXIT_FROM_VR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            launch2dActivity()
        } else {
            // This should contain a VR UI to handle the user declining the exit request.
            Log.e(TAG, "Declining the exit request isn't implemented in this sample.")
        }
    }

    override fun onResume() {
        super.onResume()
        controllerManager!!.start()
        mediaLoader!!.resume()
    }

    override fun onPause() {
        mediaLoader!!.pause()
        controllerManager!!.stop()
        super.onPause()
    }

    override fun onDestroy() {
        mediaLoader!!.destroy()
        uiView!!.setMediaPlayer(null)
        super.onDestroy()
    }

    /**
     * Standard GVR renderer. Most of the real work is done by [SceneRenderer].
     */
    private inner class Renderer
    /**
     * Creates the Renderer and configures the VR exit button.
     *
     * @param parent Any View that is already attached to the Window. The uiView will secretly be
     * attached to this View in order to properly handle UI events.
     */
    @MainThread
    constructor(parent: ViewGroup) : GvrView.StereoRenderer {

        // Used by ControllerEventListener to manipulate the scene.
        val scene: SceneRenderer

        private val viewProjectionMatrix = FloatArray(16)

        init {
            val pair = SceneRenderer.createForVR(this@VrVideoActivity, parent)
            scene = pair.first
            uiView = pair.second
            uiView!!.setVrIconClickListener(
                View.OnClickListener {
                    if (!exitFromVr()) {
                        // Directly exit Cardboard Activities.
                        onActivityResult(EXIT_FROM_VR_REQUEST_CODE, Activity.RESULT_OK, null)
                    }
                })
        }

        override fun onNewFrame(headTransform: HeadTransform) {}

        override fun onDrawEye(eye: Eye) {
            Matrix.multiplyMM(
                viewProjectionMatrix, 0, eye.getPerspective(Z_NEAR, Z_FAR), 0, eye.eyeView, 0
            )
            scene.glDrawFrame(viewProjectionMatrix, eye.type)
        }

        override fun onFinishFrame(viewport: Viewport) {}

        override fun onSurfaceCreated(config: EGLConfig) {
            scene.glInit()
            mediaLoader!!.onGlSceneReady(scene)
        }

        override fun onSurfaceChanged(width: Int, height: Int) {}

        override fun onRendererShutdown() {
            scene.glShutdown()
        }

    }

    /** Forwards Controller events to SceneRenderer.  */
    private inner class ControllerEventListener : Controller.EventListener(), ControllerManager.EventListener {
        private var touchpadDown = false
        private var appButtonDown = false

        override fun onApiStatusChanged(status: Int) {
            Log.i(TAG, ".onApiStatusChanged $status")
        }

        override fun onRecentered() {}

        override fun onUpdate() {
            controller!!.update()

            renderer!!.scene.setControllerOrientation(controller!!.orientation)

            if (!touchpadDown && controller!!.clickButtonState) {
                renderer!!.scene.handleClick()
            }

            if (!appButtonDown && controller!!.appButtonState) {
                renderer!!.scene.toggleUi()
            }

            touchpadDown = controller!!.clickButtonState
            appButtonDown = controller!!.appButtonState
        }
    }

    companion object {
        private val TAG = "VrVideoActivity"

        private val EXIT_FROM_VR_REQUEST_CODE = 42

        // Renderer options
        private val Z_NEAR = .1f
        private val Z_FAR = 100f
    }
}

