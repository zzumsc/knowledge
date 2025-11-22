package org.example.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.common.utils.Result;
import org.example.content.pojo.ShoppingCart;

import java.util.List;

public interface IShoppingCartService extends IService<ShoppingCart> {
    Result createShoppingCart(ShoppingCart shoppingCart);

    Result getShoppingCart();

    Result removeShoppingCart(List<Long> l);
}
