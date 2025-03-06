package com.kaku.avplayer.Base;

import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

//
//  YYFrame
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/20.
//
public class YYGLBase {
    public static String defaultVertexShader =
            //
            "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform   mat4 mvpMatrix;\n" +
                    "uniform mat4 textureMatrix;\n" +
                    "void main() {\n" +
                    "gl_Position = mvpMatrix * position;\n" +
                    "textureCoordinate = (textureMatrix * inputTextureCoordinate).xy;\n" +
                    "}\n";

    public static String defaultFragmentShader =
            //
            "precision mediump float;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "void main() {\n" +
                    "gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "}\n";


    public static String oesFragmentShader =
            //
            "#extension GL_OES_EGL_image_external : require \n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}\n";


    public static float[] YYIdentityMatrix() {
        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);
        return m;
    }
}
