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
import android.media.MediaFormat;
import android.opengl.EGLContext;
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
import com.kaku.avplayer.Base.YYAVTools;
import com.kaku.avplayer.Capture.YYAudioCapture;
import com.kaku.avplayer.Capture.YYAudioCaptureConfig;
import com.kaku.avplayer.Capture.YYAudioCaptureListener;
import com.kaku.avplayer.MediaCodec.YYAudioByteBufferEncoder;
import com.kaku.avplayer.MediaCodec.YYByteBufferCodec;
import com.kaku.avplayer.MediaCodec.YYMediaCodecInterface;
import com.kaku.avplayer.MediaCodec.YYMediaCodecListener;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class MainActivity extends AppCompatActivity {
    private FileOutputStream mStream = null;
    private YYAudioCapture mAudioCapture = null;///< 音频采集模块
    private YYAudioCaptureConfig mAudioCaptureConfig = null;///< 音频采集配置
    private YYMediaCodecInterface mEncoder = null;///< 音频编码
    private MediaFormat mAudioEncoderFormat = null;///< 音频编码格式描述

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

        // 获取文件路径
        String filePath = getExternalFilesDir(null).getPath() + "/test.aac";
        // String filePath = Environment.getExternalStorageDirectory().getPath() + "/test.pcm";
        // 输出文件路径到 Logcat
        Log.wtf("FilePath", "PCM filePath: " + filePath);

        FrameLayout.LayoutParams startParams = new FrameLayout.LayoutParams(200, 120);
        startParams.gravity = Gravity.CENTER_HORIZONTAL;
        Button startButton = new Button(this);
        startButton.setTextColor(Color.BLUE);
        startButton.setText("开始");
        startButton.setVisibility(View.VISIBLE);
        ///> 设置了一个点击事件监听器
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mEncoder == null){
                    mAudioCaptureConfig = new YYAudioCaptureConfig();
                    mAudioCapture = new YYAudioCapture(mAudioCaptureConfig, mAudioCaptureListener);
                    mAudioCapture.startRunning();

                    if(mStream == null){
                        try {
                            mStream = new FileOutputStream(filePath);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                    mEncoder = new YYAudioByteBufferEncoder();
                    MediaFormat mediaFormat = YYAVTools.createAudioFormat(mAudioCaptureConfig.sampleRate,mAudioCaptureConfig.channel,96*1000);
                    mEncoder.setup(true,mediaFormat,mAudioEncoderListener,null);
                    ((Button)view).setText("停止");
                }else{
                    mAudioCapture.stopRunning();

                    mEncoder.release();
                    mEncoder = null;
                    ((Button)view).setText("开始");
                }
            }
        });
        ///> 按钮添加到当前的布局中
        addContentView(startButton, startParams);
    }

    ///< 音频采集回调
    private YYAudioCaptureListener mAudioCaptureListener = new YYAudioCaptureListener() {
        @Override
        public void onError(int error, String errorMsg) {
            Log.e("YYAudioCapture","errorCode" + error + "msg"+errorMsg);
        }

        @Override
        public void onFrameAvailable(YYFrame frame) {
            Log.e("YYAudioCapture", "kaku==onFrameAvailable");
            if (frame == null) {
                Log.e("YYAudioCapture", "frame is null");
            }

            try {
                mEncoder.processFrame(frame);
            } catch (Exception e) {
                Log.e("YYAudioCapture", "Error process audio data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    };

    ///< 音频采集回调
    private YYMediaCodecListener mAudioEncoderListener = new YYMediaCodecListener() {
        @Override
        public void onError(int error, String errorMsg) {
            Log.e("YYMediaCodec","errorCode" + error + "msg"+errorMsg);
        }

        @Override
        public void dataOnAvailable(YYFrame frame) {
            ///< 音频回调数据
            if(mAudioEncoderFormat == null && mEncoder != null){
                mAudioEncoderFormat = mEncoder.getOutputMediaFormat();
            }
            YYBufferFrame bufferFrame = (YYBufferFrame)frame;
            try {
                ByteBuffer adtsBuffer = YYAVTools.getADTS(bufferFrame.bufferInfo.size,
                        mAudioEncoderFormat.getInteger(MediaFormat.KEY_PROFILE),
                        mAudioEncoderFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        mAudioEncoderFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                byte[] adtsBytes = new byte[adtsBuffer.capacity()];
                adtsBuffer.get(adtsBytes);
                mStream.write(adtsBytes);

                byte[] dst = new byte[bufferFrame.bufferInfo.size];
                bufferFrame.buffer.get(dst);
                mStream.write(dst);
                mStream.flush(); // 确保数据被及时写入文件
            }  catch (IOException e) {
                Log.e("YYMediaCodec", "Error writing audio aac data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    };
}
