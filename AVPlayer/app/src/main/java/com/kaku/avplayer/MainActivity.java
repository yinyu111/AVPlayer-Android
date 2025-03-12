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
import android.media.MediaCodecInfo;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.ReentrantLock;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

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
import com.kaku.avplayer.MediaCodec.YYVideoSurfaceDecoder;
import com.kaku.avplayer.MediaCodec.YYVideoSurfaceEncoder;
import com.kaku.avplayer.MediaCodec.YYVideoEncoderConfig;
import com.kaku.avplayer.Muxer.YYMP4Muxer;
import com.kaku.avplayer.Muxer.YYMuxerConfig;
import com.kaku.avplayer.Muxer.YYMuxerListener;
import com.kaku.avplayer.Demuxer.YYMP4Demuxer;
import com.kaku.avplayer.Demuxer.YYDemuxerConfig;
import com.kaku.avplayer.Demuxer.YYDemuxerListener;
import com.kaku.avplayer.Render.YYAudioRender;
import com.kaku.avplayer.Render.YYAudioRenderListener;

import com.kaku.avplayer.Base.YYGLBase;
import com.kaku.avplayer.Base.YYTextureFrame;
import com.kaku.avplayer.Capture.YYIVideoCapture;
import com.kaku.avplayer.Capture.YYVideoCaptureConfig;
import com.kaku.avplayer.Capture.YYVideoCaptureListener;
import com.kaku.avplayer.Capture.YYVideoCaptureV1;
import com.kaku.avplayer.Capture.YYVideoCaptureV2;
import com.kaku.avplayer.Effect.YYGLContext;
import com.kaku.avplayer.Effect.YYGLFilter;
import com.kaku.avplayer.Render.YYRenderView;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String INPUT_FILE_PATH = "input.mp4";
    private static final String OUTPUT_CAPTURE = "test.m4a";
    private static final String OUTPUT_DEMUXER = "output.aac";
    private static final String OUTPUT_DECODER = "output.pcm";
    private static final String OUTPUT_SURFACE_ENCODER = "videoEncodec.h264";
    private static final String OUTPUT_SURFACE_MUXER = "videoMuxer.mp4";
    private static final String OUTPUT_SURFACE_DEMUXER = "videoDemuxer.h264";
    private static final String OUTPUT_SURFACE_COMPILE = "videoCompile.mp4";

    private YYAudioCapture mAudioCapture = null;///< 音频采集模块
    private YYAudioCaptureConfig mAudioCaptureConfig = null;///< 音频采集配置
    private YYMediaCodecInterface mAudioEncoder = null;///< 音频编码
    private MediaFormat mAudioEncoderFormat = null;///< 音频编码格式描述
    private YYMP4Muxer mMuxer;///< 封装起器
    private YYMuxerConfig mMuxerConfig; ///< 封装器配置

    private YYMP4Demuxer mDemuxer; ///< 解封装实例
    private YYDemuxerConfig mDemuxerConfig; ///< 解封装配置
    private YYMediaCodecInterface mAudioDecoder; ///< 音频解码
    private FileOutputStream mStream = null;

    private YYAudioRender mAudioRender;///< 音频渲染实例
    private byte[] mPCMCache = new byte[10*1024*1024];///< PCM数据缓存
    private int mPCMCacheSize = 0;
    private ReentrantLock mLock = new ReentrantLock(true);

    private YYIVideoCapture mVideoCapture;///< 相机采集
    private YYVideoCaptureConfig mVideoCaptureConfig;///< 相机采集配置
    private YYRenderView mRenderView;///< 渲染视图
    private YYGLContext mEGLContext;///< OpenGL 上下文


    private YYVideoEncoderConfig mEncoderConfig;// 编码配置
    private YYMediaCodecInterface mVideoEncoder;// 编码
    private YYMediaCodecInterface mVideoDecoder = null;

    // 创建一个 FrameLayout 作为根布局
    FrameLayout rootLayout;
    LinearLayout linearLayout;
    

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootLayout = new FrameLayout(this);

        // 请求权限
        requestPermissionsIfNeeded();

        // 初始化文件路径
        initFilePaths();

        // 初始化布局
        initLayout();

        // 将 FrameLayout 设置为内容视图
        setContentView(rootLayout);
    }

    private void requestPermissionsIfNeeded() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, permissions[2]) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, permissions[3]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void initFilePaths() {
        String externalFilesDir = getExternalFilesDir(null).getPath();
        String inputFilePath = externalFilesDir + "/" + INPUT_FILE_PATH;
        Log.e("inputFilePath", "MP4 inputFilePath: " + inputFilePath);
        String outputCapture = externalFilesDir + "/" + OUTPUT_CAPTURE;
        Log.e("outputCapture", "M4A outputCapture: " + outputCapture);
        String outputDemuxer = externalFilesDir + "/" + OUTPUT_DEMUXER;
        Log.e("outputDemuxer", "aac outputDemuxer: " + outputDemuxer);
        String outputDecoder = externalFilesDir + "/" + OUTPUT_DECODER;
        Log.e("outputDecoder", "aac outputDecoder: " + outputDecoder);
        String outputVideoEncoder = externalFilesDir + "/" + OUTPUT_SURFACE_ENCODER;
        Log.e("outputVideoEncoder", "h264 outputVideoEncoder: " + outputVideoEncoder);
        String outputVideoMuxer = externalFilesDir + "/" + OUTPUT_SURFACE_MUXER;
        Log.e("outputVideoMuxer", "Muxer outputVideoMuxer: " + outputVideoMuxer);
        String outputVideoDemuxer = externalFilesDir + "/" + OUTPUT_SURFACE_DEMUXER;
        Log.e("outputVideoDemuxer", "h264 outputVideoDemuxer: " + outputVideoDemuxer);
        String outputVideoCompile = externalFilesDir + "/" + OUTPUT_SURFACE_COMPILE;
        Log.e("outputVideoCompile", "h264 outputVideoCompile: " + outputVideoCompile);


        mMuxerConfig = new YYMuxerConfig(outputVideoCompile);
        mMuxerConfig.muxerType = YYMediaBase.YYMediaType.YYMediaAV;

        mDemuxerConfig = new YYDemuxerConfig();
        mDemuxerConfig.path = inputFilePath;
        mDemuxerConfig.demuxerType = YYMediaBase.YYMediaType.YYMediaAV;
        if (mStream == null) {
            try {
                mStream = new FileOutputStream(outputVideoDemuxer);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void initLayout() {
        // 创建一个垂直的 LinearLayout
        linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // 创建开始/停止按钮
        Button startButton = createButton("AudioCapture", this::onStartStopButtonClick, Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        startButton.setLayoutParams(startParams);
        linearLayout.addView(startButton);

        // 创建新按钮
        Button demuxerButton = createButton("AudioDemuxer", this::onDemuxerButtonClick, Gravity.CENTER_HORIZONTAL);
        demuxerButton.setLayoutParams(startParams);
        linearLayout.addView(demuxerButton);

        // 创建新按钮
        Button decoderButton = createButton("AudioDecoder", this::onDecoderButtonClick, Gravity.CENTER_HORIZONTAL);
        decoderButton.setLayoutParams(startParams);
        linearLayout.addView(decoderButton);

        // 创建新按钮
        Button audioRenderButton = createButton("AudioRender", this::onAudioRenderButtonClick, Gravity.CENTER_HORIZONTAL);
        audioRenderButton.setLayoutParams(startParams);
        linearLayout.addView(audioRenderButton);

        // 创建新按钮
        Button videoRenderButton = createButton("VideoRender", this::onVideoRenderButtonClick, Gravity.CENTER_HORIZONTAL);
        videoRenderButton.setLayoutParams(startParams);
        linearLayout.addView(videoRenderButton);

        // 创建新按钮
        Button videoSurfaceEncoderButton = createButton("VideoSurfaceEncoder", this::onVideoSurfaceEncoderButtonClick, Gravity.CENTER_HORIZONTAL);
        videoSurfaceEncoderButton.setLayoutParams(startParams);
        linearLayout.addView(videoSurfaceEncoderButton);

        // 创建新按钮
        Button videoSurfaceMuxerButton = createButton("VideoSurfaceMuxer", this::onVideoSurfaceMuxerButtonClick, Gravity.CENTER_HORIZONTAL);
        videoSurfaceMuxerButton.setLayoutParams(startParams);
        linearLayout.addView(videoSurfaceMuxerButton);

        // 创建新按钮
        Button videoDemuxerButton = createButton("VideoDemuxer", this::onVideoDemuxerButtonClick, Gravity.CENTER_HORIZONTAL);
        videoDemuxerButton.setLayoutParams(startParams);
        linearLayout.addView(videoDemuxerButton);

        // 创建新按钮
        Button videoCompileButton = createButton("VideoCompile", this::onVideoCompileButtonClick, Gravity.CENTER_HORIZONTAL);
        videoCompileButton.setLayoutParams(startParams);
        linearLayout.addView(videoCompileButton);

        // 将 LinearLayout 添加到 FrameLayout 中，这样按钮会显示在 mRenderView 之上
        rootLayout.addView(linearLayout);
    }

    private Button createButton(String text, View.OnClickListener listener, int gravity) {
        Context context = this;
        Button button = new Button(context);
        button.setTextColor(Color.BLUE);
        button.setText(text);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(listener);
        return button;
    }


    private void onStartStopButtonClick(View view) {
        if (mAudioEncoder == null) {
            initAudioCapture();
            initAudioEncoder();
            initMuxer();
            ((Button) view).setText("stop");
        } else {
            releaseAudioCapture();
            releaseAudioEncoder();
            releaseMuxer();
            ((Button) view).setText("Capture");
        }
    }

    private void initAudioCapture() {
        mAudioCaptureConfig = new YYAudioCaptureConfig();
        mAudioCapture = new YYAudioCapture(mAudioCaptureConfig, mAudioCaptureListener);
        mAudioCapture.startRunning();
    }

    private void initAudioEncoder() {
        mAudioEncoder = new YYAudioByteBufferEncoder();
        MediaFormat mediaFormat = YYAVTools.createAudioFormat(mAudioCaptureConfig.sampleRate, mAudioCaptureConfig.channel, 96 * 1000);
        mAudioEncoder.setup(true, mediaFormat, mAudioEncoderListener, null);
    }

    private void initMuxer() {
        mMuxer = new YYMP4Muxer(mMuxerConfig, mMuxerListener);
    }

    private void releaseAudioCapture() {
        if (mAudioCapture != null) {
            mAudioCapture.stopRunning();
            mAudioCapture = null;
        }
    }

    private void releaseAudioEncoder() {
        if (mAudioEncoder != null) {
            mAudioEncoder.flush();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
    }

    private void releaseMuxer() {
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    private void onDemuxerButtonClick(View view) {
        if (mDemuxer == null) {
            initDemuxer();
            processDemuxer();
        }
    }

    private void initDemuxer() {
        mDemuxer = new YYMP4Demuxer(mDemuxerConfig, mDemuxerListener);
    }

    private void processDemuxer() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
        while (nextBuffer != null) {
            try {
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

    private void onDecoderButtonClick(View view) {
        if (mDemuxer == null) {
            initDemuxer();
            initDecoder();
            processDecoder();
        }
    }

    private void initDecoder() {
        mAudioDecoder = new YYByteBufferCodec();
        mAudioDecoder.setup(false, mDemuxer.audioMediaFormat(), mAudioEncoderListener, null);
    }

    private void processDecoder() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
        while (nextBuffer != null) {
            mAudioDecoder.processFrame(new YYBufferFrame(nextBuffer, bufferInfo));
            nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
        }
        mAudioDecoder.flush();
        Log.i("YYDemuxer", "complete");
    }


    private void onAudioRenderButtonClick(View view) {
        initDemuxer();
        initDecoder();
        processDecoder();
        initAudioRender();
    }

    private void initAudioRender() {
        mAudioRender = new YYAudioRender(mRenderListener, mDemuxer.samplerate(), mDemuxer.channel());
        mAudioRender.play();
    }

    private void onVideoRenderButtonClick(View view) {
        // 创建 mRenderView
        initVideoRender();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initVideoRender() {
        mEGLContext = new YYGLContext(null);
        mRenderView = new YYRenderView(this, mEGLContext.getContext());
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Rect outRect = new Rect();
        windowManager.getDefaultDisplay().getRectSize(outRect);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(outRect.width(), outRect.height());
        // 将 mRenderView 添加到 FrameLayout 中
        rootLayout.addView(mRenderView, params);
        // 将 LinearLayout 添加到 FrameLayout 中，这样按钮会显示在 mRenderView 之上
        rootLayout.removeView(linearLayout);
        rootLayout.addView(linearLayout);

        mVideoCaptureConfig = new YYVideoCaptureConfig();
        mVideoCaptureConfig.cameraFacing = LENS_FACING_FRONT;
        mVideoCaptureConfig.resolution = new Size(720, 1280);
        mVideoCaptureConfig.fps = 30;
        boolean useCamera2 = true;
        if (useCamera2) {
            mVideoCapture = new YYVideoCaptureV2();
        } else {
            mVideoCapture = new YYVideoCaptureV1();
        }
        mVideoCapture.setup(this, mVideoCaptureConfig, mVideoCaptureListener, mEGLContext.getContext());
        mVideoCapture.startRunning();

        mEncoderConfig = new YYVideoEncoderConfig();
    }

    private void onVideoSurfaceEncoderButtonClick(View view) {
        if (mEGLContext == null) {
            initVideoRender();
        }

        // 创建编码器
        if(mVideoEncoder == null){
            mVideoEncoder = new YYVideoSurfaceEncoder();
            MediaFormat mediaFormat = YYAVTools.createVideoFormat(mEncoderConfig.isHEVC,mEncoderConfig.size, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,mEncoderConfig.bitrate,mEncoderConfig.fps,mEncoderConfig.gop / mEncoderConfig.fps,mEncoderConfig.profile,mEncoderConfig.profileLevel);
            mVideoEncoder.setup(true,mediaFormat,mVideoEncoderListener,mEGLContext.getContext());
            ((Button)view).setText("stop");
        }else{
            mVideoEncoder.release();
            mVideoEncoder = null;
            ((Button)view).setText("VideoSurfaceEncoder");
        }
    }

    private void onVideoSurfaceMuxerButtonClick(View view) {
        if (mEGLContext == null) {
            initVideoRender();
        }

        // 创建编码器
        if(mVideoEncoder == null){
            mVideoEncoder = new YYVideoSurfaceEncoder();
            MediaFormat mediaFormat = YYAVTools.createVideoFormat(mEncoderConfig.isHEVC,mEncoderConfig.size, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,mEncoderConfig.bitrate,mEncoderConfig.fps,mEncoderConfig.gop / mEncoderConfig.fps,mEncoderConfig.profile,mEncoderConfig.profileLevel);
            mVideoEncoder.setup(true,mediaFormat,mVideoEncoderListener,mEGLContext.getContext());
            if (mMuxer == null) {
                initMuxer();
                mMuxer.start();
            }
            ((Button)view).setText("stop");
        }else{
            mVideoEncoder.release();
            mVideoEncoder = null;
            if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
            }
            ((Button)view).setText("VideoSurfaceMuxer");
        }

    }

    private void onVideoDemuxerButtonClick(View view) {
        // 解封装创建
        if(mDemuxer == null){
            mDemuxer = new YYMP4Demuxer(mDemuxerConfig,mDemuxerListener);

            // 根据HEVC 分别获取vps sps pps等信息
            if(mDemuxer.isHEVC()){
                try {
                    ByteBuffer extradata = mDemuxer.videoMediaFormat().getByteBuffer("csd-0");
                    byte[] extradataBytes = new byte[extradata.capacity()];
                    extradata.get(extradataBytes);
                    mStream.write(extradataBytes);
                }  catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    ByteBuffer sps = mDemuxer.videoMediaFormat().getByteBuffer("csd-0");
                    byte[] spsBytes = new byte[sps.capacity()];
                    sps.get(spsBytes);
                    mStream.write(spsBytes);
                }  catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    ByteBuffer pps = mDemuxer.videoMediaFormat().getByteBuffer("csd-1");
                    byte[] ppsBytes = new byte[pps.capacity()];
                    pps.get(ppsBytes);
                    mStream.write(ppsBytes);
                }  catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 循环读取视频数据
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer nextBuffer = mDemuxer.readVideoSampleData(bufferInfo);
            while (nextBuffer != null){
                try {
                    byte[] dst = new byte[bufferInfo.size];
                    nextBuffer.get(dst);
                    mStream.write(dst);
                }  catch (IOException e) {
                    e.printStackTrace();
                }
                nextBuffer = mDemuxer.readVideoSampleData(bufferInfo);
            }
            Log.i("YYDemuxer","complete");
        }
    }

    private void onVideoCompileButtonClick(View view) {
        ///< 创建解封装与封装。
        if (mDemuxer == null) {
            mDemuxer = new YYMP4Demuxer(mDemuxerConfig, mDemuxerListener);
            mMuxer = new YYMP4Muxer(mMuxerConfig, mMuxerListener);
            mMuxer.start();
            ///< 设置格式描述。
            mMuxer.setVideoMediaFormat(mDemuxer.videoMediaFormat());
            mMuxer.setAudioMediaFormat(mDemuxer.audioMediaFormat());

            ///< 循环读取音视频数据写入封装器。
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer videoNextBuffer = mDemuxer.readVideoSampleData(videoBufferInfo);

            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer audioNextBuffer = mDemuxer.readAudioSampleData(audioBufferInfo);
            while (audioNextBuffer != null || videoNextBuffer != null) {
                if (audioNextBuffer != null) {
                    mMuxer.writeSampleData(false, audioNextBuffer, audioBufferInfo);
                    audioNextBuffer = mDemuxer.readAudioSampleData(audioBufferInfo);
                }

                if (videoNextBuffer != null) {
                    mMuxer.writeSampleData(true, videoNextBuffer, videoBufferInfo);
                    videoNextBuffer = mDemuxer.readVideoSampleData(videoBufferInfo);
                }
            }
            mMuxer.stop();
            Log.i("YYCompile", "complete");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseAudioCapture();
        releaseAudioEncoder();
        releaseMuxer();
        releaseDemuxer();
        releaseAudioDecoder();
        releaseAudioRender();
        releaseVideoCapture();
        try {
            if (mStream != null) {
                mStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseDemuxer() {
        if (mDemuxer != null) {
            mDemuxer.release();
            mDemuxer = null;
        }
    }

    private void releaseAudioDecoder() {
        if (mAudioDecoder != null) {
            mAudioDecoder.release();
            mAudioDecoder = null;
        }
    }

    private void releaseAudioRender() {
        if (mAudioRender != null) {
            mAudioRender.stop();
            mAudioRender.release();
            mAudioRender = null;
        }
    }

    private void releaseVideoCapture() {
        if (mVideoCapture != null) {
            mVideoCapture.stopRunning();
            mVideoCapture = null;
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
                if (mAudioEncoder != null) {
                    mAudioEncoder.processFrame(frame);
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
            if(mAudioEncoderFormat == null && mAudioEncoder != null){
                mAudioEncoderFormat = mAudioEncoder.getOutputMediaFormat();
                mMuxer.setAudioMediaFormat(mAudioEncoder.getOutputMediaFormat());
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
            if(bufferFrame.buffer != null && bufferFrame.bufferInfo.size > 0){
                byte[] bytes = new byte[bufferFrame.bufferInfo.size];
                bufferFrame.buffer.get(bytes);
                mLock.lock();
                System.arraycopy(bytes,0,mPCMCache,mPCMCacheSize,bytes.length);
                mPCMCacheSize += bytes.length;
                mLock.unlock();
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
            Log.i("YYDemuxer","error" + error + "msg" + errorMsg);
        }
    };

    private YYAudioRenderListener mRenderListener = new YYAudioRenderListener() {
        @Override
        ///< 音频渲染出错
        public void onError(int error, String errorMsg) {

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        ///< 音频播放模块获取音频 PCM 数据
        public byte[] audioPCMData(int size) {
            if(mPCMCacheSize >= size){
                byte[] dst = new byte[size];
                mLock.lock();
                System.arraycopy(mPCMCache,0,dst,0,size);
                mPCMCacheSize -= size;
                System.arraycopy(mPCMCache,size,mPCMCache,0,mPCMCacheSize);
                mLock.unlock();
                return dst;
            }
            return null;
        }
    };

    private YYVideoCaptureListener mVideoCaptureListener = new YYVideoCaptureListener() {
        @Override
        ///< 相机打开回调
        public void cameraOnOpened(){}

        @Override
        ///< 相机关闭回调
        public void cameraOnClosed() {
        }

        @Override
        ///< 相机出错回调
        public void cameraOnError(int error,String errorMsg) {

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        ///< 相机数据回调
        public void onFrameAvailable(YYFrame frame) {
            //mRenderView.render((YYTextureFrame) frame);
            ///< 采集数据回调，进入编码器。
            mRenderView.render((YYTextureFrame) frame);
            if (mVideoEncoder != null) {
                mVideoEncoder.processFrame(frame);
            }
        }
    };

    private YYMediaCodecListener mVideoEncoderListener = new YYMediaCodecListener() {
        @Override
        public void onError(int error, String errorMsg) {

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void encodeDataOnAvailable(YYFrame frame) {
            if(frame == null){
                return;
            }

            YYBufferFrame bufferFrame = (YYBufferFrame)frame;
            if(bufferFrame.buffer == null){
                return;
            }

//            try {
//                byte[] dst = new byte[(int) (bufferFrame.bufferInfo.size)];
//                bufferFrame.buffer.get(dst);
//                mStream.write(dst);
//            }  catch (IOException e) {
//                e.printStackTrace();
//            }
            // 编码回调数据进入封装器
            if(mMuxer != null) {
                if ((((YYBufferFrame)frame).bufferInfo.flags & BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mMuxer.setVideoMediaFormat(mVideoEncoder.getOutputMediaFormat());
                }else{
                    mMuxer.writeSampleData(true,((YYBufferFrame)frame).buffer,((YYBufferFrame)frame).bufferInfo);
                    Log.i("Muxer","encodeDataOnAvailable");
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void decodeDataOnAvailable(YYFrame frame) {
//            if(frame == null){
//                return;
//            }
//
//            if(mVideoDecoder != null){
//                mVideoDecoder.processFrame(frame);
//            }
            if(frame == null){
                return;
            }

            YYBufferFrame bufferFrame = (YYBufferFrame)frame;
            if(bufferFrame.buffer == null){
                return;
            }

            try {
                byte[] dst = new byte[(int) (bufferFrame.bufferInfo.size)];
                bufferFrame.buffer.get(dst);
                mStream.write(dst);
            }  catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
