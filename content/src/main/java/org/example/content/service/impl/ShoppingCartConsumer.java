package org.example.content.service.impl;

import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.common.utils.Result;
import org.example.common.utils.RocketMQConstant;
import org.example.common.utils.UserContext;
import org.example.content.dao.ShoppingCartDao;
import org.example.content.pojo.ShoppingCart;
import org.example.content.service.IShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static org.example.content.util.utils.MY_SHOPPING_CART_CONTENT;

@Slf4j
@Component
// 监听指定 Topic 和消费者分组
@RocketMQMessageListener(
        topic = RocketMQConstant.SHOPPING_CART_INSERT_TOPIC,
        consumerGroup = RocketMQConstant.SHOPPING_CART_CONSUMER_GROUP,
        messageModel = MessageModel.CLUSTERING, // 集群模式（默认，避免重复消费）
        maxReconsumeTimes = 3 // 消费失败重试次数（超过后进入死信队列）
)
public class ShoppingCartConsumer implements RocketMQListener<ShoppingCart> {

    @Autowired
    private ShoppingCartDao shoppingCartMapper;

    @Override
    public void onMessage(ShoppingCart shoppingCart) {
        log.info("接收购物车入库消息，开始执行入库：{}", shoppingCart);
        try {
            int rows;
            System.out.println(shoppingCart);
            if(shoppingCartMapper.selectById(shoppingCart.getId())!=null){
                rows=shoppingCartMapper.updateById(shoppingCart);
            }
            else rows = shoppingCartMapper.insert(shoppingCart);
            if (rows > 0) {
                log.info("购物车入库成功：userId={}, knowledgeId={}",
                        shoppingCart.getUserId(), shoppingCart.getKnowledgeId());
                return;
            }
            log.error("购物车入库失败：影响行数为0，消息={}", shoppingCart);
            // 影响行数为0，抛出异常触发重试
            throw new RuntimeException("入库失败，影响行数为0");
        } catch (Exception e) {
            log.error("购物车入库异常：userId={}, knowledgeId={}",
                    shoppingCart.getUserId(), shoppingCart.getKnowledgeId(), e);
            // 抛出异常触发 RocketMQ 重试（最多 3 次）
            throw new RuntimeException("入库异常，触发重试", e);
        }
    }
}