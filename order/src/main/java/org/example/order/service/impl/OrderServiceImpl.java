package org.example.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.common.utils.Result;
import org.example.common.utils.UserContext;
import org.example.order.clients.ContentClient;
import org.example.order.dao.OrderDao;
import org.example.order.pojo.Order;
import org.example.order.pojo.PayRecord;
import org.example.order.pojo.vo.OrderDetail;
import org.example.order.service.IOrderService;
import org.example.order.service.IPayRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.order.util.RandomId.generateRandomId;
import static org.example.order.util.RandomString.generateRandomString;
import static org.example.order.util.utils.MY_ORDER_CONTENT;


@Service
public class OrderServiceImpl extends ServiceImpl<OrderDao, Order> implements IOrderService {

    @Override
    public Long getMyUserOrderNum() {
        return query().eq("user_id", UserContext.getCurrentUser()).count();
    }

    @Override
    public Result getMyOrders() {
        List<Order> orders = query().eq("user_id", UserContext.getCurrentUser()).list();
        if (orders == null) {return Result.ok("没有相关订单");}
        return Result.ok("查询全部订单成功").put("orders",orders);
    }

    @Autowired
    private IPayRecordService payRecordService;
    @Override
    public Result getOrderById(Long id) {
        Order order = query().eq("id", id).one();
        if (order == null) {return Result.fail("订单不存在");}
        Long orderId = order.getId();
        PayRecord payRecord = payRecordService.queryByOrderId(orderId);
        OrderDetail orderDetail = new OrderDetail();
        BeanUtils.copyProperties(payRecord,orderDetail);
        BeanUtils.copyProperties(order,orderDetail);
        orderDetail.setPayId(payRecord.getId());
        return Result.ok("查询订单详情成功").put("orderDetail",orderDetail);
    }

    @Override
    public List<Long> getMyOrderContent() {
        if(UserContext.getCurrentUser() == null){
            return null;
        }
        return query().select("knowledge_id")
                .eq("user_id", UserContext.getCurrentUser())
                .list().stream().map(Order::getKnowledgeId)
                .collect(Collectors.toList());
    }
    @Autowired
    ContentClient contentClient;
    @Override
    @Transactional
    public Result createOrder(Long id) {
        Long user_id = UserContext.getCurrentUser();
        //Order order = query().eq("knowledge_id", id).eq("user_id",user_id).one();
        Order one = query().eq("user_id", user_id).eq("id", id).one();
        if (one != null&&one.getStatus()==1) {return Result.fail("不能重复下单");}
        Order order = new Order();
        Long orderId=generateRandomId(user_id);
        Order x = query().eq("id", orderId).one();
        if(x!=null){
            if(timeDiff(LocalDateTime.now(),x.getCreateTime())){
                removeById(x.getId());
            }
            else orderId=generateRandomId(user_id);
        }
        order.setId(orderId);
        order.setUserId(user_id);
        order.setKnowledgeId(id);
        order.setOrderNo(generateRandomString(user_id));
        order.setCreateTime(LocalDateTime.now());
        order.setAmount(contentClient.getPriceById(id));
        if(order.getAmount() == null){
            return Result.fail("获取知识价格失败");
        }
        if(order.getAmount().compareTo(BigDecimal.ZERO)==0) {
            order.setStatus(1);
            if(payRecordService.savePayRecord(orderId)) return Result.ok("购买成功");
            return Result.fail("购买失败");
        }
        order.setStatus(0);
        boolean res=save(order);
        if(!res){return Result.fail("订单生成失败");}
        return Result.ok("订单生成成功").put("order",order);
    }

    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    @Override
    @Transactional
    public Result payOrder(Long id) {
        Order order = query().eq("id", id).one();
        if(order==null){
            return Result.fail("订单不存在");
        }
        Long orderId = order.getKnowledgeId();
        Integer status = contentClient.getStatusById(orderId);
        if(status == null||status == 0){
            return Result.fail("不能购买不存在的知识");
        }
        if(status == 2){
            return Result.fail("不能购买已下架的知识");
        }
        if(!timeDiff(LocalDateTime.now(),order.getCreateTime())){
            return Result.fail("订单已过期");
        }
        //TODO 微信开放平台校验
        if(false)return Result.fail("支付失败");
        try {
            update().eq("id", orderId).set("status", 1).update();
            payRecordService.savePayRecord(id);
        }catch (Exception e){
            log.error("更新数据库购买信息失败");
            return Result.fail("请等待运维人员处理");
        }
        redisTemplate.opsForValue().getAndDelete(MY_ORDER_CONTENT+UserContext.getCurrentUser());
        return Result.ok("购买成功");
    }

    private boolean timeDiff(LocalDateTime now, LocalDateTime createTime) {
        // 1. 计算两个时间的分钟差（between参数：开始时间 -> 结束时间，顺序不影响差值大小，仅影响正负）
        long hoursDiff = ChronoUnit.HOURS.between(createTime, now);
        // 2. 取绝对值（避免createTime晚于now导致差值为负）
        long absHoursDiff = Math.abs(hoursDiff);
        // 3. 判断是否小于1小时
        return absHoursDiff < 1;
    }
}
