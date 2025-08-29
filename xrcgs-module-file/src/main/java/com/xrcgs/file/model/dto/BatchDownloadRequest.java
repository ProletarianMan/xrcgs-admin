package com.xrcgs.file.model.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量下载实体类
 */
@Data
public class BatchDownloadRequest {
    /** 选填：按 id 列表下载（与条件组合时取交集） */
    private List<Long> ids;

    /** 选填：按业务类型过滤 */
    private String bizType;

    /** 选填：按文件大类过滤（IMAGE/DOC/VIDEO/AUDIO） */
    private String fileType;

    /** 选填：时间范围 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime from;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime to;

    /** 选填：名称/sha256 关键字 */
    private String keyword;

    /** 选填：下载的 zip 文件名（不带扩展名），默认 files-时间戳 */
    private String zipName;

    /** 必填：最大打包文件数 */
    private Integer maxSize;
}
