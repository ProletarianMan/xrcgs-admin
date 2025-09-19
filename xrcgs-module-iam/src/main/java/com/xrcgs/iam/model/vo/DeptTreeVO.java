package com.xrcgs.iam.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树 VO
 */
@Data
public class DeptTreeVO {
    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private Integer status;
    private Integer sortNo;
    private String path;
    private Long leaderUserId;
    private String phone;
    private String email;
    private String remark;
    private final List<DeptTreeVO> children = new ArrayList<>();
}
