package com.jzp.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * FileName:    UserModel
 * Author:      jzp
 * Date:        2020/6/3 9:17
 * Description: User 在业务层的 Model Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserModel {

    /**
     * id
     */
    private Integer id;
    /**
     * 昵称
     */
    @NotBlank(message = "用户昵称不能为空")
    private String name;
    /**
     * 性别
     */
    @NotNull(message = "性别不能不填写")
    private Byte gender;
    /**
     * 年龄
     */
    @NotNull(message = "年龄不能不填写")
    @Min(value = 0, message = "年龄必须大于 0")
    @Max(value = 150, message = "年龄必须小于 150")
    private Integer age;
    /**
     * 手机号
     */
    @NotBlank(message = "手机号码不能为空")
    private String telephone;
    /**
     * 注册方式
     */
    private String registerMode;
    /**
     * 第三方账号 id
     */
    private String thirdPartyId;
    /**
     * 加密密码
     */
    @NotBlank(message = "密码不能为空")
    private String encryptPassword;
}
