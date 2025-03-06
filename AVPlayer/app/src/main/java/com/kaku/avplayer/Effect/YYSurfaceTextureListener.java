package com.kaku.avplayer.Effect;

//
//  YYAudioRenderListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/4.
//

import android.graphics.SurfaceTexture;
public interface YYSurfaceTextureListener {
    void onFrameAvailable(SurfaceTexture surfaceTexture);
}
