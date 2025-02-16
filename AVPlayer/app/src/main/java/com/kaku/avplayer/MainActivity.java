package com.kaku.avplayer;
//
//  MainActivity
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
import com.kaku.avplayer.Base.YYMediaBase;
import com.kaku.avplayer.Capture.YYAudioCapture;
import com.kaku.avplayer.Capture.YYAudioCaptureConfig;
import com.kaku.avplayer.Capture.YYAudioCaptureListener;
import com.kaku.avplayer.MediaCodec.YYAudioByteBufferEncoder;
import com.kaku.avplayer.MediaCodec.YYByteBufferCodec;
import com.kaku.avplayer.MediaCodec.YYMediaCodecInterface;
import com.kaku.avplayer.MediaCodec.YYMediaCodecListener;
import com.kaku.avplayer.Muxer.YYMP4Muxer;
import com.kaku.avplayer.Muxer.YYMuxerConfig;
import com.kaku.avplayer.Muxer.YYMuxerListener;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class MainActivity extends AppCompatActivity {
    private YYAudioCapture mAudioCapture = null;///< 音频采集模块
    private YYAudioCaptureConfig mAudioCaptureConfig = null;///< 音频采集配置
    private YYMediaCodecInterface mEncoder = null;///< 音频编码
    private MediaFormat mAudioEncoderFormat = null;///< 音频编码格式描述
    private YYMP4Muxer mMuxer;///< 封装起器
    private YYMuxerConfig mMuxerConfig; ///< 封装器配置

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
        String filePath = getExternalFilesDir(null).getPath() + "/test.m4a";
        // String filePath = Environment.getExternalStorageDirectory().getPath() + "/test.pcm";
        // 输出文件路径到 Logcat
        Log.wtf("FilePath", "M4A filePath: " + filePath);

        mMuxerConfig = new YYMuxerConfig(filePath);
        mMuxerConfig.muxerType = YYMediaBase.YYMediaType.YYMediaAudio;

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

                    mEncoder = new YYAudioByteBufferEncoder();
                    MediaFormat mediaFormat = YYAVTools.createAudioFormat(mAudioCaptureConfig.sampleRate,mAudioCaptureConfig.channel,96*1000);
                    mEncoder.setup(true,mediaFormat,mAudioEncoderListener,null);

                    mMuxer = new YYMP4Muxer(mMuxerConfig,mMuxerListener);
                    ((Button)view).setText("停止");
                }else{
                    // 等待编码完成
                    mAudioCapture.stopRunning();

                    mEncoder.flush();
                    mEncoder.release();
                    mEncoder = null;

                    mMuxer.stop();
                    mMuxer.release();
                    mMuxer = null;

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
                if (mEncoder != null) {
                    mEncoder.processFrame(frame);
                }
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
            ///< 编码回调写入封装器
            if(mAudioEncoderFormat == null && mEncoder != null){
                mAudioEncoderFormat = mEncoder.getOutputMediaFormat();
                mMuxer.setAudioMediaFormat(mEncoder.getOutputMediaFormat());
                mMuxer.start();
            }

            if(mMuxer != null){
                mMuxer.writeSampleData(false,((YYBufferFrame)frame).buffer,((YYBufferFrame)frame).bufferInfo);
            }
        }
    };

    private YYMuxerListener mMuxerListener = new YYMuxerListener() {
        @Override
        public void muxerOnError(int error, String errorMsg) {
            ///< 音频封装错误回调
            Log.i("YYMuxerListener","error" + error + "msg" + errorMsg);
        }
    };
}
