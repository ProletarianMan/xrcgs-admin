package com.xrcgs.common.core;

import lombok.Data;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 统一返回包装类
 * 结构：{ "code": 200, "message": "ok", "data": {...}, "timestamp": 1711111111 }
 */
@Data
public class R<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务码：200 成功、非 200 失败（可结合网关或前端约定） */
    private int code;
    /** 文本消息 */
    private String message;
    /** 负载数据 */
    private T data;
    /** 时间戳（秒） */
    private long timestamp;

    public R() {
        this.timestamp = Instant.now().getEpochSecond();
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /* ---------- 静态便捷方法 ---------- */

    public static <T> R<T> ok() {
        return new R<>(200, "ok", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "ok", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 根据布尔值快速返回
     */
    public static R<Boolean> of(boolean success) {
        return success ? ok(true) : fail("operation failed");
    }

    /* ---------- 常用辅助 ---------- */

    public boolean isSuccess() {
        return this.code == 200;
    }
}
