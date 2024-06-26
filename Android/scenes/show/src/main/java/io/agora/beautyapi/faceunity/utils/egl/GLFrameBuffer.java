package io.agora.beautyapi.faceunity.utils.egl;

import android.graphics.Matrix;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import io.agora.base.internal.video.GlRectDrawer;
import io.agora.base.internal.video.RendererCommon;

/**
 * The type Gl frame buffer.
 */
public class GLFrameBuffer {

    /**
     * The M framebuffer id.
     */
    private int mFramebufferId = -1;
    /**
     * The M texture id.
     */
    private int mTextureId = -1;
    /**
     * The M width.
     */
    private int mWidth;

    /**
     * The M height.
     */
    private int mHeight;

    /**
     * The M rotation.
     */
    private int mRotation;

    /**
     * The Is flip v.
     */
    private boolean isFlipV;
    /**
     * The Is flip h.
     */
    private boolean isFlipH;
    /**
     * The Is texture inner.
     */
    private boolean isTextureInner;
    /**
     * The Is texture changed.
     */
    private boolean isTextureChanged;
    /**
     * The Is size changed.
     */
    private boolean isSizeChanged;

    /**
     * The Drawer.
     */
    private RendererCommon.GlDrawer drawer;

    /**
     * The M tex matrix.
     */
    private float[] mTexMatrix = GLUtils.IDENTITY_MATRIX;

    /**
     * Instantiates a new Gl frame buffer.
     */
    public GLFrameBuffer() {

    }

    /**
     * Sets size.
     *
     * @param width  the width
     * @param height the height
     * @return the size
     */
    public boolean setSize(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            isSizeChanged = true;
            return true;
        }
        return false;
    }

    /**
     * Sets rotation.
     *
     * @param rotation the rotation
     */
    public void setRotation(int rotation) {
        if (mRotation != rotation) {
            mRotation = rotation;
        }
    }

    /**
     * Sets flip v.
     *
     * @param flipV the flip v
     */
    public void setFlipV(boolean flipV) {
        if (isFlipV != flipV) {
            isFlipV = flipV;
        }
    }

    /**
     * Sets flip h.
     *
     * @param flipH the flip h
     */
    public void setFlipH(boolean flipH) {
        if (isFlipH != flipH) {
            isFlipH = flipH;
        }
    }

    /**
     * Set texture id.
     *
     * @param textureId the texture id
     */
    public void setTextureId(int textureId) {
        if (mTextureId != textureId) {
            deleteTexture();
            mTextureId = textureId;
            isTextureChanged = true;
        }
    }

    /**
     * Get texture id int.
     *
     * @return the int
     */
    public int getTextureId() {
        return mTextureId;
    }

    /**
     * Sets tex matrix.
     *
     * @param matrix the matrix
     */
    public void setTexMatrix(float[] matrix) {
        if (matrix != null) {
            mTexMatrix = matrix;
        } else {
            mTexMatrix = GLUtils.IDENTITY_MATRIX;
        }
    }

    /**
     * Reset transform.
     */
    public void resetTransform() {
        mTexMatrix = GLUtils.IDENTITY_MATRIX;
        isFlipH = false;
        isFlipV = false;
        mRotation = 0;
    }

    /**
     * Process int.
     *
     * @param textureId   the texture id
     * @param textureType the texture type
     * @return the int
     */
    public int process(int textureId, int textureType) {
        if (mWidth <= 0 && mHeight <= 0) {
            throw new RuntimeException("setSize firstly!");
        }

        if (mTextureId == -1) {
            mTextureId = createTexture(mWidth, mHeight);
            bindFramebuffer(mTextureId);
            isTextureInner = true;
        } else if (isTextureInner && isSizeChanged) {
            GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
            mTextureId = createTexture(mWidth, mHeight);
            bindFramebuffer(mTextureId);
        } else if (isTextureChanged) {
            bindFramebuffer(mTextureId);
        }
        isTextureChanged = false;
        isSizeChanged = false;

        if (drawer == null) {
            drawer = new GlRectDrawer();
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferId);
        GLUtils.checkGlError("glBindFramebuffer");

        Matrix transform = RendererCommon.convertMatrixToAndroidGraphicsMatrix(mTexMatrix);
        transform.preTranslate(0.5f, 0.5f);
        transform.preRotate(mRotation, 0.f, 0.f);
        transform.preScale(
                isFlipH ? -1 * 1.f : 1.f,
                isFlipV ? -1 * 1.f : 1.f
        );
        transform.preTranslate(-1 * 0.5f, -1 * 0.5f);
        float[] matrix = RendererCommon.convertMatrixFromAndroidGraphicsMatrix(transform);

        if (textureType == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            drawer.drawOes(textureId, 0, matrix, mWidth, mHeight, 0, 0, mWidth, mHeight);
        } else {
            drawer.drawRgb(textureId, 0, matrix, mWidth, mHeight, 0, 0, mWidth, mHeight);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glFinish();

        return mTextureId;
    }

    /**
     * Release.
     */
    public void release() {
        deleteTexture();
        deleteFramebuffer();

        if (drawer != null) {
            drawer.release();
            drawer = null;
        }
    }


    /**
     * Delete framebuffer.
     */
    private void deleteFramebuffer() {
        if (mFramebufferId != -1) {
            GLES20.glDeleteFramebuffers(1, new int[]{mFramebufferId}, 0);
            mFramebufferId = -1;
        }
    }

    /**
     * Create texture int.
     *
     * @param width  the width
     * @param height the height
     * @return the int
     */
    public int createTexture(int width, int height) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLUtils.checkGlError("glGenTextures");
        int textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE);

        return textureId;
    }

    /**
     * Resize texture.
     *
     * @param textureId the texture id
     * @param width     the width
     * @param height    the height
     */
    public void resizeTexture(int textureId, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE);
    }

    /**
     * Delete texture.
     */
    private void deleteTexture() {
        if (isTextureInner && mTextureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
        }
        isTextureInner = false;
        mTextureId = -1;
    }

    /**
     * Bind framebuffer.
     *
     * @param textureId the texture id
     */
    private void bindFramebuffer(int textureId) {
        if (mFramebufferId == -1) {
            int[] framebuffers = new int[1];
            GLES20.glGenFramebuffers(1, framebuffers, 0);
            GLUtils.checkGlError("glGenFramebuffers");
            mFramebufferId = framebuffers[0];
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                textureId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE);
    }

}
