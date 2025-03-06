package com.kaku.avplayer.Capture;
//
//  YYAudioCaptureListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/20.
//

import android.content.Context;
import android.opengl.EGLContext;
public interface YYIVideoCapture {
    ///< 视频采集初始化
    public void setup(Context context, YYVideoCaptureConfig config, YYVideoCaptureListener listener, EGLContext eglShareContext);

    ///< 释放采集实例
    public void release();

    ///< 开始采集
    public void startRunning();

    ///< 关闭采集
    public void stopRunning();

    ///< 是否正在采集
    public boolean isRunning();

    ///< 获取 OpenGL 上下文
    public EGLContext getEGLContext();

    ///< 切换摄像头
    public void switchCamera();
}
