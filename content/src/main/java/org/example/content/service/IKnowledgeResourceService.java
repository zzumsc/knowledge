package org.example.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.common.utils.Result;
import org.example.content.pojo.Knowledge;
import org.example.content.pojo.KnowledgeResource;
import org.example.content.pojo.dto.KnowledgeDTO;
import org.example.content.pojo.dto.KnowledgeResourceDTO;
import org.example.content.pojo.vo.KnowledgeVO;

import java.util.List;

public interface IKnowledgeResourceService extends IService<KnowledgeResource> {
    List<KnowledgeResource> getAllByKnowledgeId(Long id);

    Result postByKnowledgeId(KnowledgeResourceDTO knowledge);

    Result downloadByKnowledgeId(Long knowledgeId);
}
