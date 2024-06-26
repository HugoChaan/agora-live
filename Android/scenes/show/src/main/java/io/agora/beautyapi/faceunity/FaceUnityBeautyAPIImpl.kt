/*
 * MIT License
 *
 * Copyright (c) 2023 Agora Community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.agora.beautyapi.faceunity

import android.graphics.Matrix
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import com.faceunity.core.entity.FUBundleData
import com.faceunity.core.entity.FURenderInputData
import com.faceunity.core.enumeration.CameraFacingEnum
import com.faceunity.core.enumeration.FUInputBufferEnum
import com.faceunity.core.enumeration.FUInputTextureEnum
import com.faceunity.core.enumeration.FUTransformMatrixEnum
import com.faceunity.core.faceunity.FUAIKit
import com.faceunity.core.faceunity.FURenderKit
import com.faceunity.core.model.facebeauty.FaceBeauty
import com.faceunity.core.model.facebeauty.FaceBeautyFilterEnum
import io.agora.base.TextureBufferHelper
import io.agora.base.VideoFrame
import io.agora.base.VideoFrame.I420Buffer
import io.agora.base.VideoFrame.SourceType
import io.agora.base.VideoFrame.TextureBuffer
import io.agora.base.internal.video.YuvHelper
import io.agora.beautyapi.faceunity.utils.FuDeviceUtils
import io.agora.beautyapi.faceunity.utils.LogUtils
import io.agora.beautyapi.faceunity.utils.StatsHelper
import io.agora.beautyapi.faceunity.utils.egl.TextureProcessHelper
import io.agora.rtc2.Constants
import io.agora.rtc2.gl.EglBaseProvider
import io.agora.rtc2.video.IVideoFrameObserver
import io.agora.rtc2.video.VideoCanvas
import java.io.File
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.Callable

/**
 * Face unity beauty a p i impl
 *
 * @constructor Create empty Face unity beauty a p i impl
 */
class FaceUnityBeautyAPIImpl : FaceUnityBeautyAPI, IVideoFrameObserver {
    /**
     * Tag
     */
    private val TAG = "FaceUnityBeautyAPIImpl"

    /**
     * Report id
     */
    private val reportId = "scenarioAPI"

    /**
     * Report category
     */
    private val reportCategory = "beauty_android_$VERSION"

    /**
     * Beauty Mode
     */
    private var beautyMode = 0 // 0: 自动根据buffer类型切换，1：固定使用OES纹理，2：固定使用i420，3: 单纹理异步模式(自创)

    /**
     * Texture buffer helper
     */
    private var textureBufferHelper: TextureBufferHelper? = null

    /**
     * Wrap texture buffer helper
     */
    private var wrapTextureBufferHelper: TextureBufferHelper? = null

    /**
     * Byte buffer
     */
    private var byteBuffer: ByteBuffer? = null

    /**
     * Byte array
     */
    private var byteArray: ByteArray? = null

    /**
     * Config
     */
    private var config: Config? = null

    /**
     * Enable
     */
    private var enable: Boolean = false

    /**
     * Enable change
     */
    private var enableChange: Boolean = false

    /**
     * Is released
     */
    private var isReleased: Boolean = false

    /**
     * Capture mirror
     */
    private var captureMirror = false

    /**
     * Render mirror
     */
    private var renderMirror = false

    /**
     * Identity matrix
     */
    private val identityMatrix =  Matrix()

    /**
     * M texture process helper
     */
    private var mTextureProcessHelper: TextureProcessHelper? = null

    /**
     * Stats helper
     */
    private var statsHelper: StatsHelper? = null

    /**
     * Skip frame
     */
    private var skipFrame = 0

    /**
     * Process source type
     *
     * @constructor Create empty Process source type
     */
    private enum class ProcessSourceType{
        /**
         * Unknown
         *
         * @constructor Create empty Unknown
         */
        UNKNOWN,

        /**
         * Texture Oes Async
         *
         * @constructor Create empty Texture Oes Async
         */
        TEXTURE_OES_ASYNC,

