package org.example.content.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class KnowledgeDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Long userId;
    private Integer status; // 0-草稿 1-发布 2-下架
    private LocalDateTime createTime;

    private Long size; // 字节数

    List<KnowledgeResourceDTO> knowledgeResource;
}
