package com.kaku.avplayer.Effect;

//
//  YYAudioRenderListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/4.
//

import android.opengl.GLES20;
import android.util.Log;


import com.kaku.avplayer.Base.YYGLBase;

import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUseProgram;
public class YYGLProgram {
    private static final String TAG = "YYGLProgram";
    private int mProgram = 0;// 着色器程序容器
    private int mVertexShader = 0;// 顶点着色器
    private int mFragmentShader = 0;// 片元着色器

    public YYGLProgram() {
        _createProgram(YYGLBase.defaultVertexShader,YYGLBase.defaultFragmentShader);
    }

    public YYGLProgram(String vertexShader, String fragmentShader) {
        _createProgram(vertexShader,fragmentShader);
    }

    public YYGLProgram(String fragmentShader) {
        _createProgram(YYGLBase.defaultVertexShader,fragmentShader);
    }

    public void release() {
        // 释放顶点、片元着色器 着色器容器
        if (mVertexShader != 0){
            glDeleteShader(mVertexShader);
            mVertexShader = 0;
        }

        if (mFragmentShader != 0){
            glDeleteShader(mFragmentShader);
            mFragmentShader = 0;
        }

        if (mProgram != 0){
            glDeleteProgram(mProgram);
            mProgram = 0;
        }
    }

    public void use() {
        // 使用当前的着色器
        if(mProgram != 0){
            glUseProgram(mProgram);
        }
    }

    public int getUniformLocation(String uniformName) {
        // 获取着色器 uniform对应下标
        return glGetUniformLocation(mProgram, uniformName);
    }

    public int getAttribLocation(String uniformName) {
        // 获取着色器 attribute变量对应下标
        return glGetAttribLocation(mProgram, uniformName);
    }

    private void  _createProgram(String vertexSource, String fragmentSource) {
        // 创建着色器容器
        // 创建顶点、片元着色器
        mVertexShader = _loadShader(GLES20.GL_VERTEX_SHADER,   vertexSource);
        mFragmentShader = _loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        //
        if(mVertexShader != 0 && mFragmentShader != 0){
            // 创建一个空的着色器容器
            mProgram = GLES20.glCreateProgram();
            // 将顶点、片元着色器添加至着色器容器
            glAttachShader(mProgram, mVertexShader);
            glAttachShader(mProgram, mFragmentShader);

            // 链接着色器容器
            glLinkProgram(mProgram);
            int[] linkStatus = new int[1];
            glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
            // 获取链接状态
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program:");
                Log.e(TAG, glGetProgramInfoLog(mProgram));
                glDeleteProgram(mProgram);
                mProgram = 0;
            }
        }
    }

    private int _loadShader(int shaderType, String source) {
        // 根据类型创建顶点、片元着色器
        int shader = glCreateShader(shaderType);
        // 设置着色器中的源代码
        glShaderSource(shader, source);
        // 编译着色器
        glCompileShader(shader);

        int[] compiled = new int[1];
        glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        // 获取编译后状态
        if (compiled[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not compile shader(TYPE=" + shaderType + "):");
            Log.e(TAG, glGetShaderInfoLog(shader));
            glDeleteShader(shader);
            shader = 0;
        }

        return shader;
    }
}
