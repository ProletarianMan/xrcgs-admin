package com.xrcgs.iam.model.vo;

import lombok.Data;

@Data
public class UserSimpleVO {
    private String username;
    private String nickname;
    private Integer gender;
    private Boolean currentUser;
    private Long deptId;
    private String deptName;
}