        /**
         * Texture 2d Async
         *
         * @constructor Create empty Texture 2d Async
         */
        TEXTURE_2D_ASYNC,

        /**
         * I420
         *
         * @constructor Create empty I420
         */
        I420
    }

    /**
     * Curr process source type
     */
    private var currProcessSourceType = ProcessSourceType.UNKNOWN

    /**
     * Device level
     */
    private var deviceLevel = FuDeviceUtils.DEVICEINFO_UNKNOWN

    /**
     * Is front camera
     */
    private var isFrontCamera = true

    /**
     * Camera config
     */
    private var cameraConfig = CameraConfig()

    /**
     * Local video render mode
     */
    private var localVideoRenderMode = Constants.RENDER_MODE_HIDDEN

    /**
     * Pending ProcessRunList
     */
    private val pendingProcessRunList = Collections.synchronizedList(mutableListOf<()->Unit>())

    /**
     * Initialize
     *
     * @param config
     * @return
     */
    override fun initialize(config: Config): Int {
        if (this.config != null) {
            LogUtils.e(TAG, "initialize >> The beauty api has been initialized!")
            return ErrorCode.ERROR_HAS_INITIALIZED.value
        }
        this.config = config
        if (config.captureMode == CaptureMode.Agora) {
            config.rtcEngine.registerVideoFrameObserver(this)
        }
        statsHelper = StatsHelper(config.statsDuration){
            this.config?.eventCallback?.onBeautyStats(it)
        }
        LogUtils.i(TAG, "initialize >> config = $config")
        LogUtils.i(TAG, "initialize >> beauty api version=$VERSION, beauty sdk version=${FURenderKit.getInstance().getVersion()}")

        // config face beauty
        if (deviceLevel == FuDeviceUtils.DEVICEINFO_UNKNOWN) {
            deviceLevel = FuDeviceUtils.judgeDeviceLevel(config.context)
            FUAIKit.getInstance().faceProcessorSetFaceLandmarkQuality(deviceLevel)
            if (deviceLevel > FuDeviceUtils.DEVICE_LEVEL_MID) {
                FUAIKit.getInstance().fuFaceProcessorSetDetectSmallFace(true)
            }
        }
        LogUtils.i(TAG, "initialize >> FuDeviceUtils deviceLevel=$deviceLevel")
        config.rtcEngine.sendCustomReportMessage(reportId, reportCategory, "initialize", "config=$config, deviceLevel=$deviceLevel", 0)
        return ErrorCode.ERROR_OK.value
    }

    /**
     * Enable
     *
     * @param enable
     * @return
     */
    override fun enable(enable: Boolean): Int {
        LogUtils.i(TAG, "enable >> enable = $enable")
        if (config == null) {
            LogUtils.e(TAG, "enable >> The beauty api has not been initialized!")
            return ErrorCode.ERROR_HAS_NOT_INITIALIZED.value
        }
        if (isReleased) {
            LogUtils.e(TAG, "enable >> The beauty api has been released!")
            return ErrorCode.ERROR_HAS_RELEASED.value
        }
        if(config?.captureMode == CaptureMode.Custom){
            skipFrame = 2
            LogUtils.i(TAG, "enable >> skipFrame = $skipFrame")
        }
        config?.rtcEngine?.sendCustomReportMessage(reportId, reportCategory, "enable", "enable=$enable", 0)

        if(this.enable != enable){
            this.enable = enable
            enableChange = true
            LogUtils.i(TAG, "enable >> enableChange")
        }
        return ErrorCode.ERROR_OK.value
    }

