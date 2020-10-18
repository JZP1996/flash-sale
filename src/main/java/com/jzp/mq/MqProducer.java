package com.jzp.mq;

import com.alibaba.fastjson.JSON;
import com.jzp.dataobject.StockLog;
import com.jzp.error.BusinessException;
import com.jzp.mapper.StockLogMapper;
import com.jzp.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * FileName:    MqProducer
 * Author:      jzp
 * Date:        2020/6/13 10:33
 * Description: 消息队列生产者
 */
@Component
public class MqProducer {

    private DefaultMQProducer defaultMQProducer;

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameServerAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockLogMapper stockLogMapper;

    @PostConstruct
    public void init() throws MQClientException {
        /* 初始化 DefaultMQProducer */
        defaultMQProducer = new DefaultMQProducer("producer_group");
        defaultMQProducer.setNamesrvAddr(nameServerAddr);
        defaultMQProducer.start();
        /* 初始化 TransactionMQProducer */
        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameServerAddr);
        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                Integer userId = (Integer) ((Map) arg).get("userId");
                Integer promoId = (Integer) ((Map) arg).get("promoId");
                Integer itemId = (Integer) ((Map) arg).get("itemId");
                Integer amount = (Integer) ((Map) arg).get("amount");
                String stockLogId = (String) ((Map) arg).get("stockLogId");
                try {
                    orderService.createOrder(userId, itemId, promoId, amount, stockLogId);
                } catch (BusinessException e) {
                    e.printStackTrace();
                    /* 根据 id 查询并设置 StockLog 为回滚状态 */
                    StockLog stockLog = stockLogMapper.selectByPrimaryKey(stockLogId);
                    stockLog.setStatus(3);
                    stockLogMapper.updateByPrimaryKeySelective(stockLog);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            /**
             * 根据是否扣减成功来判断要返回 COMMIT_MESSAGE、ROLLBACK_MESSAGE 还是 UNKNOWN
             */
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String jsonString = new String(msg.getBody());
                /* 将 JSON 字符串 转换为 Map */
                Map<String, Object> map = JSON.parseObject(jsonString, Map.class);
                /* 获取 itemId 和 amount */
                String stockLogId = (String) map.get("stockLogId");
                StockLog stockLog = stockLogMapper.selectByPrimaryKey(stockLogId);
                /*
                 * 如果 stockLog 为空或者状态为 1，说明操作未完成或者不知道进行到哪里，返回 UNKNOWN
                 * 如果状态为 2，说明操作完成且成功，返回 COMMIT_MESSAGE
                 * 其余情况返回 ROLLBACK_MESSAGE
                 */
                if (stockLog == null || stockLog.getStatus().equals(1)) {
                    return LocalTransactionState.UNKNOW;
                } else if (stockLog.getStatus().equals(2)) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }
        });
        transactionMQProducer.start();
    }

    /**
     * 事务型同步库存扣减消息
     */
    public boolean transactionAsyncReduceStock(Integer userId,
                                               Integer itemId,
                                               Integer promoId,
                                               Integer amount,
                                               String stockLogId) {
        /* Message Body */
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("stockLogId", stockLogId);
        /* args */
        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("userId", userId);
        argsMap.put("itemId", itemId);
        argsMap.put("promoId", promoId);
        argsMap.put("amount", amount);
        argsMap.put("stockLogId", stockLogId);
        /* 设置信息 */
        Message message = new Message(topicName,
                "increase",
                JSON.toJSON(bodyMap).toString().getBytes(StandardCharsets.UTF_8));
        TransactionSendResult result;
        /* 发送信息 */
        try {
            result = transactionMQProducer.sendMessageInTransaction(message, argsMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }
        /* 只有 result 不为 null，且是 COMMIT_MESSAGE 状态时候，才返回 true */
        return result != null && result.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE;
    }

    /**
     * 同步库存扣减消息
     */
    public boolean asyncReduceStock(Integer itemId, Integer amount) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        /* 设置信息 */
        Message message = new Message(topicName,
                "increase",
                JSON.toJSON(bodyMap).toString().getBytes(StandardCharsets.UTF_8));
        /* 发送信息 */
        try {
            defaultMQProducer.send(message);
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
