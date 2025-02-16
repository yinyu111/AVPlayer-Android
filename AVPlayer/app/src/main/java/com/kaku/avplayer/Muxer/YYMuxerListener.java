package com.kaku.avplayer.Muxer;

//
//  YYMuxerListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/14.
//
public interface YYMuxerListener {
    ///< 错误回调
    void muxerOnError(int error, String errorMsg);
}
