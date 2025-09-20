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
    private Boolean enabled;
    private Long deptId;
    private List<Long> extraDeptIds;
    private DataScope dataScope;
    private List<Long> dataScopeDeptIds;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime updatedAt;
}

