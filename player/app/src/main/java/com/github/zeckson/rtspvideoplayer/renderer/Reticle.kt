package com.github.zeckson.rtspvideoplayer.renderer

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.vr.sdk.controller.Orientation

/**
 * Renders a reticle in VR for the Daydream Controller.
 *
 *
 * This is a minimal example that renders a circle at 1 meter from the user based on the rotation
 * of the controller.
 */
internal class Reticle {

    // Program-related GL items. These are only valid if program != 0.
    private var program = 0
    private var mvpMatrixHandle: Int = 0
    private var positionHandle: Int = 0

    // The reticle doesn't have a real modelMatrix. Its distance is baked into the mesh and it
    // uses a rotation matrix when rendered.
    private val modelViewProjectionMatrix = FloatArray(16)

    /** Finishes initialization of this object on the GL thread.  */
    fun glInit() {
        if (program != 0) {
            return
        }

        program = Utils.compileProgram(vertexShaderCode, fragmentShaderCode)
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMvpMatrix")
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        Utils.checkGlError()
    }

    /**
     * Renders the reticle.
     *
     * @param viewProjectionMatrix Scene's view projection matrix.
     * @param orientation Rotation matrix derived from [Orientation.toRotationMatrix].
     */
    fun glDraw(viewProjectionMatrix: FloatArray, orientation: FloatArray) {
        // Configure shader.
        GLES20.glUseProgram(program)
        Utils.checkGlError()

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, orientation, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0)
        Utils.checkGlError()

        // Render quad.
        GLES20.glEnableVertexAttribArray(positionHandle)
        Utils.checkGlError()

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )
        Utils.checkGlError()

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexData.size / COORDS_PER_VERTEX)
        Utils.checkGlError()

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    /** Frees GL resources.  */
    fun glShutdown() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
        }
    }

    companion object {
        // The reticle quad is 2 * SIZE units.
        private val SIZE = .01f
        private val DISTANCE = 1f

        // Standard vertex shader.
        private val vertexShaderCode = arrayOf(
            "uniform mat4 uMvpMatrix;", "attribute vec3 aPosition;", "varying vec2 vCoords;",

            // Passthrough normalized vertex coordinates.
            "void main() {", "  gl_Position = uMvpMatrix * vec4(aPosition, 1);",
            "  vCoords = aPosition.xy / vec2($SIZE, $SIZE);", "}"
        )

        // Procedurally render a ring on the quad between the specified radii.
        private val fragmentShaderCode = arrayOf(
            "precision mediump float;",
            "varying vec2 vCoords;",

            // Simple ring shader that is white between the radii and transparent elsewhere.
            "void main() {",
            "  float r = length(vCoords);",
            // Blend the edges of the ring at .55 +/- .05 and .85 +/- .05.
            "  float alpha = smoothstep(0.5, 0.6, r) * (1.0 - smoothstep(0.8, 0.9, r));",
            "  if (alpha == 0.0) {",
            "    discard;",
            "  } else {",
            "    gl_FragColor = vec4(alpha);",
            "  }",
            "}"
        )

        // Simple quad mesh.
        private val COORDS_PER_VERTEX = 3
        private val vertexData =
            floatArrayOf(-SIZE, -SIZE, -DISTANCE, SIZE, -SIZE, -DISTANCE, -SIZE, SIZE, -DISTANCE, SIZE, SIZE, -DISTANCE)
        private val vertexBuffer = Utils.createBuffer(vertexData)
    }
}
