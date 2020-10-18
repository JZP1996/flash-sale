package com.jzp.service;

import com.jzp.error.BusinessException;
import com.jzp.service.model.ItemModel;

import java.util.List;

/**
 * FileName:    ItemService
 * Author:      jzp
 * Date:        2020/6/7 10:05
 * Description: Item 业务层接口
 */
public interface ItemService {

    /**
     * 创建商品
     */
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    /**
     * 商品列表浏览
     */
    List<ItemModel> listItem();

    /**
     * 商品详情浏览
     */
    ItemModel getItemModelById(Integer id);

    /**
     * 根据商品 id 尝试从缓存中获取 ItemModel，获取不到则从数据库中获取
     */
    ItemModel getItemModelByIdInCache(Integer id);

    /**
     * 减库存
     */
    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;

    /**
     * 加库存
     */
    boolean increaseStock(Integer itemId, Integer amount);

    /**
     * 异步更新库存
     */
    boolean asyncDecreaseStock(Integer itemId, Integer amount);

    /**
     * 加销量
     */
    void increaseSales(Integer itemId, Integer amount);

    /**
     * 初始化库存流水
     */
    String initStockLog(Integer itemId, Integer amount);
}
