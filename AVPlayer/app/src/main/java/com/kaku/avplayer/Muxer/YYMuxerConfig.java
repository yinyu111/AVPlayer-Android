package com.kaku.avplayer.Muxer;

//
//  YYMuxerConfig
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/14.
//

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.kaku.avplayer.Base.YYMediaBase;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class YYMuxerConfig {
    ///< 输出路径
    public String outputPath = null;
    ///< 封装仅音频、仅视频、音视频
    public YYMediaBase.YYMediaType muxerType = YYMediaBase.YYMediaType.YYMediaAV;

    public YYMuxerConfig(String path){
        outputPath = path;
    }

}
