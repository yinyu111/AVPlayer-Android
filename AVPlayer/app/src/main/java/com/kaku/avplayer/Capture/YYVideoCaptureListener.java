package com.kaku.avplayer.Capture;

//
//  YYAudioCaptureListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/20.
//

import com.kaku.avplayer.Base.YYFrame;
public interface YYVideoCaptureListener {
    ///< 摄像机打开
    void cameraOnOpened();

    ///< 摄像机关闭
    void cameraOnClosed();

    ///< 摄像机出错
    void cameraOnError(int error,String errorMsg);

    ///< 数据回调给外层
    void onFrameAvailable(YYFrame frame);
}
