package org.example.content.controller;

import org.example.common.utils.Result;
import org.example.content.pojo.ShoppingCart;
import org.example.content.service.IShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shop")
public class ShoppingCartController {
    @Autowired
    private IShoppingCartService shoppingCartService;
    @PostMapping
    public Result createShoppingCart(@RequestBody ShoppingCart shoppingCart) {return shoppingCartService.createShoppingCart(shoppingCart);}
    @GetMapping
    public Result getShoppingCart() {return shoppingCartService.getShoppingCart();}
    @DeleteMapping
    public Result removeShoppingCart(@RequestBody List<Long> l){return shoppingCartService.removeShoppingCart(l);}

}
