package com.jzp.error;

/**
 * FileName:    CommonError
 * Author:      jzp
 * Date:        2020/6/3 10:21
 * Description: 通用错误接口
 */
public interface CommonError {

    int getErrorCode();
    String getErrorMsg();
    CommonError setErrorMsg(String errorMsg);
}
