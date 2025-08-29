package com.xrcgs.auth.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xrcgs.infrastructure.audit.UserIdAware;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * 统一的登录用户对象：
 * - 实现 UserDetails 供 Spring Security 使用
 * - 实现 UserIdAware 供审计（createdBy 等）获取 Long userId
 *
 * 你可以根据自己的业务在此扩展部门、手机号、角色等字段。
 */
public class LoginUser implements UserDetails, UserIdAware, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;

    @JsonIgnore // 避免被序列化到响应里
    private String password;

    @JsonIgnore
    private Collection<? extends GrantedAuthority> authorities;

    // 账号状态（默认都为 true，可按需持久化）
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;

    public LoginUser() {}

    public LoginUser(Long id,
                     String username,
                     String password,
                     Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    /** 方便构建 */
    public static LoginUser of(Long id, String username, String encodedPassword,
                               Collection<? extends GrantedAuthority> authorities) {
        return new LoginUser(id, username, encodedPassword, authorities);
    }

    /** ============ UserIdAware ============ */
    @Override
    public Long getId() {
        return id;
    }

    /** ============ UserDetails ============ */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // 注意：这里返回“登录名”，不强制与 id 相同
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** ============ 可选 setter（若你需要可变对象） ============ */
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) { this.authorities = authorities; }
    public void setAccountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }
    public void setAccountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }
    public void setCredentialsNonExpired(boolean credentialsNonExpired) { this.credentialsNonExpired = credentialsNonExpired; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** equals/hashCode 以 id 为主，避免重复认证的比较问题 */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginUser that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** 便于日志输出，不包含密码 */
    @Override
    public String toString() {
        return "LoginUser{id=" + id + ", username='" + username + "'}";
    }
}
