package com.jzp.service;

import com.jzp.error.BusinessException;
import com.jzp.service.model.UserModel;

/**
 * FileName:    UserService
 * Author:      jzp
 * Date:        2020/6/2 22:30
 * Description: User 的业务层接口
 */
public interface UserService {

    /**
     * 通过用户 id 获取用户信息，封装为 UserModel 后返回
     */
    UserModel getUserModelById(Integer id) throws BusinessException;

    /**
     * 根据用户 id 尝试从缓存中获取 UserModel，获取不到则从数据库中获取
     */
    UserModel getUserModelByIdInCache(Integer id) throws BusinessException;

    /**
     * 注册
     */
    void register(UserModel userModel) throws BusinessException;

    /**
     * 判断用户是否合法
     */
    UserModel isValid(String telephone, String password) throws BusinessException;
}
