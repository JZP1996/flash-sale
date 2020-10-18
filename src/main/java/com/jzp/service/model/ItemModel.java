package com.jzp.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * FileName:    ItemModel
 * Author:      jzp
 * Date:        2020/6/7 9:45
 * Description: 商品 Model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemModel {

    /**
     * Id
     */
    private Integer id;
    /**
     * 标题（名称）
     */
    @NotBlank(message = "商品标题（名称）不能为空")
    private String title;
    /**
     * 价格
     */
    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格必须大于 0")
    private BigDecimal price;
    /**
     * 库存
     */
    @NotNull(message = "库存必填")
    private Integer stock;
    /**
     * 描述
     */
    @NotBlank(message = "商品描述信息不能为空")
    private String description;
    /**
     * 销量
     */
    private Integer sales;
    /**
     * 描述图片的 URL
     */
    @NotBlank(message = "商品图片信息不能为空")
    private String imageUrl;
    /**
     * 秒杀活动模型，不为空表示有还未开始或正在进行的秒杀活动
     */
    private PromoModel promoModel;
}
