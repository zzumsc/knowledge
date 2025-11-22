package org.example.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.example.common.utils.Result;
import org.example.common.utils.RocketMQConstant;
import org.example.common.utils.UserContext;
import org.example.content.dao.ShoppingCartDao;
import org.example.content.pojo.ShoppingCart;
import org.example.content.service.IShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Long.parseLong;
import static org.example.content.util.utils.MY_SHOPPING_CART_CONTENT;
import static org.example.content.util.utils.SHOPPING_CART_CONTENT_TIME;

@Slf4j
@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartDao,ShoppingCart> implements IShoppingCartService {
    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    @Autowired
    RocketMQTemplate rocketMQTemplate;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createShoppingCart(ShoppingCart shoppingCart) {
        shoppingCart.setUserId(UserContext.getCurrentUser());
        List<ShoppingCart> l = (List<ShoppingCart>) redisTemplate.opsForValue().get(MY_SHOPPING_CART_CONTENT + shoppingCart.getUserId());
        if(l==null){
            l=query().eq("user_id", shoppingCart.getUserId()).list();
            redisTemplate.opsForValue().set(MY_SHOPPING_CART_CONTENT + shoppingCart.getUserId(), l);
        }
        for(ShoppingCart cart:l){
            if(cart.getKnowledgeId().equals(shoppingCart.getKnowledgeId())){
                shoppingCart.setId(cart.getId());
                shoppingCart.setUpdateTime(LocalDateTime.now());
                l.remove(cart);
                break;
            }
        }
        if(shoppingCart.getId()==null){
            shoppingCart.setId(parseLong(shoppingCart.getUserId().toString()+shoppingCart.getKnowledgeId().toString()));
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUpdateTime(LocalDateTime.now());
        }
        l.add(shoppingCart);
        redisTemplate.opsForValue().set(MY_SHOPPING_CART_CONTENT + shoppingCart.getUserId(), l,SHOPPING_CART_CONTENT_TIME, TimeUnit.MINUTES);

        try {
            // 构建消息（指定 Topic，Payload 为购物车对象）
            rocketMQTemplate.send(
                    RocketMQConstant.SHOPPING_CART_INSERT_TOPIC,
                    MessageBuilder.withPayload(shoppingCart).build()
            );
            log.info("购物车入库消息发送成功：userId={}, knowledgeId={}",
                    shoppingCart.getUserId(), shoppingCart.getKnowledgeId());
        } catch (Exception e) {
            log.error("购物车入库消息发送失败：userId={}, knowledgeId={}",
                    shoppingCart.getUserId(), shoppingCart.getKnowledgeId(), e);
        }
        return Result.ok("加入购物车成功").put("shoppingCart", shoppingCart);
    }

    @Override
    public Result getShoppingCart() {
        List<ShoppingCart> l= (List<ShoppingCart>) redisTemplate.opsForValue().get(MY_SHOPPING_CART_CONTENT+UserContext.getCurrentUser());
        if(l==null){
            l = query().eq("user_id", UserContext.getCurrentUser()).list();
            redisTemplate.opsForValue().set(MY_SHOPPING_CART_CONTENT+UserContext.getCurrentUser(), l);
        }
        if(l.isEmpty()){
            return Result.fail("未获取到购物车信息");
        }
        return Result.ok("查询成功").put("shoppingCart", l);
    }

    @Override
    public Result removeShoppingCart(List<Long> l) {
        boolean remove = remove(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, UserContext.getCurrentUser())
                .in(ShoppingCart::getKnowledgeId, l));
        if(remove)return Result.ok("删除成功");
        else return Result.fail("删除失败");
    }
}
