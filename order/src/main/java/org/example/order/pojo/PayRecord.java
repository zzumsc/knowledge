package org.example.order.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_pay_record")
public class PayRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String payNo;
    private LocalDateTime payTime;
}
