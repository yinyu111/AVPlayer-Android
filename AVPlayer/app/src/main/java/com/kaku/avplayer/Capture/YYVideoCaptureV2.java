package com.kaku.avplayer.Capture;
//
//  YYAudioCaptureListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/20.
//

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.kaku.avplayer.Base.YYFrame;
import com.kaku.avplayer.Base.YYGLBase;
import com.kaku.avplayer.Base.YYTextureFrame;
import com.kaku.avplayer.Effect.YYGLContext;
import com.kaku.avplayer.Effect.YYGLFilter;
import com.kaku.avplayer.Effect.YYSurfaceTexture;
import com.kaku.avplayer.Effect.YYSurfaceTextureListener;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class YYVideoCaptureV2 implements YYIVideoCapture {
    public static final int YYVideoCaptureV2CameraDisableError = -3000;
    private static final String TAG = "YYVideoCaptureV2";
    private YYVideoCaptureListener mListener = null;///< 回调
    private YYVideoCaptureConfig mConfig = null;///< 采集配置
    private WeakReference<Context> mContext = null;

    private CameraManager mCameraManager = null;///< 相机系统服务，用于管理和连接相机设备
    private String mCameraId;///<摄像头id
    private CameraDevice mCameraDevice = null;///< 相机设备类
    private HandlerThread mCameraThread = null;///< 采集线程
    private Handler mCameraHandler = null;
    private CaptureRequest.Builder mCaptureRequestBuilder = null;///< CaptureRequest的构造器，使用Builder模式，设置更加方便
    private CaptureRequest mCaptureRequest = null;///< 相机捕获图像的设置请求，包含传感器，镜头，闪光灯等
    private CameraCaptureSession mCameraCaptureSession = null;///< 请求抓取相机图像帧的会话，会话的建立主要会建立起一个通道,源端是相机，另一端是 Target
    private boolean mCameraIsRunning = false;
    private Range<Integer>[] mFpsRange;

    private YYGLContext mGLContext = null;
    private YYSurfaceTexture mSurfaceTexture = null;
    private YYGLFilter mOESConvert2DFilter;///< 特效
    private Surface mSurface = null;
    private HandlerThread mRenderThread = null;
    private Handler mRenderHandler = null;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    public YYVideoCaptureV2() {

    }

    @Override
    public void setup(Context context, YYVideoCaptureConfig config, YYVideoCaptureListener listener, EGLContext eglShareContext) {
        mListener = listener;
        mConfig = config;
        mContext = new WeakReference<Context>(context);

        ///< 相机采集线程
        mCameraThread = new HandlerThread("YYCameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler((mCameraThread.getLooper()));

        ///< 渲染线程
        mRenderThread = new HandlerThread("YYCameraRenderThread");
        mRenderThread.start();
        mRenderHandler = new Handler((mRenderThread.getLooper()));

        mGLContext = new YYGLContext(eglShareContext);
    }

    @Override
    public EGLContext getEGLContext() {
        return mGLContext.getContext();
    }

    @Override
    public boolean isRunning() {
        return mCameraIsRunning;
    }

    @Override
    public void startRunning() {
        ///< 开启预览
        mCameraHandler.post(() -> {
            _startRunning();
        });
    }

    @Override
    public void stopRunning() {
        ///< 停止预览
        mCameraHandler.post(() -> {
            _stopRunning();
        });
    }

    @Override
    public void release() {
        mCameraHandler.post(() -> {
            ///< 关闭采集、释放SurfaceTexture、OpenGL上下文、线程等
            _stopRunning();
            mGLContext.bind();
            if(mSurfaceTexture != null){
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }

            if(mOESConvert2DFilter != null){
                mOESConvert2DFilter.release();
                mOESConvert2DFilter = null;
            }

            mGLContext.unbind();
            mGLContext.release();
            mGLContext = null;

            if(mSurface != null){
                mSurface.release();
                mSurface = null;
            }

            mCameraThread.quit();
            mRenderThread.quit();
        });
    }

    @Override
    public void switchCamera() {
        ///< 切换摄像头
        mCameraHandler.post(() -> {
            _stopRunning();
            mConfig.cameraFacing = mConfig.cameraFacing == CameraCharacteristics.LENS_FACING_FRONT ? CameraCharacteristics.LENS_FACING_BACK : CameraCharacteristics.LENS_FACING_FRONT;
            _startRunning();
        });
    }

    private void _startRunning() {
        ///< 获取相机系统服务
        if(mCameraManager == null){
            mCameraManager = (CameraManager) mContext.get().getSystemService(Context.CAMERA_SERVICE);
        }
        ///< 根据外层摄像头方向查找摄像头id
        boolean selectSuccess = _chooseCamera();
        if (selectSuccess) {
            try {
                ///< 检测采集权限
                if (ActivityCompat.checkSelfPermission(mContext.get(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) mContext.get(), new String[] {Manifest.permission.CAMERA}, 1);
                }

                ///< 检测相机是否可用
                if(!_checkCameraService()){
                    _callBackError(YYVideoCaptureV2CameraDisableError,"相机不可用");
                    return;
                }

                ///< 打开相机设备
                mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void _stopRunning() {
        ///< 停止采集
        if(mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }

        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private YYSurfaceTextureListener mSurfaceTextureListener = new YYSurfaceTextureListener() {
        @Override
        //< SurfaceTexture 数据回调
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mRenderHandler.post(() -> {
                long timestamp = System.nanoTime();
                mGLContext.bind();
                ///< 刷新纹理数据至SurfaceTexture
                mSurfaceTexture.getSurfaceTexture().updateTexImage();
                if(mListener != null){
                    ///< 拼装好纹理数据返回给外层
                    YYTextureFrame frame = new YYTextureFrame(mSurfaceTexture.getSurfaceTextureId(),mConfig.resolution,timestamp,true);
                    mSurfaceTexture.getSurfaceTexture().getTransformMatrix(frame.textureMatrix);
                    YYFrame convertFrame = mOESConvert2DFilter.render(frame);
                    mListener.onFrameAvailable(convertFrame);
                }
                mGLContext.unbind();
            });
        }
    };

    private CameraCaptureSession.StateCallback mCaputreSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        ///< 创建会话回调
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            ///< 创建CaptureRequest
            mCaptureRequest = mCaptureRequestBuilder.build();
            mCameraCaptureSession = cameraCaptureSession;
            try {
                ///< 通过连续重复的Capture实现预览功能，每次Capture会把预览画面显示到对应的Surface上
                mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        ///< 创建会话出错回调
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            _callBackError(1005,"onConfigureFailed");
        }
    };

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        ///< 相机打开回调
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            try {
                ///< 通过相机设备创建构造器
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                Range<Integer> selectFpsRange = _chooseFpsRange();
                ///< 设置帧率
                if(selectFpsRange.getUpper() > 0){
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,selectFpsRange);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            if(mListener != null){
                mMainHandler.post(()->{
                    mListener.cameraOnOpened();
                });
            }
            mCameraIsRunning = true;

            if(mSurfaceTexture == null){
                mGLContext.bind();
                mSurfaceTexture = new YYSurfaceTexture(mSurfaceTextureListener);
                mOESConvert2DFilter = new YYGLFilter(false, YYGLBase.defaultVertexShader,YYGLBase.oesFragmentShader);
                mGLContext.unbind();
                mSurface = new Surface(mSurfaceTexture.getSurfaceTexture());
            }

            if(mSurface != null) {
                ///< 设置目标输出Surface
                mSurfaceTexture.getSurfaceTexture().setDefaultBufferSize(mConfig.resolution.getHeight(),mConfig.resolution.getWidth());
                mCaptureRequestBuilder.addTarget(mSurface);
                try {
                    ///< 创建通道会话
                    mCameraDevice.createCaptureSession(Arrays.asList(mSurface), mCaputreSessionCallback, mCameraHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            ///< 相机断开连接回调
            camera.close();
            mCameraDevice = null;
            mCameraIsRunning = false;
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            ///< 相机关闭回调
            camera.close();
            mCameraDevice = null;
            if(mListener != null){
                mMainHandler.post(()->{
                    mListener.cameraOnClosed();
                });
            }
            mCameraIsRunning = false;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            ///< 相机出错回调
            camera.close();
            mCameraDevice = null;
            _callBackError(error,"Camera onError");
            mCameraIsRunning = false;
        }
    };

    private boolean _chooseCamera() {
        try {
            ///< 根据外层配置方向选择合适的设备id 与 FPS 区间
            final String[] ids = mCameraManager.getCameraIdList();
            for(String cameraId : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing == mConfig.cameraFacing){
                    mCameraId = cameraId;
                    mFpsRange = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        Size previewSize = _getOptimalSize(map.getOutputSizes(SurfaceTexture.class), mConfig.resolution.getWidth(), mConfig.resolution.getHeight());
//                        Range<Integer>[] fpsRanges = map.getHighSpeedVideoFpsRangesFor(previewSize);///< high fps range
                        mConfig.resolution = new Size(previewSize.getHeight(),previewSize.getWidth());
                    }
                    return true;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Size _getOptimalSize(Size[] sizeMap, int width, int height) {
        ///< 根据外层配置分辨率寻找合适的分辨率
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() >= width && option.getHeight() >= height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() >= height && option.getHeight() >= width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private boolean _checkCameraService(){
        ///< 检测相机是否可用
        DevicePolicyManager dpm = (DevicePolicyManager)mContext.get().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.getCameraDisabled(null)) {
            return false;
        }
        return true;
    }

    private void _callBackError(int error, String errorMsg){
        ///< 错误回调
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.cameraOnError(error,TAG + errorMsg);
            });
        }
    }

    private Range<Integer> _chooseFpsRange() {
        ///< 根据外层配置的帧率寻找合适的帧率
        for(Range<Integer> range : mFpsRange){
            if(range.getUpper() >= mConfig.fps && range.getLower() <= mConfig.fps){
                return new Range<>(range.getLower(),mConfig.fps);
            }
        }

        return new Range<Integer>(0,0);
    }
}
