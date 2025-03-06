package com.kaku.avplayer.Effect;

//
//  YYAudioRenderListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/4.
//

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
public class YYSurfaceTexture  implements SurfaceTexture.OnFrameAvailableListener {
    private int mSurfaceTextureId = -1;// oes 纹理id
    private SurfaceTexture mSurfaceTexture = null;// 渲染TextureId的Surface
    private YYSurfaceTextureListener mListener = null;//回调

    public YYSurfaceTexture(YYSurfaceTextureListener listener) {
        mListener = listener;
        _setupSurfaceTexture();
    }

    public int getSurfaceTextureId() {
        return mSurfaceTextureId;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void release() {
        // 释放纹理id
        if(mSurfaceTextureId != -1){
            GLES20.glDeleteTextures(1,  new int[] {mSurfaceTextureId},0);
            mSurfaceTextureId = -1;
        }
    }

    private void _setupSurfaceTexture() {
        // 初始化OpenGL OES 纹理id
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        if (textures[0] == 0) {
            // 处理纹理生成失败的情况
            Log.e("OpenGL", "SurfaceTexture: Failed to generate texture ID");
            return;
        }

        mSurfaceTextureId = textures[0];
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        //将当前对象设置为帧可用的监听器。
        mSurfaceTexture.setOnFrameAvailableListener(this);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            // 处理 OpenGL 错误
            Log.e("OpenGL", "SurfaceTexture: OpenGL error: " + error);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // 回调数据
        if(mListener != null){
            mListener.onFrameAvailable(surfaceTexture);
        }
    }
}
