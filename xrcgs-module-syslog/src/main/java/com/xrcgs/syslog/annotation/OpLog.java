package com.xrcgs.syslog.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {
    /** 操作标题（描述） */
    String value() default "";

    /** 是否记录入参 */
    boolean logArgs() default true;

    /** 是否记录出参 */
    boolean logResult() default true;
}
