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
import android.content.Context;
import android.widget.LinearLayout;


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
import com.kaku.avplayer.Demuxer.YYMP4Demuxer;
import com.kaku.avplayer.Demuxer.YYDemuxerConfig;
import com.kaku.avplayer.Demuxer.YYDemuxerListener;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
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

    private YYMP4Demuxer mDemuxer; ///< 解封装实例
    private YYDemuxerConfig mDemuxerConfig; ///< 解封装配置
    private YYMediaCodecInterface mDecoder; ///< 音频解码
    private FileOutputStream mStream = null;

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
        String outputCapture = getExternalFilesDir(null).getPath() + "/test.m4a";
        Log.e("outputCapture", "M4A outputCapture: " + outputCapture);
        String inputFilePath = getExternalFilesDir(null).getPath() + "/input.mp4";
        Log.e("inputFilePath", "MP4 inputFilePath: " + inputFilePath);
        File file = new File(inputFilePath);
        if (!file.exists()) {
            Log.e("kaku", "文件不存在");
            return;
        }
        String outputDemuxer = getExternalFilesDir(null).getPath() + "/output.aac";
        Log.e("outputDemuxer", "aac outputDemuxer: " + outputDemuxer);
        String outputDecoder = getExternalFilesDir(null).getPath() + "/output.pcm";
        Log.e("outputDecoder", "aac outputDecoder: " + outputDecoder);

        mMuxerConfig = new YYMuxerConfig(outputCapture);
        mMuxerConfig.muxerType = YYMediaBase.YYMediaType.YYMediaAudio;

        mDemuxerConfig = new YYDemuxerConfig();
        mDemuxerConfig.path = inputFilePath;
        mDemuxerConfig.demuxerType = YYMediaBase.YYMediaType.YYMediaAudio;
        if(mStream == null){
            try {
                mStream = new FileOutputStream(outputDecoder);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }



        // 创建一个垂直的 LinearLayout
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // 创建开始/停止按钮
        Button startButton = createButton("Capture", this::onStartStopButtonClick, Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        startButton.setLayoutParams(startParams);
        linearLayout.addView(startButton);

        // 创建新按钮
        Button demuxerButton = createButton("Demuxer", this::onDemuxerButtonClick, Gravity.CENTER_HORIZONTAL);
        demuxerButton.setLayoutParams(startParams);
        linearLayout.addView(demuxerButton);

        // 创建新按钮
        Button decoderButton = createButton("Decoder", this::onDecoderButtonClick, Gravity.CENTER_HORIZONTAL);
        decoderButton.setLayoutParams(startParams);
        linearLayout.addView(decoderButton);

        // 将 LinearLayout 添加到内容视图
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        addContentView(linearLayout, layoutParams);

    }

    // 修改 createButton 方法的返回类型为 Button
    private Button createButton(String text, View.OnClickListener listener, int gravity) {
        Context context = this;
        Button button = new Button(context);
        button.setTextColor(Color.BLUE);
        button.setText(text);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(listener);
        return button; // 返回创建好的 Button 对象
    }


    private void onStartStopButtonClick(View view) {
        // 处理开始/停止按钮的点击事件
        if (mEncoder == null) {
            mAudioCaptureConfig = new YYAudioCaptureConfig();
            mAudioCapture = new YYAudioCapture(mAudioCaptureConfig, mAudioCaptureListener);
            mAudioCapture.startRunning();

            mEncoder = new YYAudioByteBufferEncoder();
            MediaFormat mediaFormat = YYAVTools.createAudioFormat(mAudioCaptureConfig.sampleRate, mAudioCaptureConfig.channel, 96 * 1000);
            mEncoder.setup(true, mediaFormat, mAudioEncoderListener, null);

            mMuxer = new YYMP4Muxer(mMuxerConfig, mMuxerListener);
            ((Button) view).setText("stop");
        } else {
            // 等待编码完成
            mAudioCapture.stopRunning();

            mEncoder.flush();
            mEncoder.release();
            mEncoder = null;

            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;

            ((Button) view).setText("Capture");
        }
    }

    private void onDemuxerButtonClick(View view) {
        ///< 创建解封装实例
        if(mDemuxer == null) {
            mDemuxer = new YYMP4Demuxer(mDemuxerConfig, mDemuxerListener);

            ///< 读取音频数据
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
            while (nextBuffer != null) {
                try {
                    ///< 添加 ADTS
                    ByteBuffer adtsBuffer = YYAVTools.getADTS(bufferInfo.size, mDemuxer.audioProfile(), mDemuxer.samplerate(), mDemuxer.channel());
                    byte[] adtsBytes = new byte[adtsBuffer.capacity()];
                    adtsBuffer.get(adtsBytes);
                    mStream.write(adtsBytes);

                    byte[] dst = new byte[bufferInfo.size];
                    nextBuffer.get(dst);
                    mStream.write(dst);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
            }
            Log.i("YYDemuxer", "complete");
        }
    }

    private void onDecoderButtonClick(View view) {
        ///< 创建解封装器与解码器。
        if (mDemuxer == null) {
            mDemuxer = new YYMP4Demuxer(mDemuxerConfig,mDemuxerListener);
            mDecoder = new YYByteBufferCodec();
            mDecoder.setup(false,mDemuxer.audioMediaFormat(),mAudioEncoderListener,null);

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
            ///< 循环读取音频帧进入解码器。
            while (nextBuffer != null) {
                mDecoder.processFrame(new YYBufferFrame(nextBuffer,bufferInfo));
                nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
            }
            mDecoder.flush();
            Log.i("YYDemuxer","complete");
        }
    }





    ///====================================Listern====================================

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
        public void encodeDataOnAvailable(YYFrame frame) {
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

        @Override
        public void decodeDataOnAvailable(YYFrame frame) {
            Log.i("YYMediaCodec","decodeDataOnAvailable");
            YYBufferFrame bufferFrame = (YYBufferFrame)frame;
            try {
                byte[] dst = new byte[bufferFrame.bufferInfo.size];
                bufferFrame.buffer.get(dst);
                mStream.write(dst);
            }  catch (IOException e) {
                e.printStackTrace();
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

    private YYDemuxerListener mDemuxerListener = new YYDemuxerListener() {
        ///< 解封装错误回调
        @Override
        public void demuxerOnError(int error, String errorMsg) {
            Log.i("KFDemuxer","error" + error + "msg" + errorMsg);
        }
    };
}