    /**
     * Setup local video
     *
     * @param view
     * @param renderMode
     * @return
     */
    override fun setupLocalVideo(view: View, renderMode: Int): Int {
        val rtcEngine = config?.rtcEngine
        if(rtcEngine == null){
            LogUtils.e(TAG, "setupLocalVideo >> The beauty api has not been initialized!")
            return ErrorCode.ERROR_HAS_NOT_INITIALIZED.value
        }
        LogUtils.i(TAG, "setupLocalVideo >> view=$view, renderMode=$renderMode")
        localVideoRenderMode = renderMode
        rtcEngine.sendCustomReportMessage(reportId, reportCategory, "enable", "view=$view, renderMode=$renderMode", 0)
        if (view is TextureView || view is SurfaceView) {
            val canvas = VideoCanvas(view, renderMode, 0)
            canvas.mirrorMode = Constants.VIDEO_MIRROR_MODE_DISABLED
            rtcEngine.setupLocalVideo(canvas)
            return ErrorCode.ERROR_OK.value
        }
        return ErrorCode.ERROR_VIEW_TYPE_ERROR.value
    }

    /**
     * On frame
     *
     * @param videoFrame
     * @return
     */
    override fun onFrame(videoFrame: VideoFrame): Int {
        val conf = config
        if(conf == null){
            LogUtils.e(TAG, "onFrame >> The beauty api has not been initialized!")
            return ErrorCode.ERROR_HAS_NOT_INITIALIZED.value
        }
        if (isReleased) {
            LogUtils.e(TAG, "onFrame >> The beauty api has been released!")
            return ErrorCode.ERROR_HAS_RELEASED.value
        }
        if (conf.captureMode != CaptureMode.Custom) {
            LogUtils.e(TAG, "onFrame >> The capture mode is not Custom!")
            return ErrorCode.ERROR_PROCESS_NOT_CUSTOM.value
        }
        if (processBeauty(videoFrame)) {
            return ErrorCode.ERROR_OK.value
        }
        LogUtils.i(TAG, "onFrame >> Skip Frame.")
        return ErrorCode.ERROR_FRAME_SKIPPED.value
    }

    /**
     * Update camera config
     *
     * @param config
     * @return
     */
    override fun updateCameraConfig(config: CameraConfig): Int {
        LogUtils.i(TAG, "updateCameraConfig >> oldCameraConfig=$cameraConfig, newCameraConfig=$config")
        cameraConfig = CameraConfig(config.frontMirror, config.backMirror)
        this.config?.rtcEngine?.sendCustomReportMessage(reportId, reportCategory, "updateCameraConfig", "config=$config", 0)

        return ErrorCode.ERROR_OK.value
    }

    override fun runOnProcessThread(run: () -> Unit) {
        if (config == null) {
            LogUtils.e(TAG, "runOnProcessThread >> The beauty api has not been initialized!")
            return
        }
        if (isReleased) {
            LogUtils.e(TAG, "runOnProcessThread >> The beauty api has been released!")
            return
        }
        if (textureBufferHelper?.handler?.looper?.thread == Thread.currentThread()) {
            run.invoke()
        } else if (textureBufferHelper != null) {
            textureBufferHelper?.handler?.post(run)
        } else {
            pendingProcessRunList.add(run)
        }
    }

    override fun isFrontCamera() = isFrontCamera

    /**
     * Set parameters
     *
     * @param key
     * @param value
     */
    override fun setParameters(key: String, value: String) {
        when(key){
            "beauty_mode" -> beautyMode = value.toInt()
        }
    }

