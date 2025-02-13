package com.kaku.avplayer.MediaCodec;

//
//  YYAudioByteBufferEncoder
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/12.
//

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.kaku.avplayer.Base.YYBufferFrame;
import com.kaku.avplayer.Base.YYFrame;

import java.nio.ByteBuffer;
import java.util.Queue;
public class YYAudioByteBufferEncoder extends YYByteBufferCodec {
    private int mChannel = 0;///< 音频声道数
    private int mSampleRate = 0;///< 音频采样率
    private long mCurrentTimestamp = -1;///< 标记当前时间戳 (因为数据重新分割 所以时间戳需要手动计算)
    private byte[]  mByteArray = new byte[500 * 1024];///< 输入音频数据数组
    private int mByteArraySize = 0;///< 输入音频数据Size

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int processFrame(YYFrame inputFrame) {
        ///< 获取音频声道数与采样率
        if(mChannel == 0){
            MediaFormat inputMediaFormat = getInputMediaFormat();
            if(inputMediaFormat != null){
                mChannel = inputMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                mSampleRate = inputMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            }
        }

        if(mChannel == 0  || mSampleRate == 0 || inputFrame == null){
            return YYMediaCodecProcessParams;
        }

        YYBufferFrame bufferFrame = (YYBufferFrame)inputFrame;
        if(bufferFrame.bufferInfo == null || bufferFrame.bufferInfo.size == 0){
            return YYMediaCodecProcessParams;
        }

        ///< 控制音频输入给编码器单次字节数2048字节
        int sendSize = 2048;
        ///< 外层输入如果为2048则直接跳过执行
        if(mByteArraySize == 0 && sendSize == bufferFrame.bufferInfo.size){
            return super.processFrame(inputFrame);
        }else{
            long currentTimestamp = 0;
            if(mCurrentTimestamp == -1){
                mCurrentTimestamp = bufferFrame.bufferInfo.presentationTimeUs;
            }

            ///< 将缓存中数据执行送入编码器操作
            int sendCacheStatus = sendBufferEncoder(sendSize);
            if(sendCacheStatus < 0){
                return sendCacheStatus;
            }

            ///< 将新输入数据送入缓冲区重复执行此操作
            byte[] inputBytes = new byte[bufferFrame.bufferInfo.size];
            // 从输入帧的缓冲区中获取数据到字节数组中
            bufferFrame.buffer.get(inputBytes);

            System.arraycopy(inputBytes,0,mByteArray,mByteArraySize,bufferFrame.bufferInfo.size);
            mByteArraySize += bufferFrame.bufferInfo.size;

            return sendBufferEncoder(sendSize);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void release() {
        mCurrentTimestamp = -1;
        mByteArraySize = 0;
        super.release();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void flush() {
        mCurrentTimestamp = -1;
        mByteArraySize = 0;
        super.flush();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int sendBufferEncoder(int sendSize) {
        ///< 将当前Buffer中数据按每次2048送给编码器
        while (mByteArraySize >= sendSize){
            MediaCodec.BufferInfo newBufferInfo = new MediaCodec.BufferInfo();
            newBufferInfo.size = sendSize;
            newBufferInfo.presentationTimeUs = mCurrentTimestamp;

            ByteBuffer newBuffer = ByteBuffer.allocateDirect(sendSize);
            newBuffer.put(mByteArray,0,sendSize).position(0);

            YYBufferFrame newFrame = new YYBufferFrame();
            newFrame.buffer = newBuffer;
            newFrame.bufferInfo = newBufferInfo;
            int status = super.processFrame(newFrame);
            if(status < 0){
                return status;
            }else{
                mByteArraySize -= sendSize;
                if(mByteArraySize > 0){
                    // 如果缓存中还有剩余数据，将剩余数据向前移动到数组开头
                    System.arraycopy(mByteArray,sendSize,mByteArray,0,mByteArraySize);
                }
            }
            // 更新时间戳，根据采样率、声道数和数据大小计算时间戳的增量
            //每秒数据量（字节 / 秒） = 采样率（样本 / 秒）× 声道数 × 位深度（位 / 样本）÷ 8（位 / 字节）
            mCurrentTimestamp += sendSize * 1000000 / (16 / 8 * mSampleRate * mChannel);
        }
        return YYMediaCodecProcessSuccess;
    }
}