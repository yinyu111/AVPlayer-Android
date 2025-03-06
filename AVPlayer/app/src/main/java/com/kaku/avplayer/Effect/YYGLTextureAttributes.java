package com.kaku.avplayer.Effect;

//
//  YYAudioRenderListener
//  AVPlayer
//
//  Created by 尹玉 on 2025/3/4.
//

import android.opengl.GLES20;
public class YYGLTextureAttributes {
    public int minFilter = GLES20.GL_LINEAR;
    public int magFilter = GLES20.GL_LINEAR;
    public int wrapS = GLES20.GL_CLAMP_TO_EDGE;
    public int wrapT = GLES20.GL_CLAMP_TO_EDGE;
    public int internalFormat = GLES20.GL_RGBA;
    public int format = GLES20.GL_RGBA;
    public int type = GLES20.GL_UNSIGNED_BYTE;

    public YYGLTextureAttributes() {

    }
}
