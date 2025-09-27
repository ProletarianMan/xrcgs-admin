package com.xrcgs.iam.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xrcgs.iam.enums.DataScope;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情/分页返回
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String wechatId;
    private String phone;
    private Integer gender;
    private Boolean enabled;
    private DeptBriefVO dept;
    private List<Long> extraDeptIds;
    private DataScope dataScope;
    private List<Long> dataScopeDeptIds;
    private List<RoleBriefVO> roles;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime updatedAt;
}