    /**
     * Set beauty preset
     *
     * @param preset
     * @return
     */
    override fun setBeautyPreset(preset: BeautyPreset): Int {
        val conf = config
        if(conf == null){
            LogUtils.e(TAG, "setBeautyPreset >> The beauty api has not been initialized!")
            return ErrorCode.ERROR_HAS_NOT_INITIALIZED.value
        }
        if (isReleased) {
            LogUtils.e(TAG, "setBeautyPreset >> The beauty api has been released!")
            return ErrorCode.ERROR_HAS_RELEASED.value
        }

        LogUtils.i(TAG, "setBeautyPreset >> preset = $preset")
        config?.rtcEngine?.sendCustomReportMessage(reportId, reportCategory, "enable", "preset=$preset", 0)

        val recommendFaceBeauty = conf.fuRenderKit.faceBeauty ?:
            FaceBeauty(FUBundleData("graphics" + File.separator + "face_beautification.bundle"))
        if (preset == BeautyPreset.DEFAULT) {
            recommendFaceBeauty.filterName = FaceBeautyFilterEnum.FENNEN_1
            recommendFaceBeauty.filterIntensity = 0.7
            recommendFaceBeauty.toothIntensity = 0.3
            recommendFaceBeauty.eyeBrightIntensity = 0.3
            recommendFaceBeauty.eyeEnlargingIntensity = 0.5
            recommendFaceBeauty.redIntensity = 0.5 * 2
            recommendFaceBeauty.colorIntensity = 0.75 * 2
            recommendFaceBeauty.blurIntensity = 0.75 * 6
            if (deviceLevel > FuDeviceUtils.DEVICE_LEVEL_MID) {
                val score = FUAIKit.getInstance().getFaceProcessorGetConfidenceScore(0)
                if (score > 0.95) {
                    recommendFaceBeauty.blurType = 3
                    recommendFaceBeauty.enableBlurUseMask = true
                } else {
                    recommendFaceBeauty.blurType = 2
                    recommendFaceBeauty.enableBlurUseMask = false
                }
            } else {
                recommendFaceBeauty.blurType = 2
                recommendFaceBeauty.enableBlurUseMask = false
            }
            recommendFaceBeauty.mouthIntensity = 0.3
            recommendFaceBeauty.noseIntensity = 0.1
            recommendFaceBeauty.forHeadIntensity = 0.3
            recommendFaceBeauty.chinIntensity = 0.0
            recommendFaceBeauty.cheekThinningIntensity = 0.3
            recommendFaceBeauty.cheekNarrowIntensity = 0.0
            recommendFaceBeauty.cheekSmallIntensity = 0.0
            recommendFaceBeauty.cheekVIntensity = 0.0
        }
        conf.fuRenderKit.faceBeauty = recommendFaceBeauty
        return ErrorCode.ERROR_OK.value
    }

    /**
     * Release
     *
     * @return
     */
    override fun release(): Int {
        val conf = config
        val fuRenderer = conf?.fuRenderKit
        if(fuRenderer == null){
            LogUtils.e(TAG, "release >> The beauty api has not been initialized!")
            return ErrorCode.ERROR_HAS_NOT_INITIALIZED.value
        }
        if (isReleased) {
            LogUtils.e(TAG, "setBeautyPreset >> The beauty api has been released!")
            return ErrorCode.ERROR_HAS_RELEASED.value
        }
        LogUtils.i(TAG, "release")
        if (conf.captureMode == CaptureMode.Agora) {
            conf.rtcEngine.registerVideoFrameObserver(null)
        }
        conf.rtcEngine.sendCustomReportMessage(reportId, reportCategory, "release", "", 0)

        isReleased = true
        textureBufferHelper?.let {
            textureBufferHelper = null
            it.handler.removeCallbacksAndMessages(null)
            it.invoke {
                fuRenderer.release()
                mTextureProcessHelper?.release()
                mTextureProcessHelper = null
                null
            }
            // it.handler.looper.quit()
            it.dispose()
        }
        wrapTextureBufferHelper?.let {
            wrapTextureBufferHelper = null
            it.dispose()
        }
        statsHelper?.reset()
        statsHelper = null
        pendingProcessRunList.clear()
        return ErrorCode.ERROR_OK.value
    }

