package org.example.order.pojo.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@lombok.Data
public class OrderDetail {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long knowledgeId;
    private BigDecimal amount;
    private Integer status; // 0-待支付 1-已支付 2-取消
    private LocalDateTime createTime;

    private Long payId;
    private String payNo;
    private LocalDateTime payTime;
}
