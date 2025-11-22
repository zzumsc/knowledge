package org.example.content.pojo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class KnowledgeResourceDTO {

    private MultipartFile file;
    private Long id;
    private Long knowledgeId;
    private String FileName; // 原始文件名(含后缀)
    private String url;
    private Long size; // 字节数

    private String fileMd5; // 文件唯一标识
    private Integer chunkNum; // 当前分块编号(从0开始)
    private Integer totalChunks; // 总分块数
}
