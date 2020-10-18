package com.jzp.service;

import com.jzp.error.BusinessException;
import com.jzp.service.model.OrderModel;

/**
 * FileName:    OrderService
 * Author:      jzp
 * Date:        2020/6/7 21:52
 * Description: Order 的业务层接口
 */
public interface OrderService {

    /**
     * 根据用户 id、商品 id、购买商品的创建订单
     */
    OrderModel createOrder(Integer userId,
                           Integer itemId,
                           Integer promoId,
                           Integer amount,
                           String stockLogId) throws BusinessException;
}
