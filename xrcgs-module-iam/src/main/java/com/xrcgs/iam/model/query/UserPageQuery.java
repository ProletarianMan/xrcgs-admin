package com.xrcgs.iam.model.query;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户分页查询条件
 */
@Data
public class UserPageQuery {
    private String username;
    private String nickname;
    private Long deptId;
    private List<Long> deptIds;
    private Boolean enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

