package org.example.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.common.utils.UserContext;
import org.example.user.dao.InfoDao;
import org.example.user.pojo.User;
import org.example.user.pojo.dto.UserDTO;
import org.example.user.service.IInfoService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static org.example.user.utils.utils.MY_USER_INFO;
import static org.example.user.utils.utils.USER_INFO_TIME;

@Service
public class InfoServiceImpl extends ServiceImpl<InfoDao, User> implements IInfoService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public UserDTO getMyInfo() {
        Long id= UserContext.getCurrentUser();
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        if (principal instanceof UserDetails) {
//            // 认证成功时，principal是UserDetails对象，从中获取用户名
//            id = Long.valueOf(((UserDetails) principal).getUsername());
//        } else {
//            // 未认证或匿名用户，直接转换为String（如"anonymousUser"）
//            return null;
//        }
        User user=(User)redisTemplate.opsForValue().get(MY_USER_INFO+id);
        if(user==null){
            user=query().eq("id", id).one();
            redisTemplate.opsForValue().set(MY_USER_INFO+id, user, USER_INFO_TIME, TimeUnit.MINUTES);
        }
        UserDTO userDTO=new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        userDTO.setId(id);
        return userDTO;
    }
}
