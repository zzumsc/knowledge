package org.example.content.clients;

import org.example.common.utils.Result;
import org.example.content.config.FeignJwtInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name="file-service",
            configuration = {FeignJwtInterceptor .class})
public interface FileClient {
    @PostMapping(value = "/file/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result upload(@RequestPart("file") MultipartFile file,
                    @RequestParam(value = "folderName", required = false) String folderName,
                    @RequestParam(value = "aimFileName", required = false) String aimFileName );
    @GetMapping("/file/download")
    Result download(@RequestParam("fileName") String fileName);

    @PostMapping(value = "/file/chunk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result uploadChunk(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunkNum") int chunkNum,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam(value = "folderName", required = false) String folderName,
            @RequestParam(value = "aimFileName", required = false) String aimFileName,
            @RequestPart("chunkFile") MultipartFile chunkFile
    );

    @GetMapping("/file/chunk/query")
    Result queryUploadedChunks(@RequestParam("fileMd5") String fileMd5);

    @PostMapping("/file/chunk/merge")
    Result mergeChunks(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam(value = "folderName", required = false) String folderName,
            @RequestParam(value = "aimFileName", required = false) String aimFileName,
            @RequestParam("originalFileName") String originalFileName
    );
}
