package org.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.user.pojo.User;
import org.example.user.pojo.dto.UserDTO;

public interface IInfoService extends IService<User> {
    UserDTO getMyInfo();
}
