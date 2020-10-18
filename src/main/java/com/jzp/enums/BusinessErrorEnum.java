package com.jzp.enums;

import com.jzp.error.CommonError;

/**
 * FileName:    BusinessException
 * Author:      jzp
 * Date:        2020/6/3 10:28
 * Description: 自定义错误的枚举类
 */
public enum BusinessErrorEnum implements CommonError {

    /* 以 10000 开头为通用错误类型，以 20000 开头的为用户相关错误，以 30000 开头为叫偶一相关错误 */
    /* 参数不合法错误 */
    PARAMETER_VALIDATION_ERROR(10001, "参数不合法"),
    /* 未知错误 */
    UNKNOWN_ERROR(10002, "未知错误"),
    /* 用户不存在 */
    USER_NOT_EXIST(20001, "用户不存在"),
    USER_LOGIN_ERROR(20002, "用户手机号或密码错误"),
    USER_NOT_LOGIN(20003, "用户还未登录"),
    STOCK_NOT_ENOUGH(30001, "库存不足"),
    MQ_SEND_FAIL(30002, "库存异步消息失败"),
    RATE_LIMIT(30003, "活动太火爆，请稍后再试");

    private final int errorCode;
    private String errorMsg;

    BusinessErrorEnum(int errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public CommonError setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }
}
