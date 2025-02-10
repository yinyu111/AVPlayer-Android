package com.kaku.avplayer.Capture;

//
//  YYAudioCapture
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/8.
//

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.kaku.avplayer.Base.YYBufferFrame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class YYAudioCapture {
    public static int YYAudioCaptureErrorCreate = -2600;
    public static int YYAudioCaptureErrorStart = -2601;
    public static int YYAudioCaptureErrorStop = -2602;

    private static final String TAG = "YYAudioCapture";
    private YYAudioCaptureConfig mConfig = null;///< 音频配置
    private YYAudioCaptureListener mListener = null;///< 音频回调
    private HandlerThread mRecordThread = null;///< 音频采集线程
    private Handler mRecordHandle = null;

    private HandlerThread mReadThread = null;///< 音频读数据线程
    private Handler mReadHandle = null;
    private int mMinBufferSize = 0;

    private AudioRecord mAudioRecord = null;///< 音频采集实例
    private boolean mRecording = false;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());///< 主线程用作错误回调

    public YYAudioCapture(YYAudioCaptureConfig config,YYAudioCaptureListener listener) {
        mConfig = config;
        mListener = listener;

        mRecordThread = new HandlerThread("YYAudioCaptureThread");
        mRecordThread.start();
        mRecordHandle = new Handler((mRecordThread.getLooper()));

        mReadThread = new HandlerThread("YYAudioCaptureReadThread");
        mReadThread.start();
        mReadHandle = new Handler((mReadThread.getLooper()));

        mRecordHandle.post(()->{
            _setupAudioRecord();///< 初始化音频采集实例
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startRunning() {
        ///< 开启音频采集
        mRecordHandle.post(()->{
            if(mAudioRecord != null && !mRecording){
                try {
                    mAudioRecord.startRecording();
                    mRecording = true;
                }catch (Exception e){
                    Log.e(TAG,e.getMessage());
                    _callBackError(YYAudioCaptureErrorStart,e.getMessage());
                }

                ///< 音频采集采用拉数据模式，通过读数据线程开启循环无限拉取PCM数据，拉到数据后进行回调
                mReadHandle.post(()->{
                    while (mRecording){
                        final byte[] pcmData = new byte[mMinBufferSize];
                        int readSize = mAudioRecord.read(pcmData, 0, mMinBufferSize);
                        if (readSize > 0) {
                            //处理音频数据 data
                            ByteBuffer buffer = ByteBuffer.allocateDirect(readSize).put(pcmData).order(ByteOrder.nativeOrder());
                            buffer.position(0);
                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            bufferInfo.presentationTimeUs = System.nanoTime() / 1000;
                            bufferInfo.size = readSize;
                            YYBufferFrame bufferFrame = new YYBufferFrame(buffer,bufferInfo);
                            if(mListener != null){
                                mListener.onFrameAvailable(bufferFrame);
                            }
                        }
                    }
                });
            }
        });
    }

    public void stopRunning() {
        ///< 关闭音频采集
        mRecordHandle.post(()->{
            if(mAudioRecord != null && mRecording){
                try {
                    mAudioRecord.stop();
                    mRecording = false;
                }catch (Exception e){
                    Log.e(TAG,e.getMessage());
                    _callBackError(YYAudioCaptureErrorStart,e.getMessage());
                }
            }
        });
    }

    public void release() {
        ///< 外层主动触发释放，释放采集实例、线程
        mRecordHandle.post(()->{
            if(mAudioRecord != null){
                if(mRecording){
                    try {
                        mAudioRecord.stop();
                        mRecording = false;
                    }catch (Exception e){
                        Log.e(TAG,e.getMessage());
                    }
                }

                try {
                    mAudioRecord.release();
                }catch (Exception e){
                    Log.e(TAG,e.getMessage());
                }
                mAudioRecord = null;
            }

            mRecordThread.quit();
            mReadThread.quit();
        });
    }

    private void _setupAudioRecord() {
        if(mAudioRecord == null){
            ///< 根据指定采样率、声道、位深获取每次回调数据大小
            mMinBufferSize = AudioRecord.getMinBufferSize(mConfig.sampleRate, mConfig.channel, AudioFormat.ENCODING_PCM_16BIT);
            try {
                ///< 根据采样率、声道、位深、每次回调数据大小生成采集实例
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,mConfig.sampleRate,mConfig.channel, AudioFormat.ENCODING_PCM_16BIT,mMinBufferSize);
            }catch (Exception e){
                Log.e(TAG,e.getMessage());
                _callBackError(YYAudioCaptureErrorCreate,e.getMessage());
            };
        }
    }

    private void _callBackError(int error, String errorMsg){
        ///< 错误回调
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.onError(error,TAG + errorMsg);
            });
        }
    }
}