    /**
     * Process beauty
     *
     * @param videoFrame
     * @return
     */
    private fun processBeauty(videoFrame: VideoFrame): Boolean {
        if (isReleased) {
            LogUtils.e(TAG, "processBeauty >> The beauty api has been released!")
            return false
        }

        val cMirror =
            if (isFrontCamera) {
                when (cameraConfig.frontMirror) {
                    MirrorMode.MIRROR_LOCAL_REMOTE -> true
                    MirrorMode.MIRROR_LOCAL_ONLY -> false
                    MirrorMode.MIRROR_REMOTE_ONLY -> true
                    MirrorMode.MIRROR_NONE -> false
                }
            } else {
                when (cameraConfig.backMirror) {
                    MirrorMode.MIRROR_LOCAL_REMOTE -> true
                    MirrorMode.MIRROR_LOCAL_ONLY -> false
                    MirrorMode.MIRROR_REMOTE_ONLY -> true
                    MirrorMode.MIRROR_NONE -> false
                }
            }
        val rMirror =
            if (isFrontCamera) {
                when (cameraConfig.frontMirror) {
                    MirrorMode.MIRROR_LOCAL_REMOTE -> false
                    MirrorMode.MIRROR_LOCAL_ONLY -> true
                    MirrorMode.MIRROR_REMOTE_ONLY -> true
                    MirrorMode.MIRROR_NONE -> false
                }
            } else {
                when (cameraConfig.backMirror) {
                    MirrorMode.MIRROR_LOCAL_REMOTE -> false
                    MirrorMode.MIRROR_LOCAL_ONLY -> true
                    MirrorMode.MIRROR_REMOTE_ONLY -> true
                    MirrorMode.MIRROR_NONE -> false
                }
            }
        if (captureMirror != cMirror || renderMirror != rMirror) {
            LogUtils.w(TAG, "processBeauty >> enable=$enable, captureMirror=$captureMirror->$cMirror, renderMirror=$renderMirror->$rMirror")
            captureMirror = cMirror
            if(renderMirror != rMirror){
                renderMirror = rMirror
                config?.rtcEngine?.setLocalRenderMode(
                    localVideoRenderMode,
                    if(renderMirror) Constants.VIDEO_MIRROR_MODE_ENABLED else Constants.VIDEO_MIRROR_MODE_DISABLED
                )
            }
            textureBufferHelper?.invoke {
                mTextureProcessHelper?.reset()
            }
            skipFrame = 2
            return false
        }

        val oldIsFrontCamera = isFrontCamera
        isFrontCamera = videoFrame.sourceType == SourceType.kFrontCamera
        if(oldIsFrontCamera != isFrontCamera){
            LogUtils.w(TAG, "processBeauty >> oldIsFrontCamera=$oldIsFrontCamera, isFrontCamera=$isFrontCamera")
            return false
        }

        if(enableChange){
            enableChange = false
            textureBufferHelper?.invoke {
                mTextureProcessHelper?.reset()
            }
            return false
        }

        if(!enable){
            return true
        }

        if (textureBufferHelper == null) {
            textureBufferHelper = TextureBufferHelper.create(
                "FURender",
                EglBaseProvider.instance().rootEglBase.eglBaseContext
            )
            textureBufferHelper?.invoke {
                synchronized(pendingProcessRunList){
                    val iterator = pendingProcessRunList.iterator()
                    while (iterator.hasNext()){
                        iterator.next().invoke()
                        iterator.remove()
                    }
                }
            }
            LogUtils.i(TAG, "processBeauty >> create texture buffer, beautyMode=$beautyMode")
        }
        if (wrapTextureBufferHelper == null) {
            wrapTextureBufferHelper = TextureBufferHelper.create(
                "FURenderWrap",
                EglBaseProvider.instance().rootEglBase.eglBaseContext
            )
            LogUtils.i(TAG, "processBeauty >> create texture buffer wrap, beautyMode=$beautyMode")
        }
        val startTime = System.currentTimeMillis()
        val processTexId = when (beautyMode) {
            2 -> processBeautySingleBuffer(videoFrame)
            3 -> processBeautySingleTextureAsync(videoFrame)
            else -> processBeautyAuto(videoFrame)
        }

        if(config?.statsEnable == true){
            val costTime = System.currentTimeMillis() - startTime
            statsHelper?.once(costTime)
        }

        if (processTexId <= 0) {
            LogUtils.w(TAG, "processBeauty >> processTexId <= 0")
            return false
        }

        if(skipFrame > 0){
            skipFrame --
            LogUtils.w(TAG, "processBeauty >> skipFrame=$skipFrame")
            return false
        }

        val processBuffer: TextureBuffer = wrapTextureBufferHelper?.wrapTextureBuffer(
            videoFrame.rotatedWidth,
            videoFrame.rotatedHeight,
            TextureBuffer.Type.RGB,
            processTexId,
            identityMatrix
        ) ?: return false
        videoFrame.replaceBuffer(processBuffer, 0, videoFrame.timestampNs)
        return true
    }

