package com.kaku.avplayer.Effect;

//
//  YYAudioCaptureListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/20.
//


import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

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

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
public class YYGLContext {
    private Surface mSurface = null;
    private EGLDisplay mEGLDisplay = EGL_NO_DISPLAY;// 实际显示设备的抽象
    private EGLContext mEGLContext = EGL_NO_CONTEXT;// 渲染上下文
    private EGLSurface mEGLSurface = EGL_NO_SURFACE;// 存储图像的内存区域
    private EGLContext mEGLShareContext = EGL_NO_CONTEXT;// 共享渲染上下文
    private EGLConfig mEGLConfig = null;//渲染表面的配置信息

    private EGLContext mLastContext = EGL_NO_CONTEXT;// 存储之前系统上下文
    private EGLDisplay mLastDisplay = EGL_NO_DISPLAY;// 存储之前系统设备
    private EGLSurface mLastSurface = EGL_NO_SURFACE;// 存储之前系统内存区域
    private boolean mIsBind = false;

    public YYGLContext(EGLContext shareContext) {
        mEGLShareContext = shareContext;
        // 创建GL上下文
        _eglSetup();
    }

    public YYGLContext(EGLContext shareContext,Surface surface) {
        mEGLShareContext = shareContext;
        mSurface = surface;
        // 创建GL上下文
        _eglSetup();
    }

    public void setSurface(Surface surface) {
        if (surface == null || surface == mSurface) {
            return;
        }

        // 释放渲染表面 Surface
        if (mEGLDisplay != EGL_NO_DISPLAY && mEGLSurface != EGL_NO_SURFACE) {
            eglDestroySurface(mEGLDisplay, mEGLSurface);
        }
        // 创建Surface
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        mSurface = surface;
        mEGLSurface = eglCreateWindowSurface(mEGLDisplay, mEGLConfig, mSurface,
                        surfaceAttribs, 0);
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }
    }

    public EGLContext getContext() {
        return mEGLContext;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public boolean swapBuffers() {
        // 将后台绘制的缓冲显示到前台
        if (mEGLDisplay != EGL_NO_DISPLAY && mEGLSurface != EGL_NO_SURFACE) {
            return eglSwapBuffers(mEGLDisplay, mEGLSurface);
        } else {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setPresentation(long nsecs) {
        // 设置时间戳
        if (mEGLDisplay != EGL_NO_DISPLAY && mEGLSurface != EGL_NO_SURFACE) {
            eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setPresentationTime(long nsecs) {
        // 设置时间戳
        if(mEGLDisplay != EGL_NO_DISPLAY && mEGLSurface != EGL_NO_SURFACE){
            eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
        }
    }

    public void bind() {
        // 绑定当前上下文
        mLastSurface = eglGetCurrentSurface(EGL14.EGL_READ);
        mLastContext = eglGetCurrentContext();
        mLastDisplay = eglGetCurrentDisplay();
        if (!eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
        mIsBind = true;
    }
    
    public void unbind() {
        if (!mIsBind) {
            return;
        }

        // 绑定回系统之前上下文
        if (mLastSurface != EGL_NO_SURFACE && mLastContext != EGL_NO_CONTEXT && mLastDisplay != EGL_NO_DISPLAY) {
            mLastDisplay = EGL_NO_DISPLAY;
            mLastSurface = EGL_NO_SURFACE;
            mLastContext = EGL_NO_CONTEXT;
        } else {
            if (mEGLDisplay != EGL_NO_DISPLAY) {
                eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            }
        }
        mIsBind = false;
    }

    public void release() {
        unbind();

        // 释放设备、Surface
        if (mEGLDisplay != EGL_NO_DISPLAY && mEGLSurface != EGL_NO_SURFACE) {
            eglDestroySurface(mEGLDisplay, mEGLSurface);
        }

        if (mEGLDisplay != EGL_NO_DISPLAY && mEGLContext != EGL_NO_CONTEXT) {
            eglDestroyContext(mEGLDisplay, mEGLContext);
        }

        if(mEGLDisplay != EGL_NO_DISPLAY){
            eglTerminate(mEGLDisplay);
        }

        mSurface    = null;
        mEGLShareContext = null;

        mEGLDisplay = null;
        mEGLContext = null;
        mEGLSurface = null;
    }

    private void _eglSetup() {
        // 创建设备
        mEGLDisplay = eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }

        int[] version = new int[2];
        // 根据版本初始化设备
        if (!eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        // 定义 EGLConfig 属性配置 定义红、绿、蓝、透明度、深度、模板缓冲的位数
        int[] attribList = {
                EGL14.EGL_BUFFER_SIZE, 32,
                EGL14.EGL_ALPHA_SIZE, 8,//颜色缓冲区中红色用几位来表示
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        // 找到符合要求的 EGLConfig 配置
        if (!eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888 + recordable ES2 EGL config");
        }
        mEGLConfig = configs[0];

        // 指定 OpenGL 使用版本
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        // 创建GL上下文
        mEGLContext = eglCreateContext(mEGLDisplay, mEGLConfig, mEGLShareContext != null ? mEGLShareContext : EGL_NO_CONTEXT, attrib_list, 0);
        if (mEGLContext == null) {
            throw new RuntimeException("null context");
        }

        // 创建Surface配置信息
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };

        // 创建Surface
        if (mSurface != null) {
            mEGLSurface = eglCreateWindowSurface(mEGLDisplay, mEGLConfig, mSurface, surfaceAttribs, 0);
        } else {
            mEGLSurface = eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);
        }

        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }
    }
}
