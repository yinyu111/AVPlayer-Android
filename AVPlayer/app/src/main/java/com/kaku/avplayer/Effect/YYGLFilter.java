package com.kaku.avplayer.Effect;

//
//  YYAudioRenderListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/3.
//

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.kaku.avplayer.Base.YYFrame;
import com.kaku.avplayer.Base.YYGLBase;
import com.kaku.avplayer.Base.YYTextureFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.EGL14.EGL_NO_CONTEXT;
import static android.opengl.EGL14.EGL_NO_DISPLAY;
import static android.opengl.EGL14.EGL_NO_SURFACE;
import static android.opengl.EGL14.eglChooseConfig;
import static android.opengl.EGL14.eglCreateContext;
import static android.opengl.EGL14.eglCreatePbufferSurface;
import static android.opengl.EGL14.eglCreateWindowSurface;
import static android.opengl.EGL14.eglDestroyContext;
import static android.opengl.EGL14.eglDestroySurface;
import static android.opengl.EGL14.eglGetCurrentContext;
import static android.opengl.EGL14.eglGetCurrentDisplay;
import static android.opengl.EGL14.eglGetCurrentSurface;
import static android.opengl.EGL14.eglGetDisplay;
import static android.opengl.EGL14.eglInitialize;
import static android.opengl.EGL14.eglMakeCurrent;
import static android.opengl.EGL14.eglSwapBuffers;
import static android.opengl.EGL14.eglTerminate;
import static android.opengl.EGLExt.eglPresentationTimeANDROID;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
public class YYGLFilter {
    private boolean mIsCustomFBO = false;// 是否自定义帧缓存 部分渲染到指定Surface等其它场景会自定义
    private YYGLFrameBuffer mFrameBuffer = null;// 帧缓存
    private YYGLProgram mProgram = null;// 着色器容器
    private YYGLTextureAttributes mGLTextureAttributes = null;// 纹理格式描述

    private int mTextureUniform = -1;// 纹理下标
    private int mPostionMatrixUniform = -1;// 顶点矩阵下标
    private int mTextureMatrixUniform = -1;// 纹理矩阵下标
    private int mPositionAttribute = -1; // 顶点下标
    private int mTextureCoordinateAttribute = -1;// 纹理下标
    private FloatBuffer mSquareVerticesBuffer = null;// 顶点buffer
    private FloatBuffer mTextureCoordinatesBuffer = null;// 纹理buffer
    private FloatBuffer mCustomSquareVerticesBuffer = null;// 自定义顶点buffer
    private FloatBuffer mCustomTextureCoordinatesBuffer = null;// 自定义纹理buffer

    public YYGLFilter(boolean isCustomFBO,String vertexShader,String fragmentShader) {
        mIsCustomFBO = isCustomFBO;
        // 初始化着色器
        _setupProgram(vertexShader,fragmentShader);
    }

    public YYGLFilter(boolean isCustomFBO,String vertexShader,String fragmentShader, YYGLTextureAttributes textureAttributes) {
        mIsCustomFBO = isCustomFBO;
        mGLTextureAttributes = textureAttributes;
        // 初始化着色器
        _setupProgram(vertexShader,fragmentShader);
    }

    public YYGLFrameBuffer getOutputFrameBuffer() {
        return mFrameBuffer;
    }

    public YYFrame render(YYTextureFrame frame){
        if(frame == null){
            return frame;
        }

        YYTextureFrame resultFrame = new YYTextureFrame(frame);
        // 初始化帧缓存
        _setupFrameBuffer(frame.textureSize);

        // 绑定帧缓存
        if(mFrameBuffer != null){
            mFrameBuffer.bind();
        }

        if(mProgram != null){
            // 使用着色器
            mProgram.use();

            // 设置帧缓存背景色
            glClearColor(0,0,0,1);
            // 清空帧缓存颜色
            glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // 激活纹理单元1
            glActiveTexture(GLES20.GL_TEXTURE1);
            // 根据是否OES纹理绑定纹理id
            if (frame.isOESTexture) {
                glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, frame.textureId);
            } else {
                glBindTexture(GLES20.GL_TEXTURE_2D, frame.textureId);
            }
            // 传递纹理单元1
            glUniform1i(mTextureUniform, 1);

            // 设置纹理矩阵
            if(mTextureMatrixUniform >= 0){
                glUniformMatrix4fv(mTextureMatrixUniform, 1, false, frame.textureMatrix, 0);
            }

            // 设置顶点矩阵
            if(mPostionMatrixUniform >= 0){
                glUniformMatrix4fv(mPostionMatrixUniform, 1, false, frame.positionMatrix, 0);
            }

            // 启用顶点着色器顶点坐标属性
            glEnableVertexAttribArray(mPositionAttribute);
            // 启用顶点着色器纹理坐标属性
            glEnableVertexAttribArray(mTextureCoordinateAttribute);

            // 根据自定义顶点缓存设置不同顶点坐标
            if(mCustomSquareVerticesBuffer != null){
                mCustomSquareVerticesBuffer.position(0);
                glVertexAttribPointer(mPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, mCustomSquareVerticesBuffer);
            }else{
                mSquareVerticesBuffer.position(0);
                glVertexAttribPointer(mPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, mSquareVerticesBuffer);
            }

            // 根据自定义纹理缓存设置不同纹理坐标
            if(mCustomTextureCoordinatesBuffer != null){
                mCustomTextureCoordinatesBuffer.position(0);
                glVertexAttribPointer(mTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mCustomTextureCoordinatesBuffer);
            }else{
                mTextureCoordinatesBuffer.position(0);
                glVertexAttribPointer(mTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordinatesBuffer);
            }

            // 真正的渲染
            glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            // 解除绑定纹理
            if (frame.isOESTexture) {
                glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            } else {
                glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }

            // 关闭顶点着色器顶点属性
            glDisableVertexAttribArray(mPositionAttribute);
            // 关闭顶点着色器纹理属性
            glDisableVertexAttribArray(mTextureCoordinateAttribute);
        }

