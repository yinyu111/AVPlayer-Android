package com.kaku.avplayer.MediaCodec;

//
//  YYMediaCodecListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/12.
//

import android.media.MediaCodec;

import com.kaku.avplayer.Base.YYBufferFrame;
import com.kaku.avplayer.Base.YYFrame;

import java.nio.ByteBuffer;

public interface YYMediaCodecListener {

    void onError(int error, String errorMsg);

    void encodeDataOnAvailable(YYFrame frame);
    void decodeDataOnAvailable(YYFrame frame);
}
