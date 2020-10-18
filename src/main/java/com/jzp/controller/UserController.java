package com.jzp.controller;

import com.jzp.controller.vo.UserVO;
import com.jzp.enums.BusinessErrorEnum;
import com.jzp.error.BusinessException;
import com.jzp.response.CommonReturnType;
import com.jzp.service.UserService;
import com.jzp.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * FileName:    UserController
 * Author:      jzp
 * Date:        2020/6/2 22:24
 * Description: User 的控制器
 */
@Controller("user")
@RequestMapping("/user")
/* 解决跨域请求 */
@CrossOrigin(origins = {"*"}, allowCredentials = "true")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据 ID 获取用户信息
     */
    @GetMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        /* 调用 Service，获取对应 id 的 UserModel */
        UserModel userModel = userService.getUserModelById(id);
        /* 数据校验 */
        if (userModel == null) {
            throw new BusinessException(BusinessErrorEnum.USER_NOT_EXIST);
        }
        /* 将 UserModel 转换为 UserVO */
        UserVO userVO = model2VO(userModel);
        /* 向前端页面返回结果 */
        return CommonReturnType.create(userVO);
    }

    /**
     * 用户获取 OTP 短信接口
     */
    @PostMapping(value = "/getotp", consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telephone") String telephone) {
        Random random = new Random();
        /* 生成 [0, 900000) 的一个随机数，加上 100000 之后范围变成 [100000, 999999] */
        int i = random.nextInt(900000) + 100000;
        /* 转为 String */
        String otpCode = String.valueOf(i);
        /* 使用 HttpSession 方式将 OTP 验证码与对应用户手机号关联 */
        this.httpServletRequest.getSession().setAttribute(telephone, otpCode);
        /* 将 OTP 短信验证码通过短信通道发送给用户（略），这里直接通过打印方式假装发送短信 */
        System.out.println("telephone: " + telephone + " & optCode: " + otpCode);
        /* 向前端页面返回结果 */
        return CommonReturnType.create(null);
    }

    /**
     * 用户注册
     */
    @PostMapping(value = "/register", consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telephone") String telephone,
                                     @RequestParam(name = "otpCode") String otpCode,
                                     @RequestParam(name = "name") String name,
                                     @RequestParam(name = "password") String password,
                                     @RequestParam(name = "gender") Integer gender,
                                     @RequestParam(name = "age") Integer age) throws NoSuchAlgorithmException, BusinessException {
        /* 验证手机号与 otpCode 是否相符 */
        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telephone);
        if (!StringUtils.equals(otpCode, inSessionOtpCode)) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "短信验证码不符");
        }
        /* 构建 UserModel 对象并设置属性 */
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(Byte.valueOf(String.valueOf(gender.intValue())));
        userModel.setTelephone(telephone);
        userModel.setAge(age);
        userModel.setRegisterMode("ByPhone");
        userModel.setEncryptPassword(encodeByMD5(password));
        /* 调用 Service 进行注册 */
        userService.register(userModel);
        return CommonReturnType.create(null);
    }

    /**
     * 用户登录
     */
    @PostMapping(value = "/login", consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telephone") String telephone,
                                  @RequestParam(name = "password") String password) throws NoSuchAlgorithmException, BusinessException {
        /* 参数校验 */
        if (StringUtils.isEmpty(telephone) || StringUtils.isEmpty(password)) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR);
        }
        /* 根据手机号以及（加密后的）密码验证用户是否合法 */
        UserModel userModel = userService.isValid(telephone, encodeByMD5(password));
        /* 如果用户登录成功，将登录信息和登录凭证一起存入 Redis 中 */
        /* 生成登录凭证，这里使用 UUID */
        String uudiToken = UUID.randomUUID().toString();
        uudiToken = uudiToken.replace("-", "");
        /* 建立 Token 与用户登录态之间的关系 */
        redisTemplate.opsForValue().set(uudiToken, userModel);
        /* 设置超时时间 */
        redisTemplate.expire(uudiToken, 1, TimeUnit.HOURS);
        /* 下发 Token */
        return CommonReturnType.create(uudiToken);
    }

    /**
     * 将 UserModel 转换为 UserVO
     */
    private UserVO model2VO(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

    /**
     * MD5 加密
     */
    private String encodeByMD5(String origin) throws NoSuchAlgorithmException {
        /* 确定计算方法 */
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        /* 构建加密对象 */
        BASE64Encoder base64Encoder = new BASE64Encoder();
        /* 加密 */
        return base64Encoder.encode(md5.digest(origin.getBytes()));

    }
}
