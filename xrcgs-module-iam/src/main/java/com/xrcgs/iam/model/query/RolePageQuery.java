package com.xrcgs.iam.model.query;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据模型
 * 角色查询脚尖
 */
@Data
public class RolePageQuery {
    private String keyword;           // 按 code/name 模糊
    private Integer status;           // 1/0
    private LocalDateTime startTime;  // 创建时间起
    private LocalDateTime endTime;    // 创建时间止
}
