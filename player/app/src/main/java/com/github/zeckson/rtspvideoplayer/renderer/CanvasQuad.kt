package com.github.zeckson.rtspvideoplayer.renderer

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import android.widget.FrameLayout
import com.google.vr.sdk.controller.Orientation
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Renders a floating, textured, translucent quad in VR at a hardcoded distance.
 *
 *
 * In this sample, the class is only used to render the Android View containing the UI. It also
 * contains the [Surface] and [SurfaceTexture] which hold the [Canvas] that
 * VideoUiView renders to.
 *
 *
 * A CanvasQuad can be created on any thread, but [.glInit] needs to be called on
 * the GL thread before it can be rendered.
 */
class CanvasQuad
/** Only SceneRenderer can create a CanvasQuad.  */
/* package */ internal constructor() {

    // Program-related GL items. These are only valid if program != 0.
    private var program = 0
    private var mvpMatrixHandle: Int = 0
    private var positionHandle: Int = 0
    private var textureCoordsHandle: Int = 0
    private var textureHandle: Int = 0
    private var textureId: Int = 0
    private var alphaHandle: Int = 0

    // Components used to manage the Canvas that the View is rendered to. These are only valid after
    // GL initialization. The client of this class acquires a Canvas from the Surface, writes to it
    // and posts it. This marks the Surface as dirty. The GL code then updates the SurfaceTexture
    // when rendering only if it is dirty.
    private var displaySurfaceTexture: SurfaceTexture? = null
    private var displaySurface: Surface? = null
    private val surfaceDirty = AtomicBoolean()

    /**
     * Calls [Surface.lockCanvas].
     *
     * @return [Canvas] for the View to render to or `null` if [.glInit] has not
     * yet been called.
     */
    fun lockCanvas(): Canvas? {
        return if (displaySurface == null) null else displaySurface!!.lockCanvas(null/* dirty Rect */)
    }

    /**
     * Calls [Surface.unlockCanvasAndPost] and marks the SurfaceTexture as dirty.
     *
     * @param canvas the canvas returned from [.lockCanvas]
     */
    fun unlockCanvasAndPost(canvas: Canvas?) {
        if (canvas == null || displaySurface == null) {
            // glInit() hasn't run yet.
            return
        }
        displaySurface!!.unlockCanvasAndPost(canvas)
        surfaceDirty.set(true)
    }

    /** Finishes constructing this object on the GL Thread.  */
    /* package */ internal fun glInit() {
        if (program != 0) {
            return
        }

        // Create the program.
        program = Utils.compileProgram(vertexShaderCode, fragmentShaderCode)
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMvpMatrix")
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        textureCoordsHandle = GLES20.glGetAttribLocation(program, "aTexCoords")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        textureId = Utils.glCreateExternalTexture()
        alphaHandle = GLES20.glGetUniformLocation(program, "uAlpha")
        Utils.checkGlError()

        // Create the underlying SurfaceTexture with the appropriate size.
        displaySurfaceTexture = SurfaceTexture(textureId)
        displaySurfaceTexture!!.setDefaultBufferSize(
            (WIDTH * PX_PER_UNIT).toInt(), (HEIGHT * PX_PER_UNIT).toInt()
        )
        displaySurface = Surface(displaySurfaceTexture)
    }

    /**
     * Renders the quad.
     *
     * @param viewProjectionMatrix Array of floats containing the quad's 4x4 perspective matrix in the
     * [android.opengl.Matrix] format.
     * @param alpha Specifies the opacity of this quad.
     */
    /* package */ internal fun glDraw(viewProjectionMatrix: FloatArray, alpha: Float) {
        // Configure shader.
        GLES20.glUseProgram(program)
        Utils.checkGlError()

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(textureCoordsHandle)
        Utils.checkGlError()

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, viewProjectionMatrix, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glUniform1f(alphaHandle, alpha)
        Utils.checkGlError()

        // Load position data.
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle, POSITION_COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, VERTEX_STRIDE_BYTES, vertexBuffer
        )
        Utils.checkGlError()

        // Load texture data.
        vertexBuffer.position(POSITION_COORDS_PER_VERTEX)
        GLES20.glVertexAttribPointer(
            textureCoordsHandle, TEXTURE_COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, VERTEX_STRIDE_BYTES, vertexBuffer
        )
        Utils.checkGlError()

        if (surfaceDirty.compareAndSet(true, false)) {
            // If the Surface has been written to, get the new data onto the SurfaceTexture.
            displaySurfaceTexture!!.updateTexImage()
        }

        // Render.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexData.size / COORDS_PER_VERTEX)
        Utils.checkGlError()

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordsHandle)
    }

    /** Frees GL resources.  */
    /* package */ internal fun glShutdown() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        }

        if (displaySurfaceTexture != null) {
            displaySurfaceTexture!!.release()
        }
    }

    companion object {
        // The size of the quad is hardcoded for this sample and the quad doesn't have a model matrix so
        // these dimensions are used by translateClick() for touch interaction.
        private val WIDTH = 1f
        private val HEIGHT = 1 / 8f
        private val DISTANCE = 1f
        // The number of pixels in this quad affect how Android positions Views in it. VideoUiView in VR
        // will be 1024 x 128 px in size which is similar to its 2D size. For Views that only have VR
        // layouts, using a number that results in ~10-15 px / degree is good.
        val PX_PER_UNIT = 1024

        // Standard vertex shader that passes through the texture data.
        private val vertexShaderCode = arrayOf(
            "uniform mat4 uMvpMatrix;",
            // 3D position data.
            "attribute vec3 aPosition;",
            // 2D UV vertices.
            "attribute vec2 aTexCoords;", "varying vec2 vTexCoords;",

            // Standard transformation.
            "void main() {", "  gl_Position = uMvpMatrix * vec4(aPosition, 1);", "  vTexCoords = aTexCoords;", "}"
        )

        // Renders the texture of the quad using uAlpha for transparency.
        private val fragmentShaderCode = arrayOf(
            // This is required since the texture data is GL_TEXTURE_EXTERNAL_OES.
            "#extension GL_OES_EGL_image_external : require",
            "precision mediump float;",

            // Standard texture rendering shader with extra alpha channel.
            "uniform samplerExternalOES uTexture;",
            "uniform float uAlpha;",
            "varying vec2 vTexCoords;",
            "void main() {",
            "  gl_FragColor.xyz = texture2D(uTexture, vTexCoords).xyz;",
            "  gl_FragColor.a = uAlpha;",
            "}"
        )

        // The quad has 2 triangles built from 4 total vertices. Each vertex has 3 position & 2 texture
        // coordinates.
        private val POSITION_COORDS_PER_VERTEX = 3
        private val TEXTURE_COORDS_PER_VERTEX = 2
        private val COORDS_PER_VERTEX = POSITION_COORDS_PER_VERTEX + TEXTURE_COORDS_PER_VERTEX
        private val BYTES_PER_COORD = 4  // float.
        private val VERTEX_STRIDE_BYTES = COORDS_PER_VERTEX * BYTES_PER_COORD

        // Interlaced position & texture data.
        private val vertexData = floatArrayOf(
            -WIDTH / 2,
            -HEIGHT / 2,
            -DISTANCE,
            0f,
            1f,
            WIDTH / 2,
            -HEIGHT / 2,
            -DISTANCE,
            1f,
            1f,
            -WIDTH / 2,
            HEIGHT / 2,
            -DISTANCE,
            0f,
            0f,
            WIDTH / 2,
            HEIGHT / 2,
            -DISTANCE,
            1f,
            0f
        )
        private val vertexBuffer = Utils.createBuffer(vertexData)

        /** Gets LayoutParams used by Android to properly layout VideoUiView.  */
        val layoutParams: FrameLayout.LayoutParams
            get() = FrameLayout.LayoutParams((WIDTH * PX_PER_UNIT).toInt(), (HEIGHT * PX_PER_UNIT).toInt())

        /**
         * Translates a Daydream Controller Orientation into a Point that can be passed to Android's
         * click handling system.
         *
         *
         * This is a minimal hit detection system that works for this quad because
         * it has no model matrix. All the math is based on the fact that its size & distance are
         * hard-coded into this class. For a more complex 3D mesh, a general bounding box & ray collision
         * system would be required.
         *
         * @param orientation a [com.google.vr.sdk.controller.Controller]'s [Orientation].
         */
        /* package */ internal fun translateClick(orientation: Orientation): PointF? {
            val angles = orientation.toYawPitchRollRadians(FloatArray(3))
            // Make a rough guess of the bounds of the Quad in polar coordinates. This works as long as the
            // Quad isn't too large.
            val horizontalHalfAngle = Math.atan2((WIDTH / 2).toDouble(), DISTANCE.toDouble()).toFloat()
            val verticleHalfAngle = Math.atan2((HEIGHT / 2).toDouble(), DISTANCE.toDouble()).toFloat()

            if (angles[1] < -verticleHalfAngle || angles[1] > verticleHalfAngle
                || angles[0] < -horizontalHalfAngle || angles[0] > horizontalHalfAngle
            ) {
                // Click is outside of the quad.
                return null
            }

            // Convert from the polar coordinates of the controller to the rectangular coordinates of the
            // View. Note the negative yaw & pitch used to generate Android-compliant x & y coordinates.
            val xPercent = (horizontalHalfAngle - angles[0]) / (2 * horizontalHalfAngle)
            val yPercent = (verticleHalfAngle - angles[1]) / (2 * verticleHalfAngle)
            val xPx = xPercent * WIDTH * PX_PER_UNIT.toFloat()
            val yPx = yPercent * HEIGHT * PX_PER_UNIT.toFloat()

            return PointF(xPx, yPx)
        }
    }
}