    /**
     * Process beauty auto
     *
     * @param videoFrame
     * @return
     */
    private fun processBeautyAuto(videoFrame: VideoFrame): Int {
        val buffer = videoFrame.buffer
        return if (buffer is TextureBuffer) {
            processBeautySingleTextureAsync(videoFrame)
        } else {
            processBeautySingleBuffer(videoFrame)
        }
    }

    /**
     * Process beauty single texture async
     *
     * @param videoFrame
     * @return
     */
    private fun processBeautySingleTextureAsync(videoFrame: VideoFrame): Int {
        val texBufferHelper = wrapTextureBufferHelper ?: return -1
        val textureBuffer = videoFrame.buffer as? TextureBuffer ?: return -1

        when(textureBuffer.type){
            TextureBuffer.Type.OES -> {
                if(currProcessSourceType != ProcessSourceType.TEXTURE_OES_ASYNC){
                    LogUtils.i(TAG, "processBeauty >> process source type change old=$currProcessSourceType, new=${ProcessSourceType.TEXTURE_OES_ASYNC}")
                    if (currProcessSourceType != ProcessSourceType.UNKNOWN) {
                        skipFrame = 3
                    }
                    currProcessSourceType = ProcessSourceType.TEXTURE_OES_ASYNC
                    return -1
                }
            }
            else -> {
                if(currProcessSourceType != ProcessSourceType.TEXTURE_2D_ASYNC){
                    LogUtils.i(TAG, "processBeauty >> process source type change old=$currProcessSourceType, new=${ProcessSourceType.TEXTURE_2D_ASYNC}")
                    if (currProcessSourceType != ProcessSourceType.UNKNOWN) {
                        skipFrame = 3
                    }
                    currProcessSourceType = ProcessSourceType.TEXTURE_2D_ASYNC
                    skipFrame = 6
                    return -1
                }
            }
        }

        if(mTextureProcessHelper == null) {
            mTextureProcessHelper = TextureProcessHelper()
            mTextureProcessHelper?.setFilter { frame ->
                val fuRenderKit = config?.fuRenderKit ?: return@setFilter -1

                val input = FURenderInputData(frame.width, frame.height)
                input.texture = FURenderInputData.FUTexture(
                    FUInputTextureEnum.FU_ADM_FLAG_COMMON_TEXTURE,
                    frame.textureId
                )
                val isFront = frame.isFrontCamera
                input.renderConfig.let {
                    if (isFront) {
                        it.cameraFacing = CameraFacingEnum.CAMERA_FRONT
                        it.inputBufferMatrix = FUTransformMatrixEnum.CCROT0
                        it.inputTextureMatrix = FUTransformMatrixEnum.CCROT0
                        it.outputMatrix = FUTransformMatrixEnum.CCROT0
                        it.deviceOrientation = 270
                    } else {
                        it.cameraFacing = CameraFacingEnum.CAMERA_BACK
                        it.inputBufferMatrix = FUTransformMatrixEnum.CCROT0
                        it.inputTextureMatrix = FUTransformMatrixEnum.CCROT0
                        it.outputMatrix = FUTransformMatrixEnum.CCROT0
                        it.deviceOrientation = 270
                    }
                }
                if (isReleased) {
                    return@setFilter -1
                }
                val ret = textureBufferHelper?.invoke {
                    return@invoke fuRenderKit.renderWithInput(input).texture?.texId ?: -1
                }
                return@setFilter ret ?: -1
            }
        }

        return texBufferHelper.invoke {
            if(isReleased){
                return@invoke -1
            }

            return@invoke mTextureProcessHelper?.process(
                textureBuffer.textureId,
                when (textureBuffer.type) {
                    TextureBuffer.Type.OES -> GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                    else -> GLES20.GL_TEXTURE_2D
                },
                textureBuffer.width,
                textureBuffer.height,
                videoFrame.rotation,
                textureBuffer.transformMatrixArray,
                isFrontCamera,
                (isFrontCamera && !captureMirror) || (!isFrontCamera && captureMirror)
            )?: -1
        }
    }

