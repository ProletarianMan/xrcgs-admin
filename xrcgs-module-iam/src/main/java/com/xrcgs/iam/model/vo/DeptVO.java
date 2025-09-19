package com.xrcgs.iam.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门详情 VO
 */
@Data
public class DeptVO {
    private Long id;
    private Long parentId;
    private String path;
    private String name;
    private String code;
    private Integer status;
    private Integer sortNo;
    private Long leaderUserId;
    private String phone;
    private String email;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
