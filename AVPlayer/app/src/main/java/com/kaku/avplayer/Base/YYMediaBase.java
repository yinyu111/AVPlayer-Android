package com.kaku.avplayer.Base;
//
//  YYMediaBase
//  AVPlayer
//
//  Created by 尹玉 on 2025/2/14.
//
public class YYMediaBase {

    public enum YYMediaType {
        YYMediaUnkown(0),
        YYMediaAudio(1 << 0),
        YYMediaVideo(1 << 1),
        YYMediaAV((1 << 0) | (1 << 1));

        private int index;
        YYMediaType(int index) {this.index = index; }

        public int value() { return index; }
    }
}
