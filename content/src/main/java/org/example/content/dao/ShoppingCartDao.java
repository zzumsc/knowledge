package org.example.content.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.content.pojo.ShoppingCart;

@Mapper
public interface ShoppingCartDao extends BaseMapper<ShoppingCart> {
}
