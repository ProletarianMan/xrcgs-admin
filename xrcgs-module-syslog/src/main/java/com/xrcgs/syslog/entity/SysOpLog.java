package com.xrcgs.syslog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志实体类
 */
@Data
@TableName("sys_op_log")
public class SysOpLog {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("title")
    private String title;

    @TableField("username")
    private String username;

    /** 完整方法签名：类名#方法(参数类型...) */
    @TableField("methodSign")   // 表里是驼峰列
    private String methodSign;

    @TableField("httpMethod")
    private String httpMethod;

    @TableField("uri")
    private String uri;

    @TableField("ip")
    private String ip;

    @TableField("queryString")
    private String queryString;

    /** 入参、出参（均做截断） */
    @TableField("reqBody")
    private String reqBody;
    @TableField("respBody")
    private String respBody;

    /** 是否成功 */
    @TableField("success")
    private Boolean success;

    /** 耗时 ms */
    @TableField("elapsedMs")
    private Long elapsedMs;

    /** 异常摘要（截断） */
    @TableField("exceptionMsg")
    private String exceptionMsg;

    @TableField(value = "createdAt", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
