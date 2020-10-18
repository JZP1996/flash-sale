package com.jzp.validator;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * FileName:    ValidationResult
 * Author:      jzp
 * Date:        2020/6/7 9:07
 * Description: Validator 结果
 */
@Data
public class ValidationResult {

    /**
     * 校验结果是否有错
     */
    private boolean hasError = false;

    /**
     * 存放错误信息的 Map
     */
    private Map<String, String> errorMsgMap = new HashMap<>();

    /**
     * 通用的格式化字符串信息获取错误结果信息
     */
    public String getErrorMsg() {
        return StringUtils.join(errorMsgMap.values().toArray(), ", ");
    }
}
