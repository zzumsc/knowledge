package org.example.content.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 购物车实体类（关联知识主表）
 */
@Data
@TableName("t_shopping_cart")
public class ShoppingCart {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long knowledgeId;
    private Integer quantity = 1;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}