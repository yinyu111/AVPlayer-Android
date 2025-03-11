package com.kaku.avplayer.MediaCodec;
//
//  YYVideoSurfaceEncoder
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/11.
//

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
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
import com.kaku.avplayer.Effect.YYSurfaceTexture;
import com.kaku.avplayer.Effect.YYSurfaceTextureListener;

public class YYVideoSurfaceDecoder implements YYMediaCodecInterface {
    private static final String TAG = "YYVideoSurfaceDecoder";
    private YYMediaCodecListener mListener = null;// 回调
    private MediaCodec mDecoder = null;// 解码器
    private ByteBuffer[] mInputBuffers;// 解码器输入缓存
    private MediaFormat mInputMediaFormat = null;// 输入格式描述
    private MediaFormat mOutMediaFormat = null;// 输出格式描述
    private YYGLContext mEGLContext = null;// OpenGL上下文
    private YYSurfaceTexture mSurfaceTexture = null;// 纹理缓存
    private Surface mSurface = null;// 纹理缓存 对应 Surface
    private YYGLFilter mOESConvert2DFilter;///< 特效

    private long mLastInputPts = 0;// 输入数据最后一帧时间戳
    private List<YYBufferFrame> mList = new ArrayList<>();
    private ReentrantLock mListLock = new ReentrantLock(true);

