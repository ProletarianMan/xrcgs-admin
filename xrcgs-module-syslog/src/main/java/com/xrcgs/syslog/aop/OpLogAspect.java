package com.xrcgs.syslog.aop;

import com.xrcgs.syslog.annotation.OpLog;
import com.xrcgs.syslog.config.OpLogProperties;
import com.xrcgs.syslog.entity.SysOpLog;
import com.xrcgs.syslog.service.SysOpLogService;
import com.xrcgs.syslog.util.LogJsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

import java.lang.reflect.Method;
import java.util.*;


/**
 * 核心方法，AOP切面，获取日志信息
 * 截断默认 2KB，避免撑爆日志；敏感键统一脱敏。
 * 失败兜底：saveSafe 捕获异常并 warn，不影响主流程。
 * 性能：不读取 HttpServletRequest 原始流，只序列化入参对象（过滤 Servlet、流、文件）。
 *
 */
@Slf4j
@Aspect
@Component
@Order(5) // 提前于大多数业务切面，但晚于 TraceFilter
@RequiredArgsConstructor
public class OpLogAspect {

    private final SysOpLogService opLogService;
    private final OpLogProperties properties;

    @Pointcut("@annotation(com.xrcgs.syslog.annotation.OpLog)")
    public void opLogPointcut() {}

    @Around("opLogPointcut() && @annotation(op)")
    public Object around(ProceedingJoinPoint pjp, OpLog op) throws Throwable {
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }

        long start = System.currentTimeMillis();
        boolean success = true;
        String exceptionShort = null;

        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        String className = pjp.getTarget().getClass().getName();
        String methodSign = className + "#" + method.getName() + '(' + Arrays.toString(ms.getParameterTypes()) + ')';

        HttpServletRequest request = currentRequest();
        String httpMethod = request != null ? request.getMethod() : null;
        String uri = request != null ? request.getRequestURI() : null;
        String ip = LogJsonUtils.clientIp(request);
        String query = request != null ? request.getQueryString() : null;

        String username = currentUsername();

        // 入参
        String reqBody = null;
        if (op.logArgs()) {
            Object[] args = pjp.getArgs();
            List<Object> filtered = new ArrayList<>();
            for (Object a : args) {
                if (!LogJsonUtils.isFilteredArg(a)) {
                    filtered.add(a);
                }
            }
            String json = LogJsonUtils.toJsonSafe(filtered);
            reqBody = LogJsonUtils.truncate(LogJsonUtils.maskSensitive(json), properties.getMaxBodyLength());
        }

        Object result = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            success = false;
            exceptionShort = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
            exceptionShort = LogJsonUtils.truncate(exceptionShort, 1000);
            throw ex; // 交给全局异常处理
        } finally {
            Long elapsed = System.currentTimeMillis() - start;

            // 入参兜底（即使没参数或都被过滤，也给一个占位）
            if (reqBody == null) {
                reqBody = "{}";
            }

            // 仅成功路径记录出参；失败保持 "{}" 占位，避免 NULL
            String respBody = "{}";
            if (properties.isLogResult() && op.logResult() && success) {
                String json = LogJsonUtils.toJsonSafe(result);
                respBody = LogJsonUtils.truncate(LogJsonUtils.maskSensitive(json), properties.getMaxBodyLength());
            }

            SysOpLog logRow = new SysOpLog();
            logRow.setTitle(op.value());
            logRow.setUsername(username);
            logRow.setMethodSign(methodSign);
            logRow.setHttpMethod(httpMethod);
            logRow.setUri(uri);
            logRow.setIp(ip);
            logRow.setQueryString(LogJsonUtils.truncate(query, 1024)); //2KB截断节省存储
            logRow.setReqBody(reqBody);
            logRow.setRespBody(respBody);
            logRow.setSuccess(success);
            logRow.setElapsedMs(elapsed);
            logRow.setExceptionMsg(exceptionShort);

            opLogService.saveSafe(logRow);

            // 控制台也打一条简要 info
            String traceId = MDC.get("traceId");
            log.info("日志 OP-LOG title='{}' user='{}' uri='{}' method='{}' success={} cost={}ms traceId={}",
                    op.value(), username, uri, httpMethod, success, elapsed, traceId);
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    /**
     * anonymous为匿名兜底
     * **/
    private String currentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return "anonymous";
            Object p = auth.getPrincipal();
            if (p == null) return "anonymous";
            // UserDetails 或 String
            return (p instanceof org.springframework.security.core.userdetails.UserDetails ud)
                    ? ud.getUsername()
                    : p.toString();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
