package com.why.framework.utils;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.base.Throwables;
import com.why.common.constants.APICons;
import com.why.common.exception.admin.AdminResultCode;
import com.why.common.exception.core.ErrorStatus;
import com.why.common.responses.ApiResponses;
import com.why.common.responses.FailedResponse;
import com.why.common.utils.IpUtils;
import com.why.common.utils.TypeUtils;
import com.why.framework.log.LogUtils;
import com.why.framework.servlet.wrapper.ResponseWrapper;
import com.why.framework.shiro.ShiroUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * response输出工具类
 *
 * @author Caratacus
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public abstract class ResponseUtils {

    /**
     * Portal输出json字符串
     *
     * @param response
     * @param obj      需要转换JSON的对象
     */
    public static void writeValAsJson(HttpServletRequest request, ResponseWrapper response, Object obj) {
        String userId = null;
        String loginName = null;
        try {
            userId = TypeUtils.castToString(ShiroUtils.getUserId());
        } catch (Exception ignored) {
        }
        try {
            loginName = TypeUtils.castToString(ShiroUtils.getLoginName());
        } catch (Exception ignored) {
        }
        int status = HttpServletResponse.SC_OK;
        if (Objects.nonNull(response)) {
            ErrorStatus errorStatus = response.getErrorCode();
            status = errorStatus.code();
        }
        LogUtils.printLog(status, (Long) request.getAttribute(APICons.API_BEGIN_TIME),
                userId,
                loginName,
                request.getParameterMap(),
                request.getAttribute(APICons.API_REQUEST_BODY),
                (String) request.getAttribute(APICons.API_REQURL),
                (String) request.getAttribute(APICons.API_ACTION_METHOD),
                request.getMethod(),
                IpUtils.getIpAddr(request),
                obj);
        if (ObjectUtils.isNotNull(response, obj)) {
            response.writeValueAsJson(obj);
        }
    }

    /**
     * 打印日志信息但是不输出到浏览器
     *
     * @param request
     * @param obj
     */
    public static void writeValAsJson(HttpServletRequest request, Object obj) {
        writeValAsJson(request, null, obj);
    }

    /**
     * 获取异常信息
     *
     * @param exception
     * @return
     */
    public static FailedResponse exceptionMsg(FailedResponse failedResponse, Exception exception) {
        if (exception instanceof MethodArgumentNotValidException) {
            StringBuilder builder = new StringBuilder("校验失败:");
            List<ObjectError> allErrors = ((MethodArgumentNotValidException) exception).getBindingResult().getAllErrors();
            allErrors.stream().findFirst().ifPresent(error -> {
                builder.append(((FieldError) error).getField()).append("字段规则为").append(error.getDefaultMessage());
                failedResponse.setMsg(error.getDefaultMessage());
            });
            failedResponse.setException(builder.toString());
            return failedResponse;
        } else if (exception instanceof MissingServletRequestParameterException) {
            StringBuilder builder = new StringBuilder("参数字段");
            MissingServletRequestParameterException ex = (MissingServletRequestParameterException) exception;
            builder.append(ex.getParameterName());
            builder.append("校验不通过");
            failedResponse.setException(builder.toString());
            failedResponse.setMsg(ex.getMessage());
            return failedResponse;
        } else if (exception instanceof MissingPathVariableException) {
            StringBuilder builder = new StringBuilder("路径字段");
            MissingPathVariableException ex = (MissingPathVariableException) exception;
            builder.append(ex.getVariableName());
            builder.append("校验不通过");
            failedResponse.setException(builder.toString());
            failedResponse.setMsg(ex.getMessage());
            return failedResponse;
        } else if (exception instanceof ConstraintViolationException) {
            StringBuilder builder = new StringBuilder("方法.参数字段");
            ConstraintViolationException ex = (ConstraintViolationException) exception;
            Optional<ConstraintViolation<?>> first = ex.getConstraintViolations().stream().findFirst();
            if (first.isPresent()) {
                ConstraintViolation<?> constraintViolation = first.get();
                builder.append(constraintViolation.getPropertyPath().toString());
                builder.append("校验不通过");
                failedResponse.setException(builder.toString());
                failedResponse.setMsg(ex.getMessage());
            }
            return failedResponse;
        }
        failedResponse.setException(TypeUtils.castToString(exception));
        return failedResponse;
    }

    /**
     * 发送错误信息
     *
     * @param request
     * @param response
     * @param errorStatus
     */
    public static void sendFail(HttpServletRequest request, HttpServletResponse response, ErrorStatus errorStatus,
                                Exception exception) {
        if (Objects.nonNull(exception)) {
            if (errorStatus.code() < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                log.info("Info: doResolveInfo {}", exception.getMessage());
            } else {
                log.warn("Warn: doResolveException {}", Throwables.getStackTraceAsString(exception));
            }
        }
        ResponseUtils.writeValAsJson(request, getWrapper(response, errorStatus), ApiResponses.failure(errorStatus, exception));
    }

    /**
     * 发送错误信息
     *
     * @param request
     * @param response
     * @param errorStatus
     */
    public static void sendFail(HttpServletRequest request, HttpServletResponse response, ErrorStatus errorStatus) {
        sendFail(request, response, errorStatus, null);
    }

    /**
     * 发送错误信息
     *
     * @param request
     * @param response
     * @param adminResultCode
     */
    public static void sendFail(HttpServletRequest request, HttpServletResponse response, AdminResultCode adminResultCode) {
        sendFail(request, response, adminResultCode, null);
    }

    /**
     * 发送错误信息
     *
     * @param request
     * @param response
     * @param adminResultCode
     * @param exception
     */
    public static void sendFail(HttpServletRequest request, HttpServletResponse response, AdminResultCode adminResultCode,
                                Exception exception) {
        sendFail(request, response, adminResultCode, exception);
    }

    /**
     * 获取Response
     *
     * @return
     */
    public static ResponseWrapper getWrapper(HttpServletResponse response, ErrorStatus errorStatus) {
        return new ResponseWrapper(response, errorStatus);
    }
}
