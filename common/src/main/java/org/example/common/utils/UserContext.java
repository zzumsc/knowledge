package org.example.common.utils;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class UserContext {
    public static final ThreadLocal<Long> tl = new ThreadLocal<>();
    public static void setUser(Long userId) {tl.set(userId);}
    public static Long getCurrentUser() {return tl.get();}
    public static void removeUser() {tl.remove();}

//    // 定义Context的key
//    public static final String USER_ID_KEY = "USER_ID";
//
//    // 从Reactor Context中获取userId
//    public static Mono<Long> getCurrentUser() {
//        return Mono.deferContextual(ctx -> {
//            if (ctx.hasKey(USER_ID_KEY)) {
//                return Mono.just(ctx.get(USER_ID_KEY));
//            }
//            return Mono.empty(); // 无userId返回空（后续可处理为未登录）
//        });
//    }
//
//    // 给Mono设置userId到Context
//    public static <T> Mono<T> withUser(Mono<T> mono, Long userId) {
//        return mono.contextWrite(Context.of(USER_ID_KEY, userId));
//    }
}
