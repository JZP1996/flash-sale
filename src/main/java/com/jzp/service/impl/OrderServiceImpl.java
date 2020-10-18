package com.jzp.service.impl;

import com.jzp.dataobject.Order;
import com.jzp.dataobject.Sequence;
import com.jzp.dataobject.StockLog;
import com.jzp.enums.BusinessErrorEnum;
import com.jzp.error.BusinessException;
import com.jzp.mapper.OrderMapper;
import com.jzp.mapper.SequenceMapper;
import com.jzp.mapper.StockLogMapper;
import com.jzp.service.ItemService;
import com.jzp.service.OrderService;
import com.jzp.service.UserService;
import com.jzp.service.model.ItemModel;
import com.jzp.service.model.OrderModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * FileName:    OrderServiceImpl
 * Author:      jzp
 * Date:        2020/6/7 21:57
 * Description: Order 的业务层实现类
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SequenceMapper sequenceMapper;

    @Autowired
    private StockLogMapper stockLogMapper;

    /**
     * 创建订单
     */
    @Override
    @Transactional
    public OrderModel createOrder(Integer userId,
                                  Integer itemId,
                                  Integer promoId,
                                  Integer amount,
                                  String stockLogId) throws BusinessException {
        /* 根据 itemId 获取 ItemModel */
        ItemModel itemModel = itemService.getItemModelByIdInCache(itemId);
        if (itemModel == null) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
        /* 下单前的校验：商品数量是否合法 */
        if (amount <= 0 || 99 < amount) {
            throw new BusinessException(BusinessErrorEnum.PARAMETER_VALIDATION_ERROR, "购买商品数量并不正确");
        }
        /* 落单减库存：订单创建时就将库存减去，锁定给当前用户使用 */
        boolean decreaseResult = itemService.decreaseStock(itemId, amount);
        if (!decreaseResult) {
            throw new BusinessException(BusinessErrorEnum.STOCK_NOT_ENOUGH);
        }
        /* 创建一个 OrderModel 对象，并封装基本数据 */
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(itemModel.getPrice().multiply(new BigDecimal(amount)));
        /* 生成交易流水号，即订单表主键 */
        orderModel.setId(generateOrderNumber());
        /* 将 OrderModel 对象转换为 Order 对象 */
        Order order = model2DataObject(orderModel);
        /* 订单数据入库 */
        orderMapper.insertSelective(order);
        /* 增加销量 */
        itemService.increaseSales(itemId, amount);
        /* 查询库存流水，然后设置库存流水状态为成功，同时将数据入库 */
        StockLog stockLog = stockLogMapper.selectByPrimaryKey(stockLogId);
        if (stockLog == null) {
            throw new BusinessException(BusinessErrorEnum.UNKNOWN_ERROR);
        }
        stockLog.setStatus(2);
        stockLogMapper.updateByPrimaryKeySelective(stockLog);
        /* 返回前端 */
        return orderModel;
    }

    /**
     * 十六位订单号：时间 + 自增序列 + 分库分表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected String generateOrderNumber() {
        StringBuilder sb = new StringBuilder();
        /* 8 位表示时间（年月日） */
        LocalDateTime now = LocalDateTime.now();
        sb.append(now.format(DateTimeFormatter.ISO_DATE).replace("-", ""));
        /* 6 位表示自增序列 */
        /* 获取当前 Sequence */
        Sequence orderSequence = sequenceMapper.getSequenceByName("order_information");
        /* 获取当前 Sequence 的值 */
        Integer currentValue = orderSequence.getCurrentValue();
        /* 设置下一个序列 */
        orderSequence.setCurrentValue(currentValue + orderSequence.getStep());
        /* Sequence 数据入库 */
        sequenceMapper.updateByPrimaryKeySelective(orderSequence);
        /* currentValue 凑足 6 位 */
        String currentValueString = String.valueOf(currentValue);
        for (int i = 0; i < 6 - currentValueString.length(); i++) {
            sb.append("0");
        }
        sb.append(currentValueString);
        /* 2 位表示分库、分表（这里没有分库分表，暂时直接设置位 00） */
        sb.append("00");
        return sb.toString();
    }

    /**
     * 将 OrderModel 对象转换为 Order 对象
     */
    private Order model2DataObject(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        Order order = new Order();
        BeanUtils.copyProperties(orderModel, order);
        return order;
    }
}
