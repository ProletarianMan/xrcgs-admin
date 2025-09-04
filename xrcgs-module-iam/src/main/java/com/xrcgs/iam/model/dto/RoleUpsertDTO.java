package com.xrcgs.iam.model.dto;

import com.xrcgs.iam.enums.DataScope;
import lombok.Data;
import java.util.List;

/**
 * 数据传输对象
 * 角色
 */
@Data
public class RoleUpsertDTO {
    private Long id;             // null=新增，非空=修改
    private String code;
    private String name;
    private Integer status;
    private Integer sortNo;
    private DataScope dataScope; // ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM
    private List<Long> dataScopeDeptIds; // CUSTOM 时有效
    private String remark;
}
