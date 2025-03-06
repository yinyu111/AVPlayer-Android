package com.kaku.avplayer.Render;

//
//  YYAudioRenderListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/2.
//

import android.view.Surface;

import androidx.annotation.NonNull;
public interface YYRenderListener {

    void surfaceCreate(@NonNull Surface surface);// 渲染缓存创建
    void surfaceChanged(@NonNull Surface surface, int width, int height);// 渲染缓存变更分辨率
    void surfaceDestroy(@NonNull Surface surface);// 渲染缓存销毁
}
