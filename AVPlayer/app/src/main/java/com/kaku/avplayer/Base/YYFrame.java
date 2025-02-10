package com.kaku.avplayer.Base;

//
//  YYFrame
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/8.
//

import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class YYFrame {
    public enum YYFrameType{
        YYFrameBuffer,
        YYFrameTexture;
    }

    public YYFrameType frameType = YYFrameType.YYFrameBuffer;
    public YYFrame(YYFrameType type){
        frameType = type;
    }
}
