package com.xrcgs.infrastructure.audit;

/** 让你的登录用户对象实现该接口，方便跨模块直接拿 Long ID */
public interface UserIdAware {
    Long getId();
}
