package com.kaku.avplayer.MediaCodec;
//
//  YYVideoSurfaceEncoder
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/7.
//

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.kaku.avplayer.Base.YYBufferFrame;
import com.kaku.avplayer.Base.YYFrame;
import com.kaku.avplayer.Base.YYGLBase;
import com.kaku.avplayer.Base.YYTextureFrame;
import com.kaku.avplayer.Effect.YYGLContext;
import com.kaku.avplayer.Effect.YYGLFilter;

import java.io.IOException;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
public class YYVideoSurfaceEncoder implements YYMediaCodecInterface {
    private static final String TAG = "YYVideoSurfaceEncoder";
    private YYMediaCodecListener mListener = null;// 回调
    private YYGLContext mEGLContext = null;//GL 上下文
    private YYGLFilter mFilter = null;// 渲染到Surface 特效
    private MediaCodec mEncoder = null;// 编码器
    private Surface mSurface = null;// 渲染Surface缓存

    private HandlerThread mEncoderThread = null;//编码线程
    private Handler mEncoderHandler = null;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());// 主线程
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private long mLastInputPts = 0;
    private MediaFormat mOutputFormat = null;// 输出格式描述
    private MediaFormat mInputFormat = null;// 输入格式描述

    public YYVideoSurfaceEncoder() {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setup(boolean isEncoder,MediaFormat mediaFormat, YYMediaCodecListener listener, EGLContext eglShareContext) {
        mInputFormat = mediaFormat;
        mListener = listener;

        mEncoderThread = new HandlerThread("YYSurfaceEncoderThread");
        mEncoderThread.start();
        mEncoderHandler = new Handler((mEncoderThread.getLooper()));

        mEncoderHandler.post(()->{
            if(mInputFormat == null){
                _callBackError(YYMediaCodecInterfaceErrorParams,"mInputFormat == null");
                return;
            }

            // 初始化编码器
            boolean setupSuccess = _setupEnocder();
            if(setupSuccess){
                mEGLContext = new YYGLContext(eglShareContext,mSurface);
                mEGLContext.bind();
                // 初始化特效 用于纹理渲染到编码器Surface上
                _setupFilter();
                mEGLContext.unbind();
            }
        });
    }

    @Override
    public MediaFormat getOutputMediaFormat() {
        return mOutputFormat;
    }

    @Override
    public MediaFormat getInputMediaFormat() {
        return mInputFormat;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void release() {
        mEncoderHandler.post(()->{
            // 释放编码器
            if(mEncoder != null){
                try {
                    mEncoder.stop();
                    mEncoder.release();
                } catch (Exception e) {
                    Log.e(TAG, "release: " + e.toString());
                }
                mEncoder = null;
            }

            // 释放GL特效 上下文
            if(mEGLContext != null){
                mEGLContext.bind();
                if(mFilter != null){
                    mFilter.release();
                    mFilter = null;
                }
                mEGLContext.unbind();

                mEGLContext.release();
                mEGLContext = null;
            }

            // 释放Surface缓存
            if(mSurface != null){
                mSurface.release();
                mSurface = null;
            }

            mEncoderThread.quit();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int processFrame(YYFrame inputFrame) {
        if(inputFrame == null || mEncoderHandler == null){
            return YYMediaCodecProcessParams;
        }
        YYTextureFrame frame = (YYTextureFrame)inputFrame;

        mEncoderHandler.post(()-> {
            if(mEncoder != null && mEGLContext != null){
                if(frame.isEnd){
                    // 最后一帧标记
                    mEncoder.signalEndOfInputStream();
                }else{
                    // 最近一帧时间戳
                    mLastInputPts = frame.usTime();
                    mEGLContext.bind();
                    // 渲染纹理到编码器Surface 设置视口
                    GLES20.glViewport(0, 0, frame.textureSize.getWidth(), frame.textureSize.getHeight());
                    mFilter.render(frame);
                    // 设置时间戳
                    mEGLContext.setPresentationTime(frame.usTime() * 1000);
                    mEGLContext.swapBuffers();
                    mEGLContext.unbind();

                    ///< 获取编码后的数据，尽量拿出最多的数据出来，回调给外层
                    long outputDts = -1;
                    while (outputDts < mLastInputPts){
                        int bufferIndex = 0;
                        try {
                            //输出缓冲区队列中获取已经处理好的数据缓冲区
                            bufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 10 * 1000);
                        } catch (Exception e) {
                            Log.e(TAG, "Unexpected MediaCodec exception in dequeueOutputBufferIndex, " + e);
                            _callBackError(YYMediaCodecInterfaceErrorDequeueOutputBuffer,e.getMessage());
                            return;
                        }

                        if(bufferIndex >= 0){
                            ByteBuffer byteBuffer = mEncoder.getOutputBuffer(bufferIndex);
                            if(byteBuffer != null){
                                outputDts = mBufferInfo.presentationTimeUs;
                                if(mListener != null){
                                    YYBufferFrame encodeFrame = new YYBufferFrame();
                                    encodeFrame.buffer = byteBuffer;
                                    encodeFrame.bufferInfo = mBufferInfo;
                                    mListener.encodeDataOnAvailable(encodeFrame);
                                }
                            }else{
                                break;
                            }

                            try {
                                mEncoder.releaseOutputBuffer(bufferIndex, false);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                                return;
                            }
                        }else{
                            if(bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                                mOutputFormat = mEncoder.getOutputFormat();
                            }
                            break;
                        }
                    }
                }
            }
        });

        return YYMediaCodecProcessSuccess;
    }

    @Override
    public void flush() {
        mEncoderHandler.post(()-> {
            // 刷新缓冲区
            if(mEncoder != null){
                try {
                    mEncoder.flush();
                } catch (Exception e) {
                    Log.e(TAG, "flush error!" + e);
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean _setupEnocder() {
        // 初始化编码器
        try {
            String mimeType = mInputFormat.getString(MediaFormat.KEY_MIME);
            mEncoder = MediaCodec.createEncoderByType(mimeType);
            mEncoder.configure(mInputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            Log.e(TAG, "createEncoderByType" + e);
            _callBackError(YYMediaCodecInterfaceErrorCreate,e.getMessage());
            return false;
        }

        // 创建Surface
        mSurface = mEncoder.createInputSurface();

        // 开启编码器
        try {
            mEncoder.start();
        }catch (Exception e) {
            Log.e(TAG, "start" +  e );
            _callBackError(YYMediaCodecInterfaceErrorStart,e.getMessage());
            return false;
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void _setupFilter() {
        // 创建渲染模块 渲染到编码器Surface
        if(mFilter == null){
            mFilter = new YYGLFilter(true, YYGLBase.defaultVertexShader,YYGLBase.defaultFragmentShader);
        }
    }

    private void _callBackError(int error, String errorMsg){
        // 出错回调
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.onError(error,TAG + errorMsg);
            });
        }
    }
}

