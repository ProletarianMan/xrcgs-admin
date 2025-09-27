package com.xrcgs.iam.model.dto;

import com.xrcgs.iam.enums.DataScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户新增/修改请求体
 */
@Data
public class UserUpsertDTO {

    /**
     * 主键，新增时忽略
     */
    private Long id;

    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    /**
     * 登录密码，新增时必填，修改时为空表示不变
     */
    @Size(min = 6, max = 128, message = "密码长度需在6~128个字符之间")
    private String password;

    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    @Size(max = 64, message = "微信号长度不能超过64个字符")
    private String wechatId;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    private String phone;

    @Min(value = 0, message = "性别取值只能为0或1")
    @Max(value = 1, message = "性别取值只能为0或1")
    private Integer gender;

    private Boolean enabled;

    private Long deptId;

    private List<Long> extraDeptIds;

    private DataScope dataScope;

    /**
     * 数据范围扩展，CUSTOM 模式下生效
     */
    private List<Long> dataScopeDeptIds;

    /**
     * 关联角色 ID 列表
     */
    private List<Long> roleIds;
}

