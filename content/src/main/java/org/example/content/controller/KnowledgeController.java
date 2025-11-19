package org.example.content.controller;

import jakarta.annotation.Resource;
import org.example.content.pojo.Knowledge;
import org.example.common.utils.Result;
import org.example.content.pojo.dto.KnowledgeDTO;
import org.example.content.pojo.vo.KnowledgeVO;
import org.example.content.service.IKnowledgeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/content")
public class KnowledgeController {
    @Resource
    private IKnowledgeService knowledgeService;

    @GetMapping
    public Result knowledge() {
        return knowledgeService.getMyKnowledge();
    }

    @PostMapping
    public Result postKnowledge(@RequestBody Knowledge knowledge) {
        return knowledgeService.postKnowledge(knowledge);
    }

    @GetMapping("/order")
    public Result getMyOrderContent() {
        return knowledgeService.getMyOrderContent();
    }

    @GetMapping("/{id}")
    public Result getDetailById(@PathVariable Long id) {
        return knowledgeService.getDetailById(id);
    }
}
