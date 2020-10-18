package com.jzp.handler;

import com.jzp.enums.BusinessErrorEnum;
import com.jzp.error.BusinessException;
import com.jzp.response.CommonReturnType;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * FileName:    GlobalExceptionHandler
 * Author:      jzp
 * Date:        2020/6/9 10:09
 * Description: 全局异常处理类
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CommonReturnType doError(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    Exception e) {
        e.printStackTrace();
        Map<String, Object> responseData = new HashMap<>();
        if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            responseData.put("errorCode", businessException.getErrorCode());
            responseData.put("errorMsg", businessException.getErrorMsg());
        } else if (e instanceof ServletRequestBindingException) {
            responseData.put("errorCode", BusinessErrorEnum.UNKNOWN_ERROR.getErrorCode());
            responseData.put("errorMsg", "URL 绑定路由问题");
        } else if (e instanceof NoHandlerFoundException) {
            responseData.put("errorCode", BusinessErrorEnum.UNKNOWN_ERROR.getErrorCode());
            responseData.put("errorMsg", "没有找到对应的访问路径");
        } else {
            responseData.put("errorCode", BusinessErrorEnum.UNKNOWN_ERROR.getErrorCode());
            responseData.put("errorMsg", BusinessErrorEnum.UNKNOWN_ERROR.getErrorMsg());
        }
        return CommonReturnType.create("fail", responseData);
    }
}