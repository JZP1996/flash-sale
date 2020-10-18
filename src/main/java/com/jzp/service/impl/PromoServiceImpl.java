package com.jzp.service.impl;

import com.jzp.dataobject.Promo;
import com.jzp.enums.BusinessErrorEnum;
import com.jzp.error.BusinessException;
import com.jzp.mapper.PromoMapper;
import com.jzp.service.ItemService;
import com.jzp.service.PromoService;
import com.jzp.service.UserService;
import com.jzp.service.model.ItemModel;
import com.jzp.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * FileName:    PromoServiceImpl
 * Author:      jzp
 * Date:        2020/6/8 12:22
 * Description: 秒杀营销活动业务层实现类
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoMapper promoMapper;

    @Autowired
    private ItemService itemService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserService userService;

    /**
     * 根据 itemId 获取对应商品的秒杀活动信息
     */
    @Override
    public PromoModel getPromoModelByItemId(Integer itemId) {
        /* 根据 itemId 获取对应商品秒杀活动信息 */
        Promo promo = promoMapper.selectByItemId(itemId);
        /* 将 Promo 对象转换为 PromoModel 对象，并返回 PromoModel 对象 */
        return dataObject2Model(promo);
    }

    /**
     * 活动发布
     */
    @Override
    public void publishPromo(Integer promoId) {
        /* 根据活动 id 获取活动信息 */
        Promo promo = promoMapper.selectByPrimaryKey(promoId);
        if (promo.getItemId() != null && promo.getItemId() != 0) {
            /* 根据商品 id 获取 ItemModel */
            ItemModel itemModel = itemService.getItemModelById(promo.getItemId());
            /* 将库存信息同步到 Redis 内 */
            redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());
            /* 将大闸的限制数设置到 Redis 中 */
            redisTemplate.opsForValue().set("promo_door_count_" + promoId, itemModel.getStock() * 3);
        }
    }

    /**
     * 对活动信息、用户信息、商品信息进行校验，然后生成秒杀用的令牌
     */
    @Override
    public String generateFlashSaleToken(Integer promoId,
                                         Integer itemId,
                                         Integer userId) throws BusinessException {
        /* 判断是否售罄，如果 Redis 中 Key 存在，说明已经售罄 */
        Boolean hasKey = redisTemplate.hasKey("promo_item_stock_invalid_" + itemId);
        if (hasKey != null && hasKey) {
            throw new BusinessException(BusinessErrorEnum.STOCK_NOT_ENOUGH);
        }
        /* 根据 promoId 获取对应商品秒杀活动信息，并转换为 PromoModel 对象 */
        PromoModel promoModel = dataObject2Model(promoMapper.selectByPrimaryKey(promoId));
        /* 如果 promoModel 为 null 或者不是状态 2（正在进行中），直接返回 null，直接返回 */
        if (promoModel == null || promoModel.getStatus() != 2) {
            return null;
        }
        /* 获取并判断商品信息是否存在 */
        if (itemService.getItemModelByIdInCache(itemId) == null) {
            return null;
        }
        /* 获取并判断用户信息是否存在 */
        if (userService.getUserModelByIdInCache(userId) == null) {
            return null;
        }
        /* 获取秒杀大闸的数量 */
        Long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if (result == null || result < 0) {
            return null;
        }
        /* 生成秒杀令牌，将此令牌存入 Redis，并设置过期时间 */
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = "promo_token_" + promoId + "_userId_" + userId + "_itemId_" + itemId;
        redisTemplate.opsForValue().set(tokenKey, token);
        redisTemplate.expire(tokenKey, 5, TimeUnit.MINUTES);
        /* 返回 token */
        return token;
    }

    /**
     * 将 Promo 对象转换为 PromoModel 对象
     */
    private PromoModel dataObject2Model(Promo promo) {
        if (promo == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promo, promoModel);
        promoModel.setStartDate(new DateTime(promo.getStartDate()));
        promoModel.setEndDate(new DateTime(promo.getEndDate()));
        /* 判断是否是即将开始或正在进行的秒杀活动：1 表示未开始，2 表示进行中，3 表示已结束 */
        if (promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(1);
        } else if (promoModel.getEndDate().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        /* 返回 promoModel 对象 */
        return promoModel;
    }
}
