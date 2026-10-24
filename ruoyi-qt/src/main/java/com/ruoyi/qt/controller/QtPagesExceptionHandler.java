package com.ruoyi.qt.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
 * Pages 专属异常转换。
 *
 * 只作用于 Pages 控制器（assignableTypes 限定 + 高优先级），把业务错误同时映射为
 * 真实 HTTP 状态和 AjaxResult.code：400 参数错误、403 非有效塔员、404 非本人或不存在、
 * 409 唯一冲突、429 限流、503 功能关闭。不修改全项目 GlobalExceptionHandler。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = QtPageSiteController.class)
public class QtPagesExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(QtPagesExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletResponse response)
    {
        int code = e.getCode() != null ? e.getCode() : HttpStatus.ERROR;
        log.warn("Pages 业务错误: {}", e.getMessage());
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

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public AjaxResult handleUnreadable(Exception e, HttpServletResponse response)
    {
        response.setStatus(HttpStatus.BAD_REQUEST);
        return AjaxResult.error(HttpStatus.BAD_REQUEST, "请求参数不合法");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public AjaxResult handleDuplicateKey(DuplicateKeyException e, HttpServletResponse response)
    {
        log.warn("Pages 唯一冲突: {}", e.getMessage());
        response.setStatus(HttpStatus.CONFLICT);
        return AjaxResult.error(HttpStatus.CONFLICT, "站点创建冲突，请稍后重试");
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
        log.error("Pages 未预期异常", e);
        response.setStatus(HttpStatus.ERROR);
        return AjaxResult.error(HttpStatus.ERROR, "系统内部错误，请联系管理员");
    }
}
