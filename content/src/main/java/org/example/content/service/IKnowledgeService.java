package org.example.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.common.utils.Result;
import org.example.content.pojo.Knowledge;
import org.example.content.pojo.dto.KnowledgeDTO;

import java.math.BigDecimal;

public interface IKnowledgeService extends IService<Knowledge> {

    Result getMyKnowledge();

    Result postKnowledge(Knowledge knowledge);

    Result getMyOrderContent();

    BigDecimal getPriceById(Long id);

    Integer getStatusById(Long id);

    Result getDetailById(Long id);
}
