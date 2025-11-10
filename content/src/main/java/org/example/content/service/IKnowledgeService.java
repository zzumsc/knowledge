package org.example.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.content.pojo.Knowledge;
import org.example.content.pojo.dto.Result;

public interface IKnowledgeService extends IService<Knowledge> {

    Result getMyKnowledge();

    Result postKnowledge(Knowledge knowledge);
}
