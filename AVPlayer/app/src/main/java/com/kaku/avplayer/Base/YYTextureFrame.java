package com.kaku.avplayer.Base;

import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;
import static com.kaku.avplayer.Base.YYFrame.YYFrameType.YYFrameTexture;

//
//  YYFrame
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/20.
//

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
////extends !!!
public class YYTextureFrame extends YYFrame {
    public int textureId = -1;
    public Size textureSize = new Size(0, 0);
    public long nanoTime = 0;
    public Boolean isOESTexture = false;
    public float[] textureMatrix = YYGLBase.YYIdentityMatrix();
    public float[] positionMatrix = YYGLBase.YYIdentityMatrix();
    public boolean isEnd = false;

    public YYTextureFrame(int texture, Size size, long nanoTimeStamp) {
        super(YYFrameTexture);
        textureId = texture;
        textureSize = size;
        nanoTime = nanoTimeStamp;
    }

    public YYTextureFrame(int texture, Size size, long nanoTimeStamp, boolean isOES) {
        super(YYFrameTexture);
        textureId = texture;
        textureSize = size;
        nanoTime = nanoTimeStamp;
        isOESTexture = isOES;
    }

    public YYTextureFrame(int texture, Size size, long nanoTimeStamp, boolean isOES, final float[] texMatrix) {
        super(YYFrameTexture);
        textureId = texture;
        textureSize = size;
        nanoTime = nanoTimeStamp;
        isOESTexture = isOES;
        textureMatrix = texMatrix;
    }

    public YYTextureFrame(int texture, Size size, long nanoTimeStamp, boolean isOES, final float[] texMatrix, final float[] posMatrix) {
        super(YYFrameTexture);
        textureId = texture;
        textureSize = size;
        nanoTime = nanoTimeStamp;
        isOESTexture = isOES;
        textureMatrix = texMatrix;
        positionMatrix = posMatrix;
    }

    public YYTextureFrame(YYTextureFrame inputFrame) {
        super(YYFrameTexture);
        textureId = inputFrame.textureId;
        textureSize = inputFrame.textureSize;
        nanoTime = inputFrame.nanoTime;
        isOESTexture = inputFrame.isOESTexture;
        textureMatrix = inputFrame.textureMatrix;
    }

    public YYFrameType frameType() {
        return YYFrameTexture;
    }

    public long usTime() {
        return nanoTime / 1000;
    }

    public long msTime() {
        return nanoTime / 1000000;
    }
}
