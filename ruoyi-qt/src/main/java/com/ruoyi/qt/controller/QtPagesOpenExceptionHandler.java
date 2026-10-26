package com.ruoyi.qt.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 开放数据 API 专属异常转换：把业务错误映射为真实 HTTP 状态，
 * 便于成员站点里的浏览器 JS 按 HTTP 状态判断成功/失败（与门户内部“HTTP 200 + body code”不同）。
 * 只作用于 QtPagesOpenController，不修改全局处理器。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = QtPagesOpenController.class)
public class QtPagesOpenExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(QtPagesOpenExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletResponse response)
    {
        int code = e.getCode() != null ? e.getCode() : HttpStatus.ERROR;
        log.warn("Pages 开放 API 业务错误: {}", e.getMessage());
        response.setStatus(code);
        return AjaxResult.error(code, e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult handleValidation(BindException e, HttpServletResponse response)
    {
        String message = e.getAllErrors().isEmpty() ? "请求参数不合法"
                : e.getAllErrors().get(0).getDefaultMessage();
        response.setStatus(HttpStatus.BAD_REQUEST);
        return AjaxResult.error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletResponse response)
    {
        response.setStatus(HttpStatus.BAD_REQUEST);
        return AjaxResult.error(HttpStatus.BAD_REQUEST, "请求参数不合法");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDenied(AccessDeniedException e, HttpServletResponse response)
    {
        response.setStatus(HttpStatus.FORBIDDEN);
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限访问该功能");
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleUnexpected(Exception e, HttpServletResponse response)
    {
        log.error("Pages 开放 API 未预期异常", e);
        response.setStatus(HttpStatus.ERROR);
        return AjaxResult.error(HttpStatus.ERROR, "系统内部错误，请联系管理员");
    }
}
