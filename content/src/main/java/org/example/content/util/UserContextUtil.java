package org.example.content.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class UserContextUtil {
    // 已实现的HTTP场景方法（直接复用）
    public static Long getUserIdFromHttp() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        if (request == null) throw new RuntimeException("非HTTP请求");
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isBlank()) throw new RuntimeException("未携带用户ID");
        return Long.valueOf(userIdStr);
    }

    public static Long getCurrentUserId() {
        try {
            return getUserIdFromHttp(); // Feign调用走HTTP场景
        } catch (Exception e) {
            // 兼容其他场景，无需修改
//            return getUserIdFromDubbo();
            return null;
        }
    }
    // 其他方法（getUserNameFromHttp等）不变
    public static String getUserNameFromHttp() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return request == null ? null : request.getHeader("X-User-Name");
    }

    // 2. Dubbo场景：从RPC上下文获取用户信息
//    public static Long getUserIdFromDubbo() {
//        String userIdStr = RpcContext.getContext().getAttachment("X-User-Id");
//        if (userIdStr == null || userIdStr.isBlank()) {
//            throw new RuntimeException("未携带用户ID");
//        }
//        return Long.valueOf(userIdStr);
//    }

//   public static String getUserNameFromDubbo() {
//        return RpcContext.getContext().getAttachment("X-User-Name");
//    }

    public static String getCurrentUserName() {
        try {
            return getUserNameFromHttp();
        } catch (Exception e) {
//            return getUserNameFromDubbo();
            return null;
        }
    }
}
