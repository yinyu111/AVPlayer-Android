package com.kaku.avplayer.Render;

//
//  YYAudioRenderListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/19.
//

import java.nio.ByteBuffer;

public interface YYAudioRenderListener {
    ///< 出错回调
    void onError(int error,String errorMsg);
    ///< 获取PCM数据
    byte[] audioPCMData(int size);
}
