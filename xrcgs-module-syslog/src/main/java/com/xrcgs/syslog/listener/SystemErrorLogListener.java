package com.xrcgs.syslog.listener;

import com.xrcgs.common.event.SystemErrorEvent;
import com.xrcgs.syslog.entity.SysOpLog;
import com.xrcgs.syslog.service.SysOpLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 监听全局异常事件，将异常摘要落库到 sys_op_log。
 * 这里对 reqBody、respBody 等字段做了安全兜底，避免出现 NOT NULL 报错。
 */
@Component
@RequiredArgsConstructor
public class SystemErrorLogListener {

    private final SysOpLogService opLogService;

    @Async
    @Order(100)
    @EventListener(SystemErrorEvent.class)
    public void onError(SystemErrorEvent e) {
        SysOpLog row = new SysOpLog();

        // 标题/用户/请求基本信息
        row.setTitle(nvl(e.getTitle(), "系统异常"));
        row.setUsername(nvl(e.getUsername(), "anonymous"));
        row.setHttpMethod(nvl(e.getHttpMethod(), ""));
        row.setUri(safeLen(nvl(e.getUri(), ""), 512));
        row.setIp(nvl(e.getIp(), ""));
        row.setQueryString(safeLen(nvl(e.getQueryString(), ""), 1024));

        // 结果与异常
        row.setSuccess(false);
        row.setExceptionMsg(safeLen(nvl(e.getExceptionMsg(), ""), 1024));

        // AOP 不参与的异常链路，补齐默认 req/resp，避免列为 NULL
        row.setReqBody("{}");
        row.setRespBody("{}");

        // 本监听器不做方法签名与耗时统计，保持空值或 0
        row.setMethodSign("");
        row.setElapsedMs(0L);

        opLogService.saveSafe(row);
    }

    private static String nvl(String v, String def) {
        return (v == null) ? def : v;
    }

    private static String safeLen(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : (v.substring(0, max) + "...(truncated)");
    }
}
