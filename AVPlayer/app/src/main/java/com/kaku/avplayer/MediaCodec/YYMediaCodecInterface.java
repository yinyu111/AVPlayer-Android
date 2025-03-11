package com.kaku.avplayer.MediaCodec;

//
//  YYMediaCodecInterface
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/12.
//

import android.media.MediaFormat;
import android.opengl.EGLContext;

import com.kaku.avplayer.Base.YYFrame;
public interface YYMediaCodecInterface {
    public static final int YYMediaCodecInterfaceErrorCreate = -2000;
    public static final int YYMediaCodecInterfaceErrorConfigure = -2001;
    public static final int YYMediaCodecInterfaceErrorStart = -2003;
    public static final int YYMediaCodecInterfaceErrorDequeueOutputBuffer = -2003;
    public static final int YYMediaCodecInterfaceErrorParams = -2004;

    public static int YYMediaCodecProcessParams = -1;
    public static int YYMediaCodecProcessAgainLater = -2;
    public static int YYMediaCodecProcessSuccess = 0;

    ///< 初始化Codec,第一个参数需告知使用编码还是解码
    public void setup(boolean isEncoder, MediaFormat mediaFormat, YYMediaCodecListener listener, EGLContext eglContext);

    ///< 释放Codec
    public void release();

    ///< 获取输出格式描述
    public MediaFormat getOutputMediaFormat();

    ///< 获取输入格式描述
    public MediaFormat getInputMediaFormat();

    ///< 处理每一帧数据，编码前与编码后都可以，支持编解码2种模式
    public int processFrame(YYFrame frame);

    ///< 清空 Codec 缓冲区
    public void flush();
}
