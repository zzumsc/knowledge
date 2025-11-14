package org.example.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.common.utils.UserContext;
import org.example.common.utils.JwtTool;
import org.example.user.clients.OrderClient;
import org.example.user.dao.UserDao;
import org.example.user.pojo.User;
import org.example.user.pojo.dto.LoginDTO;
import org.example.common.utils.Result;
import org.example.user.service.IInfoService;
import org.example.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.example.user.utils.utils.MY_USER_INFO;
import static org.example.user.utils.utils.USER_INFO_TIME;

@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements IUserService {

//    @Resource
//    private AuthenticationManager authenticationManager;

    @Resource
    private RedisTemplate<String, User> redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 注入JWT工具类（核心新增）
    @Resource
    private JwtTool jwtTool;

    @Override
    public Result loginByPassword(LoginDTO loginDTO) {
        User user = query().eq("username", loginDTO.getUsername()).one();
        if (user == null) {
            // 新用户注册
            user = new User();
            user.setUsername(loginDTO.getUsername());
            user.setPassword(passwordEncoder.encode(loginDTO.getPassword())); // 加密密码
            user.setNickname(loginDTO.getUsername());
            user.setCreateTime(LocalDateTime.now());
            save(user);
            // 注册后直接登录（返回JWT令牌）
            return login(user.getId(), loginDTO.getPassword());
        }
        // 密码校验失败
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.fail("密码错误");
        }
        // 老用户登录（返回JWT令牌）
        return login(user.getId(), loginDTO.getPassword());
    }

    @Override
    public Result getMyUserInfo() {
        // 保持不变：从UserContext获取GlobalFilter传递的userId
        Long id = UserContext.getCurrentUser();
        System.out.println(id);
        User user = redisTemplate.opsForValue().get(MY_USER_INFO + id);
        if (user == null) {
            user = query().eq("id", id).one();
            redisTemplate.opsForValue().set(MY_USER_INFO + id, user, USER_INFO_TIME, TimeUnit.MINUTES);
        }
        return Result.ok("用户信息查询成功").put("user", user);
    }

    @Override
    public Result updateMyUserInfo(User user) {
        // 保持不变：校验权限 + 更新Redis缓存
        Long id = UserContext.getCurrentUser();
        if(id==null){
            return Result.fail("未查询到用户信息");
        }
        if (!id.equals(user.getId())) {
            return Result.fail("不能修改其他用户的信息");
        }
        if (query().eq("username", user.getUsername()).ne("id", id).one() != null) {
            return Result.fail("用户名不能重复");
        }
        if (updateById(user)) {
            redisTemplate.delete(MY_USER_INFO + id); // 清理缓存
            return Result.ok("更新成功");
        }
        return Result.fail("更新失败");
    }

    @Autowired
    private OrderClient orderClient;
    @Autowired
    private IInfoService infoService;

    @Override
    public Result getMyUserOrder() {
        Long order_num=orderClient.getMyUserOrderNum();
        return Result.ok("用户和订单数查询成功").put("User",infoService.getMyInfo()).put("order_num", order_num);
    }

    /**
     * 核心修改：生成JWT令牌，替代Session认证
     */
    public Result login(Long userId, String password) {
//        // 1. Spring Security认证（校验用户名/密码合法性，保持原有逻辑）
//        UsernamePasswordAuthenticationToken authRequest =
//                new UsernamePasswordAuthenticationToken(userId.toString(), password);
//        Authentication authenticated = authenticationManager.authenticate(authRequest);
//
//        // 2. 存入SecurityContext（仅用于当前线程的权限控制）
//        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
//        securityContext.setAuthentication(authenticated);
//        SecurityContextHolder.setContext(securityContext);

        // 3. 生成JWT令牌（核心新增：用userId生成令牌）
        String token = jwtTool.generateToken(userId);
        System.out.println(token);

        // 4. 返回令牌给前端（前端需存储token，后续请求携带在Authorization头）
        return Result.ok("登录成功")
                .put("token", token) // 令牌字段（前端存储用）
                .put("expireHours", jwtTool.getExpireHours()); // 过期时间（前端可做刷新提示）
    }
}