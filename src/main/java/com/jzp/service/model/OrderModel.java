package com.jzp.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * FileName:    OrderModel
 * Author:      jzp
 * Date:        2020/6/7 20:25
 * Description: 订单 Model，也就是用户下单的交易模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderModel {

    /**
     * id
     */
    private String id;
    /**
     * 下单用户的 id
     */
    private Integer userId;
    /**
     * 购买商品的 id
     */
    private Integer itemId;
    /**
     * 秒杀商品对应的活动 id，非空表示该商品以秒杀形式下单
     */
    private Integer promoId;
    /**
     * 购买商品的单价，promoId 非空表示以秒杀形式下单
     */
    private BigDecimal itemPrice;
    /**
     * 购买数量
     */
    private Integer amount;
    /**
     * 订单总金额，promoId 非空表示以秒杀形式下单
     */
    private BigDecimal orderPrice;
}
