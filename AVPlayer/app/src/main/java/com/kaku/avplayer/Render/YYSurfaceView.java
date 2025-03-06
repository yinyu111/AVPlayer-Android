package com.kaku.avplayer.Render;

//
//  YYAudioRenderListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/2.
//

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class YYSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private YYRenderListener mListener = null;// 回调
    private SurfaceHolder mHolder = null;// surface的抽象接口

    public YYSurfaceView(Context context, YYRenderListener listener) {
        super(context);
        mListener = listener;
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        // Surface 创建
        mHolder = surfaceHolder;
        // 根据SurfaceHolder 创建 Surface
        if(mListener != null){
            mListener.surfaceCreate(surfaceHolder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
        // Surface 分辨率变更
        if(mListener != null){
            mListener.surfaceChanged(surfaceHolder.getSurface(),width,height);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        // Surface 销毁
        if (mListener != null) {
            mListener.surfaceDestroy(surfaceHolder.getSurface());
        }
    }
}
