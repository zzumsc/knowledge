package org.example.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.common.utils.Result;
import org.example.common.utils.UserContext;
import org.example.content.clients.OrderClient;
import org.example.content.dao.KnowledgeDao;
import org.example.content.pojo.Knowledge;
import org.example.content.pojo.KnowledgeResource;
import org.example.content.pojo.vo.KnowledgeVO;
import org.example.content.service.IKnowledgeResourceService;
import org.example.content.service.IKnowledgeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.example.content.util.utils.MY_ORDER_CONTENT;
import static org.example.content.util.utils.ORDER_CONTENT_TIME;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeDao,Knowledge> implements IKnowledgeService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public Result getMyKnowledge() {
        Long id= UserContext.getCurrentUser();
        List<Knowledge> l=query().eq("user_id",id).list();
        return Result.ok("查询成功").put("knowledge",l);
    }

    @Override
    public Result postKnowledge(Knowledge knowledge) {
        Boolean b=save(knowledge);
        if(b==true)return Result.ok("发送成功");
        else return Result.fail("发布失败");
    }

    @Autowired
    private OrderClient orderClient;
    @Override
    public Result getMyOrderContent() {
        List<Knowledge>s= (List<Knowledge>) redisTemplate.opsForValue().get(MY_ORDER_CONTENT+UserContext.getCurrentUser());
        if (s == null) {
            List<Long> l= orderClient.getMyOrderContent();
            s=query().in("id",l).list();
            redisTemplate.opsForValue().set(MY_ORDER_CONTENT+UserContext.getCurrentUser(), s, ORDER_CONTENT_TIME, TimeUnit.MINUTES);
        }
        return Result.ok("查询完成").put("orderContent",s);
    }

    @Override
    public BigDecimal getPriceById(Long id) {
        return query().eq("id",id).one().getPrice();
    }

    @Override
    public Integer getStatusById(Long id) {
        return query().eq("id",id).one().getStatus();
    }

    @Autowired
    IKnowledgeResourceService knowledgeResourceService;
    @Override
    public Result getDetailById(Long id) {
        KnowledgeVO vo=new KnowledgeVO();
        KnowledgeResource kr=knowledgeResourceService.getById(id);
        BeanUtils.copyProperties(kr,vo);
        vo.setKnowledgeId(kr.getId());
        BeanUtils.copyProperties(this.getById(id),vo);
        return Result.ok("查询成功").put("knowledge",vo);
    }
}
