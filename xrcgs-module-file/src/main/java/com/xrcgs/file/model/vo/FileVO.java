package com.xrcgs.file.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文件返回前端类型
 */
@Data
@Builder
public class FileVO {
    private Long id;
    private String url;        // 下载 URL
    private String previewUrl; // 预览 URL（文档转 PDF 完成后）
    private String fileType;
    private String bizType;
    private String mime;
    private Long size;
    private String originalName; // 原文件名称
    private String status;
}
