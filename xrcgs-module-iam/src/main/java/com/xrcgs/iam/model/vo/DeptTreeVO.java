package com.xrcgs.iam.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private final List<DeptTreeVO> children = new ArrayList<>();
}
