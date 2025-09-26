package com.xrcgs.iam.model.dto;

import com.xrcgs.iam.enums.DataScope;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    /**
     * 登录密码，新增时必填，修改时为空表示不变
     */
    @Size(min = 6, max = 128, message = "密码长度需在6~128个字符之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    @Size(max = 64, message = "微信号长度不能超过64个字符")
    private String wechatId;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    private String phone;

    private Boolean enabled;

    private Long deptId;

    private List<Long> extraDeptIds;

    private DataScope dataScope;

    /**
     * 数据范围扩展，CUSTOM 模式下生效
     */
    private List<Long> dataScopeDeptIds;
}

