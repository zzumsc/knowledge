package org.example.content.util;

public class utils {
    public static final String MY_ORDER_CONTENT = "order:content:";

    public static final String MY_SHOPPING_CART_CONTENT = "shopping_cart:content:";

    public static final Long ORDER_CONTENT_TIME = 30L;

    public static final Long SHOPPING_CART_CONTENT_TIME = 30L;
    // 大文件阈值：超过 10MB 则分块上传（可根据需求调整）
    public static final long LARGE_FILE_THRESHOLD = 10 * 1024 * 1024; // 10MB
    // 分块大小：5MB/块（平衡请求数和传输效率）
    public static final long CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
}
