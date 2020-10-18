package com.jzp.error;

/**
 * FileName:    BusinessException
 * Author:      jzp
 * Date:        2020/6/3 10:28
 * Description: 包装器业务异常
 */
public class BusinessException extends Exception implements CommonError {

    private final CommonError commonError;

    /**
     * 直接接收一个 CommonError，用于构造业务异常
     */
    public BusinessException(CommonError commonError) {
        super();
        this.commonError = commonError;
    }

    /**
     * 接收自定义 errorMsg 的方式构造业务异常
     */
    public BusinessException(CommonError commonError, String errorMsg) {
        super();
        this.commonError = commonError;
        this.commonError.setErrorMsg(errorMsg);
    }

    @Override
    public int getErrorCode() {
        return this.commonError.getErrorCode();
    }

    @Override
    public String getErrorMsg() {
        return this.commonError.getErrorMsg();
    }

    @Override
    public CommonError setErrorMsg(String errorMsg) {
        this.commonError.setErrorMsg(errorMsg);
        return this;
    }
}
