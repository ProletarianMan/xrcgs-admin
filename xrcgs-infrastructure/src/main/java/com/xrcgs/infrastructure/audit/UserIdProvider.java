package com.xrcgs.infrastructure.audit;

/** 获取当前登录用户ID的统一入口（Long） */
public interface UserIdProvider {
    Long getCurrentUserId();
}
