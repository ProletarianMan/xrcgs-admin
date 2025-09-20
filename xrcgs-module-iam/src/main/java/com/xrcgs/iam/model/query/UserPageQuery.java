package com.xrcgs.iam.model.query;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户分页查询条件
 */
@Data
public class UserPageQuery {
    private String username;
    private String nickname;
    private Long deptId;
    private Boolean enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

