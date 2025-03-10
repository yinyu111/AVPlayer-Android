package com.kaku.avplayer.MediaCodec;

//
//  YYVideoEncoderConfig
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/7.
//

import android.media.MediaCodecInfo;
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;


//通过此配置生成编码格式描述 MediaFormat
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class YYVideoEncoderConfig {
    public Size size = new Size(720,1280);
    public int bitrate = 4 * 1024 * 1024;
    public int fps = 30;
    public int gop = 30 * 4;
    public boolean isHEVC = false;
    public int profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline;
    public int profileLevel = MediaCodecInfo.CodecProfileLevel.AVCLevel1;

    public YYVideoEncoderConfig() {

    }
}
