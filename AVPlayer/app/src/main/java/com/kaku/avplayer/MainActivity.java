package com.kaku.avplayer;
//
//  YYAudioCapture
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/8.
//

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.kaku.avplayer.Base.YYBufferFrame;
import com.kaku.avplayer.Base.YYFrame;
import com.kaku.avplayer.Capture.YYAudioCapture;
import com.kaku.avplayer.Capture.YYAudioCaptureConfig;
import com.kaku.avplayer.Capture.YYAudioCaptureListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class MainActivity extends AppCompatActivity {
    private FileOutputStream mStream = null;
    private YYAudioCapture mAudioCapture = null;
    private YYAudioCaptureConfig mAudioCaptureConfig = null;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< 音频录制权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this,
                    new String[] {Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        try {
            mAudioCaptureConfig = new YYAudioCaptureConfig();
            mAudioCapture = new YYAudioCapture(mAudioCaptureConfig, mAudioCaptureListener);
            mAudioCapture.startRunning();
        } catch (Exception e) {
            Log.e("YYAudioCapture", "Error starting audio capture: " + e.getMessage());
            e.printStackTrace();
        }

        // 获取文件路径
        String filePath = getExternalFilesDir(null).getPath() + "/test.pcm";
//        String filePath = Environment.getExternalStorageDirectory().getPath() + "/test.pcm";
        // 输出文件路径到 Logcat
        Log.wtf("FilePath", "PCM filePath: " + filePath);

        if(mStream == null){
            try {
                mStream = new FileOutputStream(filePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mStream != null) {
            try {
                mStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    ///< 音频采集回调
    private YYAudioCaptureListener mAudioCaptureListener = new YYAudioCaptureListener() {
        @Override
        public void onError(int error, String errorMsg) {
            Log.e("YYAudioCapture","errorCode" + error + "msg"+errorMsg);
        }

        @Override
        public void onFrameAvailable(YYFrame frame) {
            ///< 获取到音频Buffer数据存储到本地PCM
            try {
                ByteBuffer pcmData = ((YYBufferFrame)frame).buffer;
                byte[] ppsBytes = new byte[pcmData.capacity()];
                pcmData.get(ppsBytes);
                mStream.write(ppsBytes);
                mStream.flush(); // 确保数据被及时写入文件
            }  catch (IOException e) {
                Log.e("YYAudioCapture", "Error writing audio data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    };
}
