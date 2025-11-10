package org.example.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.order.pojo.Order;

@Mapper
public interface OrderDao extends BaseMapper<Order> {
}
