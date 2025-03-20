# **AVPlayer - Android**

### **项目介绍**
AVPlayer - Android 是基于 Android 平台的音视频处理 Demo，借助 MediaCodec、OpenGL ES、AudioRecord 等框架，实现了音视频从采集、编码、封装、解封装、解码、渲染的流程。<br>

### **项目结构**

#### **Base 模块**
项目的基础部件，包含以下源文件：<br>
- YYAVTools：音频参数生产 AAC packet 对应的 ADTS 头数据。<br>
- YYFrame：音视频数据对象。<br>
- YYBufferFrame：继承自 YYFrame，包含 ByteBuffer 数据与 BufferInfo 数据信息。<br>
- YYGLBase：定义了默认的 VertexShader 和 FragmentShader。<br>
- YYTextureFrame：表示帧纹理对象。<br>

#### **Capture 模块**
负责音视频的采集功能，具体源文件如下：<br>
- YYAudioCapture：初始化方法、开始采集、停止采集接口。<br>
- YYAudioCaptureConfig：定义音频采集参数的配置。<br>
- YYAudioCaptureListener：实现采集回调，包含错误回调与数据回调。<br>
- YYVideoCapture：初始化、开始采集、停止采集、切换摄像头等接口。<br>
- YYVideoCaptureConfig：定义视频采集参数的配置。<br>
- YYVideoCaptureListener：提供了相机打开回调、相机关闭回调、以及相机出错回调的接口。<br>
- YYVideoCaptureV1/YYVideoCaptureV2：初始化接口、创建采集设备与开启预览、切换摄像头、停止视频采集、清理摄像机实例。<br>

#### **Demuxer 模块**
用于分离音视频流，具体如下：<br>
- YYDemuxerConfig：定义音视频解封装参数的配置。<br>
- YYDemuxerListener：解封装错误回调的接口。<br>
- YYMP4Demuxer：创建解封装器实例、清理实例、获取视频信息实例、输入源读取数据接口。<br>

#### **Muxer 模块**
用于将音视频流进行合并，包含的文件为：<br>
- YYMuxerConfig：定义 MP4 封装的参数的配置。<br>
- YYMuxerListener：封装错误回调的接口。<br>
- YYMP4Muxer：创建封装器实例、开始写入封装数据、停止写入封装数据和添加封装数据的接口。<br>

#### **MediaCodec 模块**
用于对采集到的音视频进行编码处理，包含以下文件：<br>
- YYMediaCodecInterface：<br>
- YYByteBufferCodec：创建与开启编码实例、停止与清理编码实例、刷新编码缓冲区、处理音频编码数据<br>
- YYByteBufferBufferEncoder：继承自 YYByteBufferCodec，重写 processFrame release flush 三个方法。<br>
- YYVideoEnocderConfig：配置编码相关参数<br>
- YYVideoSurfaceDecoder：与YYByteBufferCodec数据源不同，输入为纹理数据<br>
- YYVideoSurfaceEncoder：与YYByteBufferCodec数据源不同，输入为纹理数据<br>
- YYMediaCodecListener：<br>

#### **Render 模块**
负责将解码后的音视频进行渲染展示，有以下源文件：<br>
- YYAudioRender：创建音频渲染实例、实现开始渲染和停止渲染逻辑、清理音频渲染实例、处理音频渲染实例的数据回调。<br>
- YYAudioRenderListener：音频渲染数据输入回调和错误回调的接口。<br>
- YYRenderListener：定义渲染回调。<br>
- YYRenderView：管理 YYSurfaceView、YYTextureView 以及具体渲染逻辑。<br>
- YYSurfaceView：继承自 SurfaceView 来实现渲染。<br>
- YYTextureView：继承自 TextureView 来实现渲染。<br>

#### **Effect 模块**
协助构建渲染流程，源文件包括：<br>
- YYGLContext：创建 OpenGL 环境<br>
- YYGLFilter：自定义滤镜，外部输入纹理，进行自定义效果渲染。<br>
- YYGLFrameBuffer：封装了使用 FBO 的 API。<br>
- YYGLProgram：封装了使用 GL 程序的部分 API。<br>
- YYGLTextureAttributes：对纹理 Texture 属性的封装。<br>
- YYSurfaceTexture：处理纹理的生成、配置和释放。<br>
- YYSurfaceTextureListener：SurfaceTexture 数据回调。<br>


### **依赖说明**
项目依赖以下框架：<br>
- MediaCodec：提供音视频编解码能力。<br>
- OpenGL ES：用于实现视频渲染。<br>
- AudioRecord：提供音频录制等功能。<br>

