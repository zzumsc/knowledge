package org.example.content.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_knowledge")
public class Knowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Long userId;
    private Integer status; // 0-草稿 1-发布 2-下架
    private LocalDateTime createTime;
}
