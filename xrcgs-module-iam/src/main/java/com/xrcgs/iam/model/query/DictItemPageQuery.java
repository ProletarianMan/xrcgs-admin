package com.xrcgs.iam.model.query;

import lombok.Data;

/**
 * 字典项分页查询：按 type + label 模糊
 */
@Data
public class DictItemPageQuery {
    /** 字典类别（typeCode/type）——必填或选填均可 */
    private String typeCode;

    /** 字典项名称（label）——模糊匹配 */
    private String label;
}
