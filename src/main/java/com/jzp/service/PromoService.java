package com.jzp.service;

import com.jzp.error.BusinessException;
import com.jzp.service.model.PromoModel;

/**
 * FileName:    PromoService
 * Author:      jzp
 * Date:        2020/6/8 11:40
 * Description: 秒杀营销活动业务层接口
 */
public interface PromoService {

    /**
     * 根据 itemId 获取对应商品的秒杀活动信息
     */
    PromoModel getPromoModelByItemId(Integer itemId);

    /**
     * 活动发布
     */
    void publishPromo(Integer promoId);

    /**
     * 生成秒杀用的令牌
     */
    String generateFlashSaleToken(Integer promoId, Integer itemId, Integer userId) throws BusinessException;
}
