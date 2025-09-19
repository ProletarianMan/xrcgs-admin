package com.xrcgs.file.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xrcgs.file.enums.FileStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件对象实体类
 */
@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String bizType;
    private String fileType;

    private String originalName;
    private String ext;
    private String mime;
    private Long size;
    private String sha256;

    private String storagePath; // 相对路径：images/2025/08/28/xxx.jpg
    private String previewPath; // 相对路径：docs/2025/08/28/xxx.pdf → 放在 preview 根下

    private String status;      // FileStatus 枚举名
    private String errorMsg;

    private Long deptId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT) // ★ 新增：启用自动填充
    private Long createdBy;
}
