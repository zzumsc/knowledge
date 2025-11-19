package org.example.content.controller;

import org.example.content.service.IKnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class ContentController {
    @Autowired
    IKnowledgeService knowledgeService;
    @GetMapping("/order/price/{id}")
    BigDecimal getPriceById(@PathVariable Long id){return knowledgeService.getPriceById(id);};
    @GetMapping("/order/{id}")
    Integer getStatusById(@PathVariable Long id){return knowledgeService.getStatusById(id);};
}
