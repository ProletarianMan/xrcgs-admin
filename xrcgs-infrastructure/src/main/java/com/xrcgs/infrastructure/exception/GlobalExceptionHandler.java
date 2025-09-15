package com.xrcgs.infrastructure.exception;

import com.xrcgs.common.core.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * 全局异常处理（唯一实例）
 * 说明：放在 infrastructure 里，所有模块共用，避免重复定义。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(400, "缺少必填参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        var fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getField() + " " + fieldError.getDefaultMessage() : "参数校验失败";
        return R.fail(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        return R.fail(400, "参数校验失败：" + e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        return R.fail(400, "请求体解析失败");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDenied(AccessDeniedException e) {
        return R.fail(403, "无权访问");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        // 生产环境可以隐藏具体异常信息；这里返回统一提示
        log.error(e.getMessage(), e);
        return R.fail(500, "服务器开小差了");
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class})
    public R<Void> handleMaxUpload(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        return R.fail(413, "上传文件过大，请压缩或分批上传");
    }

    @ExceptionHandler({MultipartException.class})
    public R<Void> handleMultipart(MultipartException e) {
        // 兜底处理各种 multipart 解析异常
        return R.fail(400, "上传解析失败：" + e.getMessage());
    }
}
