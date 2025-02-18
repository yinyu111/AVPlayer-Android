package com.kaku.avplayer.Demuxer;

//
//  YYAudioCapture
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/16.
//

public interface YYDemuxerListener {
    ///< 错误回调
    void demuxerOnError(int error,String errorMsg);
}
