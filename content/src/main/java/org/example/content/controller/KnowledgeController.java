package org.example.content.controller;

import jakarta.annotation.Resource;
import org.example.content.pojo.Knowledge;
import org.example.content.pojo.dto.Result;
import org.example.content.service.IKnowledgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/Knowledge")
public class KnowledgeController {
    @Resource
    private IKnowledgeService knowledgeService;
    @GetMapping
    public Result knowledge() {
        return knowledgeService.getMyKnowledge();
    }
    @PostMapping
    public Result addKnowledge(@RequestBody Knowledge knowledge) {
        return knowledgeService.postKnowledge(knowledge);
    }
}
