package com.jzp.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * FileName:    ItemVO
 * Author:      jzp
 * Date:        2020/6/7 10:45
 * Description: Item 的 View Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemVO {

    /**
     * Id
     */
    private Integer id;
    /**
     * 标题（名称）
     */
    private String title;
    /**
     * 价格
     */
    private BigDecimal price;
    /**
     * 库存
     */
    private Integer stock;
    /**
     * 描述
     */
    private String description;
    /**
     * 销量
     */
    private Integer sales;
    /**
     * 描述图片的 URL
     */
    private String imageUrl;
    /**
     * 商品是否处于秒杀状态
     * 0 表示没有秒杀活动，1 表示秒杀活动带开始，2 表示m秒杀活动进行中
     */
    private Integer promoStatus;
    /**
     * 秒杀活动价格
     */
    private BigDecimal promoPrice;
    /**
     * 秒杀活动 id
     */
    private Integer promoId;
    /**
     * 秒杀活动开始时间
     */
    private String promoStartDate;
}
