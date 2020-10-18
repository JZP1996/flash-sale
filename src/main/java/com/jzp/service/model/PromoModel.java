package com.jzp.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * FileName:    PromoModel
 * Author:      jzp
 * Date:        2020/6/8 10:59
 * Description: 秒杀营销活动模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoModel {

    /**
     * id
     */
    private Integer id;
    /**
     * 秒杀活动状态
     * 1 表示未开始，2 表示进行中，3 表示已结束
     */
    private Integer status;
    /**
     * 秒杀活动名称
     */
    private String promoName;
    /**
     * 秒杀活动开始时间
     */
    private DateTime startDate;
    /**
     * 秒杀活动结束时间
     */
    private DateTime endDate;
    /**
     * 秒杀活动的适用商品
     */
    private Integer itemId;
    /**
     * 秒杀活动的商品价格
     */
    private BigDecimal promoItemPrice;
}
