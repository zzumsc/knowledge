package org.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.user.pojo.User;
import org.example.user.pojo.dto.LoginDTO;
import org.example.user.pojo.dto.Result;


public interface IUserService extends IService<User> {
    Result loginByPassword(LoginDTO loginDTO);

    Result getMyUserInfo();

    Result updateMyUserInfo(User user);
}
