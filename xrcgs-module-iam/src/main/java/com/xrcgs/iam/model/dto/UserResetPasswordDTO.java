package com.xrcgs.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户重置密码请求体
 */
@Data
public class UserResetPasswordDTO {

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需在6~128个字符之间")
    private String password;
}
