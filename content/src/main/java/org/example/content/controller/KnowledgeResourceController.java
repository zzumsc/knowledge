package org.example.content.controller;

import org.example.common.utils.Result;
import org.example.content.pojo.dto.KnowledgeResourceDTO;
import org.example.content.service.IKnowledgeResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resource")
public class KnowledgeResourceController {
    @Autowired
    IKnowledgeResourceService resourceService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result postByKnowledgeId(
            @RequestPart("file") MultipartFile file,
            @RequestParam("knowledgeId") String knowledgeId,
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam(value = "chunkNum", required = false) Integer chunkNum,
            @RequestParam(value = "totalChunks", required = false) Integer totalChunks,
            @RequestParam("fileName") String fileName) {
        KnowledgeResourceDTO dto = new KnowledgeResourceDTO();
        dto.setKnowledgeId(Long.parseLong(knowledgeId));
        dto.setFile(file);
        dto.setFileMd5(fileMd5);
        dto.setChunkNum(chunkNum);
        dto.setTotalChunks(totalChunks);
        dto.setFileName(fileName);
        return resourceService.postByKnowledgeId(dto);
    }
    @GetMapping
    Result downloadByKnowledgeId(@RequestParam("knowledgeId") Long knowledgeId) {
        return resourceService.downloadByKnowledgeId(knowledgeId);
    }
}
