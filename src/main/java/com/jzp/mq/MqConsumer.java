package com.jzp.mq;

import com.alibaba.fastjson.JSON;
import com.jzp.mapper.ItemStockMapper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * FileName:    MqConsumer
 * Author:      jzp
 * Date:        2020/6/13 10:33
 * Description: 消息队列消费者
 */
@Component
public class MqConsumer {

    private DefaultMQPushConsumer consumer;

    @Value("${mq.nameserver.addr}")
    private String nameServerAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private ItemStockMapper itemStockMapper;


    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer("stock_consumer_group");
        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(topicName, "*");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            /* 获取一个消息 */
            Message msg = msgs.get(0);
            /* 获取（JSON）字符串 */
            String jsonString = new String(msg.getBody());
            /* 将 JSON 字符串 转换为 Map */
            Map<String, Object> map = JSON.parseObject(jsonString, Map.class);
            /* 获取 itemId 和 amount */
            Integer itemId = (Integer) map.get("itemId");
            Integer amount = (Integer) map.get("amount");
            /* 减库存 */
            itemStockMapper.decreaseStock(itemId, amount);
            /* 返沪结果 */
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();

    }
}
