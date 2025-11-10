package org.example.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.content.dao.KnowledgeDao;
import org.example.content.pojo.Knowledge;
import org.example.content.pojo.dto.Result;
import org.example.content.service.IKnowledgeService;
import org.example.content.util.UserContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeDao,Knowledge> implements IKnowledgeService {
    @Autowired
    public RedisTemplate<Long,Object> redisTemplate;
    @Override
    public Result getMyKnowledge() {
        Long id= UserContextUtil.getCurrentUserId();
        List<Knowledge> l= (List<Knowledge>) redisTemplate.opsForValue().get(id);
        if(l==null){
            l=query().eq("userId",id).orderByDesc("createTime").list();
            redisTemplate.opsForValue().set(id, l);
        }
        return Result.ok("查询成功").put("knowledge",l);
    }

    @Override
    public Result postKnowledge(Knowledge knowledge) {
        Boolean b=save(knowledge);
        if(b==true)return Result.ok("发送成功");
        else return Result.fail("发布失败");
    }
}