        // 解绑帧缓存
        if(mFrameBuffer != null){
            mFrameBuffer.unbind();
        }

        // 返回渲染后数据
        if(mFrameBuffer != null){
            resultFrame.textureId = mFrameBuffer.getTextureId();
            resultFrame.textureSize = mFrameBuffer.getSize();
            resultFrame.isOESTexture = false;
            resultFrame.textureMatrix = YYGLBase.YYIdentityMatrix();
            resultFrame.positionMatrix = YYGLBase.YYIdentityMatrix();
        }
        return resultFrame;
    }

    public void release() {
        // 释放帧缓存、着色器
        if(mFrameBuffer != null){
            mFrameBuffer.release();
            mFrameBuffer = null;
        }

        if(mProgram != null){
            mProgram.release();
            mProgram = null;
        }
    }

    public void setSquareVerticesBuffer(FloatBuffer squareVerticesBuffer) {
        mSquareVerticesBuffer = squareVerticesBuffer;
    }

    public void setTextureCoordinatesBuffer(FloatBuffer textureCoordinatesBuffer) {
        mCustomTextureCoordinatesBuffer = textureCoordinatesBuffer;
    }

    public void setIntegerUniformValue(String uniformName, int intValue){
        // 设置 int 类型 uniform数据
        if(mProgram != null){
            int uniforamIndex = mProgram.getUniformLocation(uniformName);
            mProgram.use();
            glUniform1i(uniforamIndex, intValue);
        }
    }

    public void setFloatUniformValue(String uniformName, float floatValue){
        // 设置 float 类型 uniform数据
        if(mProgram != null){
            int uniforamIndex = mProgram.getUniformLocation(uniformName);
            mProgram.use();
            glUniform1f(uniforamIndex, floatValue);
        }
    }

    private void _setupFrameBuffer(Size size) {
        if(mIsCustomFBO) {
            return;
        }

        // 初始化帧缓存与对应纹理
        if(mFrameBuffer == null || mFrameBuffer.getSize().getWidth() != size.getWidth() || mFrameBuffer.getSize().getHeight() != size.getHeight()){
            if(mFrameBuffer != null){
                mFrameBuffer.release();
                mFrameBuffer = null;
            }

            mFrameBuffer = new YYGLFrameBuffer(size,mGLTextureAttributes);
        }
    }

    private void _setupProgram(String vertexShader,String fragmentShader){
        // 根据vs fs 初始化着色器容器
        if(mProgram == null){
            mProgram = new YYGLProgram(vertexShader,fragmentShader);
            mTextureUniform = mProgram.getUniformLocation("inputImageTexture");
            mPostionMatrixUniform = mProgram.getUniformLocation("mvpMatrix");
            mTextureMatrixUniform = mProgram.getUniformLocation("textureMatrix");
            mPositionAttribute = mProgram.getAttribLocation("position");
            mTextureCoordinateAttribute = mProgram.getAttribLocation("inputTextureCoordinate");

            final float squareVertices[] = {
                    -1.0f, -1.0f,
                    1.0f, -1.0f,
                    -1.0f,  1.0f,
                    1.0f,  1.0f,
            };

            ByteBuffer squareVerticesByteBuffer = ByteBuffer.allocateDirect(4 * squareVertices.length);
            squareVerticesByteBuffer.order(ByteOrder.nativeOrder());
            mSquareVerticesBuffer = squareVerticesByteBuffer.asFloatBuffer();
            mSquareVerticesBuffer.put(squareVertices);
            mSquareVerticesBuffer.position(0);

            final float textureCoordinates[] = {
                    0.0f, 0.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
            };
            ByteBuffer textureCoordinatesByteBuffer = ByteBuffer.allocateDirect(4 * textureCoordinates.length);
            textureCoordinatesByteBuffer.order(ByteOrder.nativeOrder());
            mTextureCoordinatesBuffer = textureCoordinatesByteBuffer.asFloatBuffer();
            mTextureCoordinatesBuffer.put(textureCoordinates);
            mTextureCoordinatesBuffer.position(0);
        }
    }
}
