package com.jzp.service.impl;

import com.jzp.dataobject.User;
import com.jzp.dataobject.UserPassword;
import com.jzp.enums.BusinessErrorEnum;
import com.jzp.error.BusinessException;
import com.jzp.mapper.UserMapper;
import com.jzp.mapper.UserPasswordMapper;
import com.jzp.service.UserService;
import com.jzp.service.model.UserModel;
import com.jzp.validator.BusinessValidator;
import com.jzp.validator.ValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * FileName:    UserServiceImpl
 * Author:      jzp
 * Date:        2020/6/2 22:31
 * Description: User 业务层实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPasswordMapper userPasswordMapper;

    @Autowired
    private BusinessValidator businessValidator;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public UserModel getUserModelById(Integer id) throws BusinessException {
        /* 根据用户 ID 获取用户信息 */
        User user;
        try {
            user = userMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorEnum.USER_NOT_EXIST);
        }
        /* 根据用户 ID 获取用户加密密码信息 */
        UserPassword userPassword = userPasswordMapper.selectByUserId(id);
        return dataObject2Model(user, userPassword);
    }

    /**
     * 根据用户 id 尝试从缓存中获取 UserModel，获取不到则从数据库中获取
     */
    @Override
    public UserModel getUserModelByIdInCache(Integer id) throws BusinessException {
        String key = "user_validate_" + id;
        /* 尝试从缓存中获取 */
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(key);
        /* 如果数据不存在 */
        if (userModel == null) {
            /* 从数据库中获取 */
            userModel = getUserModelById(id);
            /* 将 UserModel 存放到 Redis，并设置超时时间 */
            redisTemplate.opsForValue().set(key, userModel);
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
        }
        /* 返回结果 */
        return userModel;
    }

    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        /* userModel 校验 */
        if (userModel == null) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR);
        }
        /* userModel 内部数据校验 */
        ValidationResult result = businessValidator.validate(userModel);
        if (result.isHasError()) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, result.getErrorMsg());
        }
        /* 将 userModel 转换为 User 对象 */
        User user = model2DataObject(userModel);
        /* User 数据入库 */
        try {
            userMapper.insertSelective(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "手机号已注册");
        }
        /* 设置 userModel 中的 id，也就是 userId */
        userModel.setId(user.getId());
        /* 将 userModel 转换为 UserPassword 对象 */
        UserPassword userPassword = model2Password(userModel);
        /* Password 数据入库 */
        userPasswordMapper.insertSelective(userPassword);
    }

    /**
     * 通过用户手机号以及（加密后的）密码验证用户是否合法
     *
     * @param telephone       用户手机号
     * @param encryptPassword 用户加密后的密码
     */
    @Override
    public UserModel isValid(String telephone, String encryptPassword) throws BusinessException {
        /* 通过用户的手机号获取用户的信息 */
        User user = userMapper.selectByTelephone(telephone);
        /* 校验 */
        if (user == null) {
            throw new BusinessException(BusinessErrorEnum.USER_LOGIN_ERROR);
        }
        /* 通过 userId 获取用户的密码信息 */
        UserPassword userPassword = userPasswordMapper.selectByUserId(user.getId());
        /* 校验 */
        if (userPassword == null) {
            throw new BusinessException(BusinessErrorEnum.USER_LOGIN_ERROR);
        }
        /* 判断密码是否匹配 */
        if (!StringUtils.equals(userPassword.getEncryptPassword(), encryptPassword)) {
            throw new BusinessException(BusinessErrorEnum.USER_LOGIN_ERROR);
        }
        /* 将 user 和 userPassword 转换为 userModel，然后返回 */
        return dataObject2Model(user, userPassword);
    }

    /**
     * 将 User 转换为 UserModel，同时封装 Password
     */
    private UserModel dataObject2Model(User user, UserPassword userPassword) {
        if (user == null) {
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(user, userModel);
        if (userPassword != null) {
            userModel.setEncryptPassword(userPassword.getEncryptPassword());
        }
        return userModel;
    }

    /**
     * 将 UserModel 转换为 User
     */
    private User model2DataObject(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(userModel, user);
        return user;
    }

    /**
     * 将 UserModel 转换为 UserPassword
     */
    private UserPassword model2Password(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserPassword userPassword = new UserPassword();
        userPassword.setUserId(userModel.getId());
        userPassword.setEncryptPassword(userModel.getEncryptPassword());
        return userPassword;
    }
}
