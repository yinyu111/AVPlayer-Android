package com.kaku.avplayer.Capture;
//
//  YYAudioCaptureListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/8.
//

import com.kaku.avplayer.Base.YYFrame;
public interface YYAudioCaptureListener {

    void onError(int error,String errorMsg);
    void onFrameAvailable(YYFrame frame);

}
