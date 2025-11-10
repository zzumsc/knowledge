package org.example.content.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.content.pojo.Knowledge;

@Mapper
public interface KnowledgeDao extends BaseMapper<Knowledge> {
}
