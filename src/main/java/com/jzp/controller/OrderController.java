package com.jzp.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.jzp.enums.BusinessErrorEnum;
import com.jzp.error.BusinessException;
import com.jzp.mq.MqProducer;
import com.jzp.response.CommonReturnType;
import com.jzp.service.ItemService;
import com.jzp.service.OrderService;
import com.jzp.service.PromoService;
import com.jzp.service.model.UserModel;
import com.jzp.utils.VerifyCodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * FileName:    OrderController
 * Author:      jzp
 * Date:        2020/6/8 9:13
 * Description: Order 的控制器
 */
@Controller("order")
@RequestMapping("/order")
/* 解决跨域请求 */
@CrossOrigin(origins = {"*"}, allowCredentials = "true")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init() {
        /* 初始化为大小 20 的线程池，用于队列泄洪 */
        executorService = Executors.newFixedThreadPool(20);
        /* 初始化 RateLimiter */
        orderCreateRateLimiter = RateLimiter.create(100);
    }

    /**
     * 生成验证码
     */
    @GetMapping(value = "/generateverifycode")
    @ResponseBody
    public void generateVerifyCode(HttpServletResponse httpServletResponse) throws BusinessException, IOException {
        /* 尝试从 Redis 中获取用户数据 */
        UserModel userModel = getUserModelFromRedisByToken("用户未登录，无法生成验证码");
        /* 调用验证码生成工具生成验证码 */
        Map<String, Object> map = VerifyCodeUtil.generateCodeAndPicture();
        /* 将验证码存放到 Redis，同时设置有效期 */
        String verifyCodeKey = "verify_code_" + userModel.getId();
        redisTemplate.opsForValue().set(verifyCodeKey, map.get("code"));
        redisTemplate.expire(verifyCodeKey, 5, TimeUnit.MINUTES);
        /* 将图片输出到 HttpServletResponse 的输出流 */
        ImageIO.write((RenderedImage) map.get("codePicture"), "jpeg", httpServletResponse.getOutputStream());
    }

    /**
     * 生成秒杀令牌
     */
    @PostMapping(value = "/generatetoken", consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(name = "itemId") Integer itemId,
                                          @RequestParam(name = "promoId") Integer promoId,
                                          @RequestParam(name = "verifyCode") String verifyCode) throws BusinessException {
        /* 获取并验证用户的登录态 */
        UserModel userModel = getUserModelFromRedisByToken("用户未登录，无法生成秒杀令牌");

        /* 获取并验证 verifyCode */
        String inRedisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if (StringUtils.isEmpty(inRedisVerifyCode)) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "请求非法");
        }
        if (!StringUtils.equalsIgnoreCase(inRedisVerifyCode, verifyCode)) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "请求非法，验证码错误");
        }
        /* 获取秒杀访问令牌，同时判断秒杀令牌是否正确生成（不为 null） */
        String promoToken = promoService.generateFlashSaleToken(promoId, itemId, userModel.getId());
        if (promoToken == null) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }

        /* 将 promoToken 返回给前端 */
        return CommonReturnType.create(promoToken);
    }

    @PostMapping(value = "/createOrder", consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "promoId", required = false) Integer promoId,
                                        @RequestParam(name = "promoToken", required = false) String promoToken,
                                        @RequestParam(name = "amount") Integer amount) throws BusinessException, ExecutionException, InterruptedException {
        if (!orderCreateRateLimiter.tryAcquire()) {
            throw new BusinessException(BusinessErrorEnum.RATE_LIMIT);
        }
        /* 获取并验证用户的登录态 */
        UserModel userModel = getUserModelFromRedisByToken("用户未登录，无法下单！");

        /* 校验秒杀令牌是否正确 */
        if (promoId != null) {
            /* Promo Token 在 Redis 中的 Key */
            String promoTokenKey = "promo_token_" + promoId + "_userId_" + userModel.getId() + "_itemId_" + itemId;
            /* 从 Redis 中获取 Promo Token，并对器进行校验，满足条件才能继续下单 */
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get(promoTokenKey);
            if (inRedisPromoToken == null) {
                throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
            if (!StringUtils.equals(inRedisPromoToken, promoToken)) {
                throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }

        /* 调用线程池来执行下单操作 */
        Future<Object> future = executorService.submit(() -> {
            /* 加入库存初始化状态 */
            String stockLogId = itemService.initStockLog(itemId, amount);
            /* 下单，同时根据返回值判断是否下单成功 */
            if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)) {
                throw new BusinessException(BusinessErrorEnum.UNKNOWN_ERROR, "下单失败");
            }
            return null;
        });
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new BusinessException(BusinessErrorEnum.UNKNOWN_ERROR);
        }

        /* 返还结果 */
        return CommonReturnType.create(null);
    }

    /**
     * 从 Session 中获取 token，然后根据 token 从 Redis 中获取对应的 UserModel
     * 同时，此过程包含对 token 和 UserModel 的校验
     */
    private UserModel getUserModelFromRedisByToken(String msg) throws BusinessException {
        /* 获取 Token，并判断 Token 是否为空，及判断用户是否登录 */
        String token = this.httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(BusinessErrorEnum.USER_NOT_LOGIN, msg);
        }
        /* 获取 Redis 中用户信息，并进行非空校验 */
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(BusinessErrorEnum.USER_NOT_LOGIN, msg);
        }
        /* userModel 不为空，正常返回 */
        return userModel;
    }
}
