package com.jzp.controller;

import org.springframework.stereotype.Controller;

/**
 * FileName:    BaseController
 * Author:      jzp
 * Date:        2020/6/3 10:51
 * Description: 基础的控制器
 */
@Controller
public class BaseController {

    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /*
    @ExceptionHandler
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonReturnType handlerException(HttpServletRequest request, Exception e) {
        Map<String, Object> data = new HashMap<>();
        if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            data.put("errorCode", businessException.getErrorCode());
            data.put("errorMsg", businessException.getErrorMsg());
        } else {
            e.printStackTrace();
            data.put("errorCode", BusinessErrorEnum.UNKNOWN_ERROR.getErrorCode());
            data.put("errorMsg", BusinessErrorEnum.UNKNOWN_ERROR.getErrorMsg());
        }
        return CommonReturnType.create("fail", data);
    }
    */
}
