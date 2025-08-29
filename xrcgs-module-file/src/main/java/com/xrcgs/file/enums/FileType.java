package com.xrcgs.file.enums;

/**
 * 文件类型
 */
public enum FileType {
    IMAGE, DOC, VIDEO, AUDIO;

    public static FileType fromMime(String mime) {
        if (mime == null) return DOC;
        if (mime.startsWith("image/")) return IMAGE;
        if (mime.startsWith("video/")) return VIDEO;
        if (mime.startsWith("audio/")) return AUDIO;
        return DOC;
    }
}
