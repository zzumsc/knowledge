package org.example.order.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

public class RandomId {

    public static Long generateRandomId(Object userId) {
        try {
            long secondTimestamp = System.currentTimeMillis();
            String timePart = String.valueOf(secondTimestamp%100000000);
            timePart = padZero(timePart, 8); // 不足8位补0

            String idStr = Objects.toString(userId);
            String idHashPart = hashTo4Digit(idStr); // 不同ID生成不同4位数字

            SecureRandom random = new SecureRandom();
            String randomPart = String.valueOf(random.nextInt(100)); // 00-99
            randomPart = padZero(randomPart, 2); // 不足2位补0

            return Long.parseLong(timePart + idHashPart + randomPart);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成随机数失败", e);
        }
    }

    private static String hashTo4Digit(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(input.getBytes());
        // 哈希值转为大整数，取模10000得到4位数字
        long hashNum = new java.math.BigInteger(1, hashBytes).mod(java.math.BigInteger.valueOf(10000)).longValue();
        return padZero(String.valueOf(hashNum), 4); // 不足4位补0
    }

    private static String padZero(String str, int length) {
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }

}