    /**
     * Process beauty single buffer
     *
     * @param videoFrame
     * @return
     */
    private fun processBeautySingleBuffer(videoFrame: VideoFrame): Int {
        val texBufferHelper = textureBufferHelper ?: return -1
        if(currProcessSourceType != ProcessSourceType.I420){
            LogUtils.i(TAG, "processBeauty >> process source type change old=$currProcessSourceType, new=${ProcessSourceType.I420}")
            if (currProcessSourceType != ProcessSourceType.UNKNOWN) {
                skipFrame = 3
            }
            currProcessSourceType = ProcessSourceType.I420
            return -1
        }
        val bufferArray = getNV21Buffer(videoFrame) ?: return -1
        val buffer = videoFrame.buffer
        val width = buffer.width
        val height = buffer.height
        val isFront = videoFrame.sourceType == SourceType.kFrontCamera
        val mirror = (isFrontCamera && !captureMirror) || (!isFrontCamera && captureMirror)
        val rotation = videoFrame.rotation

        return texBufferHelper.invoke(Callable {
            if(isReleased){
                return@Callable -1
            }
            val fuRenderKit = config?.fuRenderKit ?: return@Callable -1
            val input = FURenderInputData(width, height)
            input.imageBuffer = FURenderInputData.FUImageBuffer(
                FUInputBufferEnum.FU_FORMAT_NV21_BUFFER,
                bufferArray
            )
            input.renderConfig.let {
                if (isFront) {
                    it.cameraFacing = CameraFacingEnum.CAMERA_FRONT
                    it.inputBufferMatrix = if(mirror) {
                        when (rotation) {
                            0 ->  FUTransformMatrixEnum.CCROT0
                            180 -> FUTransformMatrixEnum.CCROT180
                            else -> FUTransformMatrixEnum.CCROT90
                        }
                    } else {
                        when (rotation) {
                            0 -> FUTransformMatrixEnum.CCROT0_FLIPHORIZONTAL
                            180 -> FUTransformMatrixEnum.CCROT0_FLIPVERTICAL
                            else -> FUTransformMatrixEnum.CCROT90_FLIPHORIZONTAL
                        }
                    }
                    it.inputTextureMatrix = if(mirror) {
                        when (rotation) {
                            0 -> FUTransformMatrixEnum.CCROT0
                            180 -> FUTransformMatrixEnum.CCROT180
                            else -> FUTransformMatrixEnum.CCROT90
                        }
                    } else {
                        when (rotation) {
                            0 -> FUTransformMatrixEnum.CCROT0_FLIPHORIZONTAL
                            180 -> FUTransformMatrixEnum.CCROT0_FLIPVERTICAL
                            else -> FUTransformMatrixEnum.CCROT90_FLIPHORIZONTAL
                        }
                    }
                    it.deviceOrientation = when(rotation){
                        0 -> 270
                        180 -> 90
                        else -> 0
                    }
                    it.outputMatrix = FUTransformMatrixEnum.CCROT0
                } else {
                    it.cameraFacing = CameraFacingEnum.CAMERA_BACK
                    it.inputBufferMatrix = if(mirror) {
                        when (rotation) {
                            0 ->  FUTransformMatrixEnum.CCROT0_FLIPHORIZONTAL
                            180 -> FUTransformMatrixEnum.CCROT0_FLIPVERTICAL
                            else -> FUTransformMatrixEnum.CCROT90_FLIPVERTICAL
                        }
                    } else {
                        when (rotation) {
                            0 -> FUTransformMatrixEnum.CCROT0
                            180 -> FUTransformMatrixEnum.CCROT180
                            else -> FUTransformMatrixEnum.CCROT270
                        }
                    }
                    it.inputTextureMatrix = if(mirror) {
                        when (rotation) {
                            0 -> FUTransformMatrixEnum.CCROT0_FLIPHORIZONTAL
                            180 -> FUTransformMatrixEnum.CCROT0_FLIPVERTICAL
                            else -> FUTransformMatrixEnum.CCROT90_FLIPVERTICAL
                        }
                    } else {
                        when (rotation) {
                            0 -> FUTransformMatrixEnum.CCROT0
                            180 -> FUTransformMatrixEnum.CCROT180
                            else -> FUTransformMatrixEnum.CCROT270
                        }
                    }
                    it.deviceOrientation = when(rotation){
                        0 -> 270
                        180 -> 90
                        else -> 0
                    }
                    it.outputMatrix = FUTransformMatrixEnum.CCROT0
                }
            }

            mTextureProcessHelper?.let {
                if(it.size() > 0){
                    it.reset()
                    return@Callable -1
                }
            }
            return@Callable fuRenderKit.renderWithInput(input).texture?.texId ?: -1
        })
    }

