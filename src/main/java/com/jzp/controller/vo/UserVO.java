package com.jzp.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FileName:    UserVO
 * Author:      jzp
 * Date:        2020/6/3 9:35
 * Description: User 的 View Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telephone;
}
