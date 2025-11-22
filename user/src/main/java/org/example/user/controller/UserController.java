package org.example.user.controller;

import jakarta.annotation.Resource;
import org.example.user.pojo.User;
import org.example.user.pojo.dto.LoginDTO;
import org.example.common.utils.Result;
import org.example.user.pojo.dto.UserDTO;
import org.example.user.service.IInfoService;
import org.example.user.service.IUserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private IUserService userService;
    @Resource
    private IInfoService infoService;

    @GetMapping("/info")
    public Result info() {
        return userService.getMyUserInfo();
    }

    @GetMapping("/myInfo")
    public UserDTO myInfo() {return infoService.getMyInfo();}

    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {
        return userService.loginByPassword(loginDTO);
    }
    @PostMapping("/info")
    public Result info(@RequestBody User user) {
        return userService.updateMyUserInfo(user);
    }

    @GetMapping("/order")
    public Result order() {return userService.getMyUserOrder();}

    @PostMapping("/logout")
    public Result logout() {return userService.logout();}
}