    /**
     * Get n v21buffer
     *
     * @param videoFrame
     * @return
     */
    private fun getNV21Buffer(videoFrame: VideoFrame): ByteArray? {
        val buffer = videoFrame.buffer
        val width = buffer.width
        val height = buffer.height
        val size = (width * height * 3.0f / 2.0f + 0.5f).toInt()
        if (byteBuffer == null || byteBuffer?.capacity() != size || byteArray == null || byteArray?.size != size) {
            byteBuffer?.clear()
            byteBuffer = ByteBuffer.allocateDirect(size)
            byteArray = ByteArray(size)
            return null
        }
        val outArray = byteArray ?: return null
        val outBuffer = byteBuffer ?: return null
        val i420Buffer = buffer as? I420Buffer ?: buffer.toI420()
        YuvHelper.I420ToNV12(
            i420Buffer.dataY, i420Buffer.strideY,
            i420Buffer.dataV, i420Buffer.strideV,
            i420Buffer.dataU, i420Buffer.strideU,
            outBuffer, width, height
        )
        outBuffer.position(0)
        outBuffer.get(outArray)
        if(buffer !is I420Buffer){
            i420Buffer.release()
        }
        return outArray
    }

    // IVideoFrameObserver implements

    /**
     * On capture video frame
     *
     * @param sourceType
     * @param videoFrame
     * @return
     */
    override fun onCaptureVideoFrame(sourceType: Int, videoFrame: VideoFrame?): Boolean {
        videoFrame ?: return false
        return processBeauty(videoFrame)
    }

    /**
     * On pre encode video frame
     *
     * @param sourceType
     * @param videoFrame
     */
    override fun onPreEncodeVideoFrame(sourceType: Int, videoFrame: VideoFrame?) = false

    /**
     * On media player video frame
     *
     * @param videoFrame
     * @param mediaPlayerId
     */
    override fun onMediaPlayerVideoFrame(videoFrame: VideoFrame?, mediaPlayerId: Int) = false

    /**
     * On render video frame
     *
     * @param channelId
     * @param uid
     * @param videoFrame
     */
    override fun onRenderVideoFrame(
        channelId: String?,
        uid: Int,
        videoFrame: VideoFrame?
    ) = false

    /**
     * Get video frame process mode
     *
     */
    override fun getVideoFrameProcessMode() = IVideoFrameObserver.PROCESS_MODE_READ_WRITE

    /**
     * Get video format preference
     *
     */
    override fun getVideoFormatPreference() = IVideoFrameObserver.VIDEO_PIXEL_DEFAULT

    /**
     * Get rotation applied
     *
     */
    override fun getRotationApplied() = false

    /**
     * Get mirror applied
     *
     */
    override fun getMirrorApplied() = captureMirror && !enable

    /**
     * Get observed frame position
     *
     */
    override fun getObservedFramePosition() = IVideoFrameObserver.POSITION_POST_CAPTURER

}