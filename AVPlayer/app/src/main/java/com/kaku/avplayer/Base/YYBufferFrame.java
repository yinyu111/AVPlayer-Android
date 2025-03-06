package com.kaku.avplayer.Base;

//
//  YYBufferFrame
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/8.
//

import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

import static com.kaku.avplayer.Base.YYFrame.YYFrameType.YYFrameBuffer;

//extends !!!
public class YYBufferFrame extends YYFrame {
    public ByteBuffer buffer;
    public MediaCodec.BufferInfo bufferInfo;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public YYBufferFrame() {
        super(YYFrameBuffer);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public YYBufferFrame(ByteBuffer inputBuffer, MediaCodec.BufferInfo inputBufferInfo) {
        super(YYFrameBuffer);
        buffer = inputBuffer;
        bufferInfo = inputBufferInfo;
    }

    public YYFrameType frameType() {
        return YYFrameBuffer;
    }
}
