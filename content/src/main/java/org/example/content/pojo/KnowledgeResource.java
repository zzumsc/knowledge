package org.example.content.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_knowledge_resource")
public class KnowledgeResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeId;
    private Integer resourceType; // 1-视频 2-图片
    private String url;
    private Long size; // 字节数
}
