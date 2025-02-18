package com.kaku.avplayer.Demuxer;

//
//  YYAudioCapture
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/16.
//

import com.kaku.avplayer.Base.YYMediaBase;
public class YYDemuxerConfig {
    ///< 输入路径
    public String path;
    ///< 音视频解封装类型（仅音频 仅视频 音视频）
    public YYMediaBase.YYMediaType demuxerType = YYMediaBase.YYMediaType.YYMediaAV;
}
