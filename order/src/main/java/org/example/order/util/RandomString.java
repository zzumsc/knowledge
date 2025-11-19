package org.example.order.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

public class RandomString {
    public static String generateRandomString(Object userId) {
        try {
            // 1. 核心要素：毫秒时间戳（确保时序唯一）+ 用户ID（区分不同主体）+ 安全随机数（兜底去重）
            long timestamp = System.currentTimeMillis(); // 毫秒级时间戳，重复概率极低
            String idStr = Objects.toString(userId); // 统一ID为字符串，兼容多类型
            int randomNum = new SecureRandom().nextInt(1000000); // 6位安全随机数，进一步降低重复可能

            // 2. 拼接要素（顺序不影响，仅为混合信息）
            String mixedData = timestamp + "_" + idStr + "_" + randomNum;

            // 3. SHA-1哈希（简单不可逆，结果固定40位，避免原始信息泄露）
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = sha1.digest(mixedData.getBytes());

            // 4. 转为16进制字符串（直观易存储，无乱码）
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexBuilder.append('0');
                hexBuilder.append(hex);
            }
            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成随机数失败", e); // 简化异常处理，直接抛出运行时异常
        }
    }
}
