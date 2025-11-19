package org.example.content.pojo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class KnowledgeResourceDTO {

    private MultipartFile file;
    private Long id;
    private Long knowledgeId;
    private Integer resourceType; // 1-视频 2-图片
    private String url;
    private Long size; // 字节数
}
