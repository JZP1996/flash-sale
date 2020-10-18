package com.jzp.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;


/**
 * FileName:    BusinessValidator
 * Author:      jzp
 * Date:        2020/6/7 9:18
 * Description: 业务的 Validator
 */
@Component
public class BusinessValidator implements InitializingBean {

    private Validator validator;

    /**
     * 校验并返回校验结果
     */

    public ValidationResult validate(Object bean) {
        ValidationResult result = new ValidationResult();
        Set<ConstraintViolation<Object>> violationSet = validator.validate(bean);
        if (violationSet.size() > 0) {
            result.setHasError(true);
            violationSet.forEach(constraintViolation -> {
                /* 获取出错的属性名 */
                String propertyName = constraintViolation.getPropertyPath().toString();
                /* 获取错误信息 */
                String message = constraintViolation.getMessage();
                /* 将错误信息存放到 errorMsgMap */
                result.getErrorMsgMap().put(propertyName, message);
            });
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() {
        /* 将 Hibernate Validator 通过工厂使其实例化 */
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
}
