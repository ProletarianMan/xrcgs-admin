package com.xrcgs.iam.model.query;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据模型
 * 字典类型查询条件
 *
 */
@Data
public class DictTypePageQuery {
    private String keyword;          // code/name 模糊
    private Integer status;          // 1/0
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
