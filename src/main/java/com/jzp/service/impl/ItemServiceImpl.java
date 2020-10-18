package com.jzp.service.impl;

import com.jzp.dataobject.Item;
import com.jzp.dataobject.ItemStock;
import com.jzp.dataobject.StockLog;
import com.jzp.enums.BusinessErrorEnum;
import com.jzp.error.BusinessException;
import com.jzp.mapper.ItemMapper;
import com.jzp.mapper.ItemStockMapper;
import com.jzp.mapper.StockLogMapper;
import com.jzp.mq.MqProducer;
import com.jzp.service.ItemService;
import com.jzp.service.PromoService;
import com.jzp.service.model.ItemModel;
import com.jzp.service.model.PromoModel;
import com.jzp.validator.BusinessValidator;
import com.jzp.validator.ValidationResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * FileName:    ItemServiceImpl
 * Author:      jzp
 * Date:        2020/6/7 10:13
 * Description: Item 业务层实现类
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemStockMapper itemStockMapper;

    @Autowired
    private BusinessValidator businessValidator;

    @Autowired
    private PromoService promoService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MqProducer producer;

    @Autowired
    private StockLogMapper stockLogMapper;

    /**
     * 根据 itemModel 的信息创建一个商品
     */
    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        /* 校验入参 */
        ValidationResult result = businessValidator.validate(itemModel);
        if (result.isHasError()) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, result.getErrorMsg());
        }
        /* itemModel 转换为 Item 对象 */
        Item item = model2DataObject(itemModel);
        /* Item 数据入库 */
        itemMapper.insertSelective(item);
        /* 设置 itemModel 中的 id */
        itemModel.setId(item.getId());
        /* itemModel 转化为 ItemStock 对象 */
        ItemStock itemStock = model2Stock(itemModel);
        /* ItemStock 数据入库 */
        itemStockMapper.insertSelective(itemStock);
        /* 返回创建完成的对象 */
        return itemModel;
    }

    /**
     * 查询所有商品信息，转换为 ItemModel 对象，组合成列表后返回
     */
    @Override
    public List<ItemModel> listItem() {
        List<Item> itemList = itemMapper.listItem();
        return itemList.stream().map(item -> {
            /* 根据商品 Id 查询 ItemStock */
            ItemStock itemStock = itemStockMapper.selectByItemId(item.getId());
            /* 封装并返回 */
            return dataObject2Model(item, itemStock);
        }).collect(Collectors.toList());
    }

    /**
     * 根据商品 ID 查询商品，并转换为 ItemModel
     */
    @Override
    public ItemModel getItemModelById(Integer id) {
        /* 根据商品 id 查询商品 */
        Item item = itemMapper.selectByPrimaryKey(id);
        /* 校验 */
        if (id == null) {
            return null;
        }
        /* 根据商品 id 查询商品库存信息 */
        ItemStock itemStock = itemStockMapper.selectByItemId(id);
        /* 封装结果并返回 */
        ItemModel itemModel = dataObject2Model(item, itemStock);
        /* 获取活动商品信息 */
        PromoModel promoModel = promoService.getPromoModelByItemId(id);
        /* 设置 ItemModel 的 promoModel 属性 */
        if (promoModel != null && promoModel.getStatus() != 3) {
            itemModel.setPromoModel(promoModel);
        }
        /* 返回封装解结果的 itemModel */
        return itemModel;
    }

    /**
     * 根据商品 id 尝试从缓存中获取 ItemModel，获取不到则从数据库中获取
     */
    @Override
    public ItemModel getItemModelByIdInCache(Integer id) {
        String key = "item_validate_" + id;
        /* 尝试从缓存中获取 ItemModel */
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get(key);
        /* 如果数据不存在 */
        if (itemModel == null) {
            /* 从数据库中获取 */
            itemModel = getItemModelById(id);
            /* 将 ItemModel 存放到 Redis，并设置超时时间 */
            redisTemplate.opsForValue().set(key, itemModel);
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
        }
        /* 返回结果 */
        return itemModel;
    }

    /**
     * 减库存
     */
    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        String key = "promo_item_stock_" + itemId;
        Long stockResult = redisTemplate.opsForValue().increment(key, amount * -1);
        if (stockResult == null) {
            throw new BusinessException(BusinessErrorEnum.STOCK_NOT_ENOUGH);
        }
        /* - 剩余库存数大于等于 0，说明可以购买
         *   - 剩余库存数大于 0，购买之后还有商品，直接返回 true 即可
         *   - 剩余库存数等于 0，此时表示售罄，需要打上标识，然后返回 true
         * - 剩余库存数小于 0，说明库存不足，无法购买，将 Redis 中的库存回滚，并返回 false
         */
        if (stockResult > 0) {
            return true;
        } else if (stockResult == 0) {
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");
            return true;
        } else {
            increaseStock(itemId, amount);
            return false;
        }
    }

    /**
     * 加库存
     */
    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount);
        return true;
    }

    /**
     * 异步更新库存
     */
    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        return producer.asyncReduceStock(itemId, amount);
    }

    /**
     * 加销量
     */
    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemMapper.increaseSales(itemId, amount);
    }

    /**
     * 初始化库存流水
     */
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLog stockLog = new StockLog();
        String stockLogId = UUID.randomUUID().toString().replace("-", "");
        stockLog.setStockLogId(stockLogId);
        stockLog.setItemId(itemId);
        stockLog.setAmount(amount);
        stockLog.setStatus(1);
        stockLogMapper.insertSelective(stockLog);
        return stockLogId;
    }

    /**
     * 将 ItemModel 对象转换为 Item 对象
     */
    private Item model2DataObject(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        Item item = new Item();
        BeanUtils.copyProperties(itemModel, item);
        return item;
    }

    /**
     * 将 Item 对象转换为 ItemModel 对象
     */
    private ItemStock model2Stock(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemStock itemStock = new ItemStock();
        itemStock.setItemId(itemModel.getId());
        itemStock.setStock(itemModel.getStock());
        return itemStock;
    }

    /**
     * 将 Item 封装成 ItemModel 对象
     */
    private ItemModel dataObject2Model(Item item) {
        if (item == null) {
            return null;
        }
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(item, itemModel);
        return itemModel;
    }

    /**
     * 将 Item 和 ItemStock 封装成 ItemModel 对象
     */
    private ItemModel dataObject2Model(Item item, ItemStock itemStock) {
        ItemModel itemModel = dataObject2Model(item);
        if (itemStock != null) {
            itemModel.setStock(itemStock.getStock());
        }
        return itemModel;
    }
}
