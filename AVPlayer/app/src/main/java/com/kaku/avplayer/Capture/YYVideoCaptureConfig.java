package com.kaku.avplayer.Capture;

//
//  YYAudioCaptureListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/20.
//

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class YYVideoCaptureConfig {
    ///< 摄像头方向
    public Integer cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;

    ///< 分辨率
    public Size resolution = new Size(1080, 1920);

    ///< 帧率
    public Integer fps = 30;
}
