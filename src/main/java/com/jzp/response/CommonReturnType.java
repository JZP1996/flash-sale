package com.jzp.response;

import lombok.Data;

/**
 * FileName:    CommonResponse
 * Author:      jzp
 * Date:        2020/6/3 9:54
 * Description: 通用返回类型
 */
@Data
public class CommonReturnType {

    /**
     * 表明对应请求的处理结果，"success" 或 "fail"
     */
    private String status;
    /**
     * 数据
     * 如果 status 为 "success"，则为前端所需要的数据
     * 如果 status 为 "fail"，则使用通用的错误码格式
     */
    private Object data;

    private CommonReturnType() {
    }

    /**
     * status 为 "success" 时 CommonReturnType 的创建
     */
    public static CommonReturnType create(Object data) {
        return create("success", data);
    }

    /**
     * 根据 status 和 data 创建
     */
    public static CommonReturnType create(String status, Object data) {
        CommonReturnType commonReturnType = new CommonReturnType();
        commonReturnType.setStatus(status);
        commonReturnType.setData(data);
        return commonReturnType;
    }
}
