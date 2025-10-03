package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统部门实体
 */
@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;

    private String path;

    private String name;

    private String code;

    private Integer status;

    @TableField("sort_no")
    private Integer sortNo;

    private Long leaderUserId;

    private String phone;

    private String email;

    private String remark;

    private Long createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