    private HandlerThread mDecoderThread = null;// 解码线程
    private Handler mDecoderHandler = null;
    private HandlerThread mRenderThread = null;// 渲染线程
    private Handler mRenderHandler = null;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());// 主线程

    public YYVideoSurfaceDecoder() {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setup(boolean isEncoder,MediaFormat mediaFormat,YYMediaCodecListener listener, EGLContext eglShareContext) {
        mInputMediaFormat = mediaFormat;
        mListener = listener;

        // 创建解码线程
        mDecoderThread = new HandlerThread("YYVideoSurfaceDecoderThread");
        mDecoderThread.start();
        mDecoderHandler = new Handler((mDecoderThread.getLooper()));

        // 创建渲染线程
        mRenderThread = new HandlerThread("YYVideoSurfaceRenderThread");
        mRenderThread.start();
        mRenderHandler = new Handler((mRenderThread.getLooper()));

        mDecoderHandler.post(()->{
            if(mInputMediaFormat == null){
                _callBackError(YYMediaCodecInterfaceErrorParams,"mInputMediaFormat null");
                return;
            }

            // 创建OpenGL 上下文、纹理缓存、纹理缓存Surface、OES转2D数据
            mEGLContext = new YYGLContext(eglShareContext);
            mEGLContext.bind();
            mSurfaceTexture = new YYSurfaceTexture(mSurfaceTextureListener);
            mSurfaceTexture.getSurfaceTexture().setDefaultBufferSize(mInputMediaFormat.getInteger(MediaFormat.KEY_WIDTH),mInputMediaFormat.getInteger(MediaFormat.KEY_HEIGHT));
            mSurface = new Surface(mSurfaceTexture.getSurfaceTexture());
            mOESConvert2DFilter = new YYGLFilter(false, YYGLBase.defaultVertexShader,YYGLBase.oesFragmentShader);
            mEGLContext.unbind();

            _setupDecoder();
        });
        }

        @Override
        public MediaFormat getOutputMediaFormat() {
            return mOutMediaFormat;
        }

        @Override
        public MediaFormat getInputMediaFormat() {
            return mInputMediaFormat;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void release() {
            mDecoderHandler.post(()-> {
                // 释放解码器、GL上下文、数据缓存、SurfaceTexture
                if(mDecoder != null){
                    try {
                        mDecoder.stop();
                        mDecoder.release();
                    } catch (Exception e) {
                        Log.e(TAG, "release: " + e.toString());
                    }
                    mDecoder = null;
                }

                mEGLContext.bind();
                if(mSurfaceTexture != null){
                    mSurfaceTexture.release();
                    mSurfaceTexture = null;
                }
                if(mSurface != null){
                    mSurface.release();
                    mSurface = null;
                }
                if(mOESConvert2DFilter != null){
                    mOESConvert2DFilter.release();
                    mOESConvert2DFilter = null;
                }
                mEGLContext.unbind();

                if(mEGLContext != null){
                    mEGLContext.release();
                    mEGLContext = null;
                }

                mListLock.lock();
                mList.clear();
                mListLock.unlock();

                mDecoderThread.quit();
                mRenderThread.quit();
            });
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void flush() {
            mDecoderHandler.post(()-> {
                // 刷新解码器缓冲区
                if(mDecoder == null){
                    return;
                }

                try {
                    mDecoder.flush();
                } catch (Exception e) {
                    Log.e(TAG, "flush" + e);
                }

                mListLock.lock();
                mList.clear();
                mListLock.unlock();
            });
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public int processFrame(YYFrame inputFrame) {
            if(inputFrame == null){
                return YYMediaCodecProcessParams;
            }

            YYBufferFrame frame = (YYBufferFrame)inputFrame;
            if(frame.buffer ==null || frame.bufferInfo == null || frame.bufferInfo.size == 0){
                return YYMediaCodecProcessParams;
            }

            // 外层数据进入缓存
            _appendFrame(frame);

            mDecoderHandler.post(()-> {
                if(mDecoder == null){
                    return;
                }

                // 缓存获取数据，尽量多的输入给解码器
                mListLock.lock();
                int mListSize = mList.size();
                mListLock.unlock();
                while (mListSize > 0){
                    mListLock.lock();
                    YYBufferFrame packet = mList.get(0);
                    mListLock.unlock();

                    int bufferIndex;
                    try {
                        // 获取解码器输入缓存下标
                        bufferIndex = mDecoder.dequeueInputBuffer(10 * 1000);
                    } catch (Exception e) {
                        Log.e(TAG, "dequeueInputBuffer" + e);
                        return;
                    }

                    if(bufferIndex >= 0){
                        // 填充数据
                        mInputBuffers[bufferIndex].clear();
                        mInputBuffers[bufferIndex].put(packet.buffer);
                        mInputBuffers[bufferIndex].flip();
                        try {
                            // 数据塞入解码器
                            mDecoder.queueInputBuffer(bufferIndex, 0, packet.bufferInfo.size, packet.bufferInfo.presentationTimeUs, packet.bufferInfo.flags);
                        } catch (Exception e) {
                            Log.e(TAG, "queueInputBuffer" + e);
                            return;
                        }

                        mLastInputPts = packet.bufferInfo.presentationTimeUs;
                        mListLock.lock();
                        mList.remove(0);
                        mListSize = mList.size();
                        mListLock.unlock();
                    }else{
                        break;
                    }
                }

                // 从解码器拉取尽量多的数据出来
                long outputDts = -1;
                MediaCodec.BufferInfo outputBufferInfo = new MediaCodec.BufferInfo();
                while (outputDts < mLastInputPts) {
                    int bufferIndex;
                    try {
                        // 获取解码器输出缓存下标
                        bufferIndex = mDecoder.dequeueOutputBuffer(outputBufferInfo, 10 * 1000);
                    } catch (Exception e) {
                        Log.e(TAG, "dequeueOutputBuffer" + e);
                        return;
                    }

                    if(bufferIndex >= 0){
                        // 释放缓存，第二个参数必须设置位true，这样数据刷新到指定surface
                        mDecoder.releaseOutputBuffer(bufferIndex,true);
                    }else{
                        if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            mOutMediaFormat = mDecoder.getOutputFormat();
                        }
                        break;
                    }
                }
            });

            return YYMediaCodecProcessSuccess;
        }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void _appendFrame(YYBufferFrame frame) {
        // 添加数据到缓存List
        YYBufferFrame packet = new YYBufferFrame();

        ByteBuffer newBuffer = ByteBuffer.allocateDirect(frame.bufferInfo.size);
        newBuffer.put(frame.buffer).position(0);
        MediaCodec.BufferInfo newInfo = new MediaCodec.BufferInfo();
        newInfo.size = frame.bufferInfo.size;
        newInfo.flags = frame.bufferInfo.flags;
        newInfo.presentationTimeUs = frame.bufferInfo.presentationTimeUs;
        packet.buffer = newBuffer;
        packet.bufferInfo = newInfo;

        mListLock.lock();
        mList.add(packet);
        mListLock.unlock();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean _setupDecoder() {
        // 初始化解码器
        try {
            // 根据输入格式描述创建解码器
            String mimetype = mInputMediaFormat.getString(MediaFormat.KEY_MIME);
            mDecoder = MediaCodec.createDecoderByType(mimetype);
        }catch (Exception e) {
            Log.e(TAG, "createDecoderByType" + e);
            _callBackError(YYMediaCodecInterfaceErrorCreate,e.getMessage());
            return false;
        }

        try {
            // 配置位Surface 解码模式
            mDecoder.configure(mInputMediaFormat, mSurface, null, 0);
        }catch (Exception e) {
            Log.e(TAG, "configure" + e);
            _callBackError(YYMediaCodecInterfaceErrorConfigure,e.getMessage());
            return false;
        }

        try {
            // 启动解码器
            mDecoder.start();
            // 获取解码器输入缓存
            mInputBuffers = mDecoder.getInputBuffers();
        }catch (Exception e) {
            Log.e(TAG, "start" +  e );
            _callBackError(YYMediaCodecInterfaceErrorStart,e.getMessage());
            return false;
        }

        return true;
    }

    private void _callBackError(int error, String errorMsg){
        // 错误回调
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.onError(error,TAG + errorMsg);
            });
        }
    }

    private YYSurfaceTextureListener mSurfaceTextureListener = new YYSurfaceTextureListener() {
        // SurfaceTexture 数据回调
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mRenderHandler.post(() -> {
                mEGLContext.bind();
                mSurfaceTexture.getSurfaceTexture().updateTexImage();
                if(mListener != null){
                    int width = mInputMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = mInputMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    int rotation = (mInputMediaFormat.getInteger(MediaFormat.KEY_ROTATION) + 360) % 360;
                    int rotationWidth = (rotation % 360 == 90 || rotation % 360 == 270) ? height : width;
                    int rotationHeight = (rotation % 360 == 90 || rotation % 360 == 270) ? width : height;
                    YYTextureFrame frame = new YYTextureFrame(mSurfaceTexture.getSurfaceTextureId(),new Size(rotationWidth,rotationHeight),mSurfaceTexture.getSurfaceTexture().getTimestamp() * 1000,true);
                    mSurfaceTexture.getSurfaceTexture().getTransformMatrix(frame.textureMatrix);
                    // OES 数据转换2D
                    YYFrame convertFrame = mOESConvert2DFilter.render(frame);
                    mListener.decodeDataOnAvailable(convertFrame);
                }
                mEGLContext.unbind();
            });
        }
    };

}
