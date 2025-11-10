package org.example.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.order.dao.OrderDao;
import org.example.order.pojo.Order;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderDao, Order> implements IOrderService {

}
