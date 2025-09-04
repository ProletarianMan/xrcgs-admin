package com.xrcgs.iam.model.query;

import lombok.Data;

/**
 * 数据类型
 * 字典类型查询条件
 */
@Data
public class DictItemQuery {
    private String typeCode;
    private String keyword;  // label/value 模糊
    private Integer status;  // 1/0
}
