package com.jzp.controller;

import com.jzp.controller.vo.ItemVO;
import com.jzp.error.BusinessException;
import com.jzp.response.CommonReturnType;
import com.jzp.service.CacheService;
import com.jzp.service.ItemService;
import com.jzp.service.PromoService;
import com.jzp.service.model.ItemModel;
import com.jzp.service.model.PromoModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * FileName:    ItemController
 * Author:      jzp
 * Date:        2020/6/7 10:43
 * Description: Item 的控制器
 */
@Controller("item")
@RequestMapping("/item")
/* 解决跨域请求 */
@CrossOrigin(origins = {"*"}, allowCredentials = "true")
public class ItemController extends BaseController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建一个商品
     */
    @PostMapping(value = "/create", consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam("title") String title,
                                       @RequestParam("price") BigDecimal price,
                                       @RequestParam("stock") Integer stock,
                                       @RequestParam("description") String description,
                                       @RequestParam("imageUrl") String imageUrl) throws BusinessException {
        /* 根据参数创建 ItemModel，并封装数据 */
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setDescription(description);
        itemModel.setImageUrl(imageUrl);
        /* 调用 Service 创建商品 */
        ItemModel createResult = itemService.createItem(itemModel);
        /* 将创建的结果封装为 ItemVO，然后封装如 CommonReturnType 中并返回 */
        return CommonReturnType.create(model2VO(createResult));
    }

    /**
     * 获取一个商品
     */
    @GetMapping("/get")
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id) {
        String cacheKey = "item_" + id;
        /* 根据商品 ID 到本地缓存中获取 */
        ItemModel itemModel = (ItemModel) cacheService.getFromCommonCache(cacheKey);
        /* 如果本地缓存中不存在对应的 ItemModel */
        if (itemModel == null) {
            /* 根据商品 ID 到 Redis 中获取 */
            itemModel = (ItemModel) redisTemplate.opsForValue().get(cacheKey);
            /* 如果 Redis 中不存在对应的 ItemModel */
            if (itemModel == null) {
                /* 进入 Service 中获取 */
                itemModel = itemService.getItemModelById(id);
                /* 设置 ItemModel 对象到 Redis 中，然后设置过期时间 */
                redisTemplate.opsForValue().set(cacheKey, itemModel);
                redisTemplate.expire(cacheKey, 10, TimeUnit.MINUTES);
            }
            /* 设置 ItemModel 对象到本地对象中 */
            cacheService.setCommonCache(cacheKey, itemModel);
        }
        /* 将 ItemModel 转换为 ItemVO */
        ItemVO itemVO = model2VO(itemModel);
        /* 向前端返回信息 */
        return CommonReturnType.create(itemVO);
    }

    /**
     * 获取所有的商品
     */
    @GetMapping("/list")
    @ResponseBody
    public CommonReturnType listItems() {
        /* 查询所有 Item 并封装 ItemStock */
        List<ItemModel> itemModelList = itemService.listItem();
        /* 将 ItemModel 转换为 ItemVO */
        List<ItemVO> itemVOList = itemModelList.stream().map(this::model2VO).collect(Collectors.toList());
        /* 返回结果 */
        return CommonReturnType.create(itemVOList);
    }

    /**
     * 发布活动信息
     */
    @GetMapping("/publishpromo")
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam(name = "id") Integer id) {
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
    }


    /**
     * 将 ItemModel 对象转换为 ItemVO 对象
     */
    private ItemVO model2VO(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        /* 设置属性 */
        BeanUtils.copyProperties(itemModel, itemVO);
        /* 判断是否有即将开始或正在进行的秒杀活动 */
        if (itemModel.getPromoModel() != null) {
            PromoModel promoModel = itemModel.getPromoModel();
            /* 设置秒杀活动状态 */
            itemVO.setPromoStatus(promoModel.getStatus());
            /* 设置秒杀活动 id */
            itemVO.setPromoId(promoModel.getId());
            /* 设置秒杀活动开始时间 */
            itemVO.setPromoStartDate(promoModel.getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            /* 设置秒杀活动价格 */
            itemVO.setPromoPrice(promoModel.getPromoItemPrice());
        } else {
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }
}
