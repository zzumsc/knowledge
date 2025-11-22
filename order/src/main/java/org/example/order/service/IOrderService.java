package org.example.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.common.utils.Result;
import org.example.order.pojo.Order;
import org.example.order.pojo.PayRecord;

import java.util.List;

public interface IOrderService extends IService<Order> {
    Long getMyUserOrderNum();

    Result getMyOrders();

    Result getOrderById(Long id);

    List<Long> getMyOrderContent();

    Result createOrder(Long id);

    Result payOrder(Long id);

    Result createOrderList(List<Long> l);

    Result payOrderList(List<Long> l);
}
