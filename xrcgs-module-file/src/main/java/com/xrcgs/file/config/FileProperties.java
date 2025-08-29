// xrcgs-module-file/src/main/java/com/xrcgs/file/config/FileProperties.java
package com.xrcgs.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 文件对象属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /** 上传根目录（相对/绝对） */
    private String storageRoot = "data/uploads";

    /** 预览/转换输出根目录 */
    private String previewRoot = "data/preview";

    /** 最大大小（字节），默认 100MB */
    private long maxSize = 100 * 1024 * 1024L;

    /** 预览是否需要鉴权（默认 false 便于联调） */
    private boolean previewAuthRequired = false;

    /** 允许（可选）bizType 白名单；为空则不强校验，仅记录 */
    private Set<String> bizTypeWhitelist;

    /** 允许的扩展名（含点/不含点均可；最终会做归一化） */
    private List<String> allowedExts = List.of(
            // images
            "jpg","jpeg","png","webp","gif",
            // docs
            "doc","docx","xls","xlsx","ppt","pptx","pdf",
            // video
            "mp4","mov","mkv","avi",
            // audio
            "mp3","wav","m4a","aac"
    );

    /** 允许的 MIME 前缀或完整值 */
    private List<String> allowedMimes = List.of(
            "image/", "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "video/", "audio/"
    );
}
