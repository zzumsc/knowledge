//package org.example.file.controller;
//
//import io.minio.StatObjectResponse;
//import io.minio.messages.Bucket;
//import io.minio.messages.Item;
//import org.example.common.utils.Result;
//import org.example.file.utils.MinIOUtil;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.http.codec.multipart.FilePart;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
///**
// * MinIO 响应式接口（适配 WebFlux 网关，支持大文件上传/下载）
// * 前缀：/file，保持原有路径不变，前端无感知
// */
//@RestController
//@RequestMapping("/file")
//public class MinIOController {
//
//    private final MinIOUtil minIOUtil;
//
//    // 构造器注入响应式 MinIOUtil（Spring 自动装配）
//    public MinIOController(MinIOUtil minIOUtil) {
//        this.minIOUtil = minIOUtil;
//    }
//
//    // ========================== 桶操作接口（响应式）==========================
//    /**
//     * 查看桶是否存在
//     */
//    @GetMapping("/bucket/exists")
//    public Mono<Result> checkBucketExists() {
//        return minIOUtil.bucketExists()
//                .map(exists -> Result.ok(exists ? "桶已存在" : "桶不存在")
//                        .put("exists", exists))
//                .onErrorResume(e -> Mono.just(Result.fail("查询桶状态失败：" + e.getMessage())));
//    }
//
//    /**
//     * 创建存储桶
//     */
//    @PutMapping("/bucket")
//    public Mono<Result> createBucket() {
//        return minIOUtil.createBucket()
//                .map(success -> success ? Result.ok("桶创建成功").put("success", true)
//                        : Result.fail("桶创建失败").put("success", false))
//                .onErrorResume(e -> Mono.just(Result.fail("创建桶失败：" + e.getMessage())));
//    }
//
//    /**
//     * 删除存储桶（需桶内无文件）
//     */
//    @DeleteMapping("/bucket")
//    public Mono<Result> removeBucket() {
//        return minIOUtil.removeBucket()
//                .map(success -> success ? Result.ok("桶删除成功").put("success", true)
//                        : Result.fail("桶删除失败（可能存在文件）").put("success", false))
//                .onErrorResume(e -> Mono.just(Result.fail("删除桶失败：" + e.getMessage())));
//    }
//
//    /**
//     * 获取桶策略
//     */
//    @GetMapping("/bucket/policy")
//    public Mono<Result> getBucketPolicy() {
//        return minIOUtil.getBucketPolicy()
//                .map(policy -> policy != null ? Result.ok("桶策略获取成功").put("policy", policy)
//                        : Result.fail("桶策略获取失败"))
//                .onErrorResume(e -> Mono.just(Result.fail("获取桶策略失败：" + e.getMessage())));
//    }
//
//    /**
//     * 获取当前配置的桶信息
//     */
//    @GetMapping("/bucket")
//    public Mono<Result> getCurrentBucket() {
//        return minIOUtil.getBucket()
//                .map(bucket -> Result.ok("桶信息获取成功").put("bucket", bucket))
//                .defaultIfEmpty(Result.fail("桶信息获取失败"))
//                .onErrorResume(e -> Mono.just(Result.fail("获取桶信息失败：" + e.getMessage())));
//    }
//
//    /**
//     * 获取所有存储桶
//     */
//    @GetMapping("/buckets")
//    public Mono<Result> getAllBuckets() {
//        return minIOUtil.getAllBuckets()
//                .collectList() // Flux<Bucket> → Mono<List<Bucket>>
//                .map(buckets -> buckets.isEmpty() ? Result.fail("无可用存储桶")
//                        : Result.ok("所有桶信息获取成功").put("buckets", buckets))
//                .onErrorResume(e -> Mono.just(Result.fail("获取所有桶信息失败：" + e.getMessage())));
//    }
//
//    /**
//     * 创建目录（文件夹）
//     * @param directoryName 目录路径（如："test-dir"、"a/b/c"）
//     */
//    @PutMapping("/bucket/directory")
//    public Mono<Result> createDirectory(@RequestParam String directoryName) {
//        return minIOUtil.createDirectory(directoryName)
//                .map(success -> success ? Result.ok("目录创建成功").put("success", true)
//                        : Result.fail("目录创建失败").put("success", false))
//                .onErrorResume(e -> Mono.just(Result.fail("创建目录失败：" + e.getMessage())));
//    }
//
//    // ========================== 文件操作接口（响应式）==========================
//    /**
//     * 判断文件是否存在
//     * @param objectName 文件完整路径（如："test-dir/123.jpg"）
//     */
//    @GetMapping("/exists")
//    public Mono<Result> checkFileExists(@RequestParam String objectName) {
//        return minIOUtil.objectExist(objectName)
//                .map(exists -> Result.ok(exists ? "文件已存在" : "文件不存在")
//                        .put("exists", exists))
//                .onErrorResume(e -> Mono.just(Result.fail("查询文件状态失败：" + e.getMessage())));
//    }
//
//    /**
//     * 判断文件夹是否存在
//     * @param folderName 文件夹名称（不带前后斜杠，如："test-dir"）
//     */
//    @GetMapping("/folder/exists")
//    public Mono<Result> checkFolderExists(@RequestParam String folderName) {
//        return minIOUtil.folderExist(folderName)
//                .map(exists -> Result.ok(exists ? "文件夹已存在" : "文件夹不存在")
//                        .put("exists", exists))
//                .onErrorResume(e -> Mono.just(Result.fail("查询文件夹状态失败：" + e.getMessage())));
//    }
//
//    /**
//     * 文件上传（指定文件夹和目标文件名，无需后缀）
//     * @param file 待上传文件（WebFlux 响应式 FilePart）
//     * @param folderName 目录（可为空，如："test-dir"）
//     * @param aimFileName 目标文件名（无后缀，如："my-file"）
//     */
//    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public Mono<Result> uploadFile(
//            @RequestPart("file") FilePart file, // 注意：@RequestPart 替代 @RequestParam
//            @RequestParam(required = false) String folderName,
//            @RequestParam(required = false) String aimFileName
//    ) {
//        return minIOUtil.putObject(file, folderName, aimFileName)
//                .map(fileUrl -> fileUrl != null ? Result.ok("文件上传成功").put("fileUrl", fileUrl)
//                        : Result.fail("文件上传失败"))
//                .onErrorResume(e -> Mono.just(Result.fail("文件上传失败：" + e.getMessage())));
//    }
//
//    /**
//     * 文件上传（不指定文件夹，仅指定目标文件名）
//     */
//    @PostMapping(value = "/upload/no-folder", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public Mono<Result> uploadFileNoFolder(
//            @RequestPart("file") FilePart file,
//            @RequestParam(required = false) String fileName
//    ) {
//        return minIOUtil.putObject(file, fileName)
//                .map(fileUrl -> fileUrl != null ? Result.ok("文件上传成功").put("fileUrl", fileUrl)
//                        : Result.fail("文件上传失败"))
//                .onErrorResume(e -> Mono.just(Result.fail("文件上传失败：" + e.getMessage())));
//    }
//
//    /**
//     * 文件上传（不指定文件夹和文件名，自动生成UUID文件名）
//     */
//    @PostMapping(value = "/upload/auto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public Mono<Result> uploadFileAuto(@RequestPart("file") FilePart file) {
//        return minIOUtil.putObject(file)
//                .map(fileUrl -> fileUrl != null ? Result.ok("文件上传成功").put("fileUrl", fileUrl)
//                        : Result.fail("文件上传失败"))
//                .onErrorResume(e -> Mono.just(Result.fail("文件上传失败：" + e.getMessage())));
//    }
//
//    /**
//     * 拷贝文件（从其他桶拷贝到当前配置桶）
//     * @param srcBucketName 源桶名称
//     * @param srcObjectName 源文件完整路径
//     * @param targetObjectName 目标文件完整路径（当前桶内）
//     */
//    @PostMapping("/copy")
//    public Mono<Result> copyFile(
//            @RequestParam String srcBucketName,
//            @RequestParam String srcObjectName,
//            @RequestParam String targetObjectName
//    ) {
//        return minIOUtil.copyObject(srcBucketName, srcObjectName, targetObjectName)
//                .map(success -> success ? Result.ok("文件拷贝成功").put("success", true)
//                        : Result.fail("文件拷贝失败").put("success", false))
//                .onErrorResume(e -> Mono.just(Result.fail("文件拷贝失败：" + e.getMessage())));
//    }
//
//    /**
//     * 文件下载（响应式，支持中文文件名、大文件）
//     * @param fileName 文件完整路径（如："test-dir/123.jpg"）
//     * @param response WebFlux 响应式响应对象
//     */
//    @GetMapping("/download")
//    public Mono<Void> downloadFile(
//            @RequestParam String fileName,
//            ServerHttpResponse response // 替代 HttpServletResponse
//    ) {
//        return minIOUtil.getObject(fileName, response)
//                .onErrorResume(e -> {
//                    response.setRawStatusCode(500);
//                    return response.writeWith(Mono.just(
//                            response.bufferFactory().wrap(("文件下载失败：" + e.getMessage()).getBytes())
//                    ));
//                });
//    }
//
//    /**
//     * 获取文件信息（大小、创建时间等）
//     * @param objectName 文件完整路径
//     */
//    @GetMapping("/info")
//    public Mono<Result> getFileInfo(@RequestParam String objectName) {
//        return minIOUtil.getObjectInfo(objectName)
//                .map(fileInfo -> Result.ok("文件信息获取成功").put("fileInfo", fileInfo))
//                .defaultIfEmpty(Result.fail("文件信息获取失败"))
//                .onErrorResume(e -> Mono.just(Result.fail("获取文件信息失败：" + e.getMessage())));
//    }
//
//    /**
//     * 获取文件临时访问URL（有效期1小时）
//     * @param fileName 文件完整路径
//     */
//    @GetMapping("/url")
//    public Mono<Result> getFileUrl(@RequestParam String fileName) {
//        return minIOUtil.getObjectUrl(fileName)
//                .map(fileUrl -> fileUrl != null ? Result.ok("文件URL获取成功（1小时内有效）").put("fileUrl", fileUrl)
//                        : Result.fail("文件URL获取失败"))
//                .onErrorResume(e -> Mono.just(Result.fail("获取文件URL失败：" + e.getMessage())));
//    }
//
//    /**
//     * 获取桶内所有文件列表
//     */
//    @GetMapping("/list")
//    public Mono<Result> getBucketFileList() {
//        return minIOUtil.getBucketObjects()
//                .collectList() // Flux<Item> → Mono<List<Item>>
//                .map(fileList -> fileList.isEmpty() ? Result.ok("桶内无文件").put("fileList", fileList)
//                        : Result.ok("桶内文件列表获取成功").put("fileList", fileList))
//                .onErrorResume(e -> Mono.just(Result.fail("获取文件列表失败：" + e.getMessage())));
//    }
//
//    /**
//     * 根据前缀查询文件列表（支持递归/非递归）
//     * @param prefix 文件前缀（如："test-dir/"）
//     * @param recursive 是否递归查询（true=递归，false=仅当前目录）
//     */
//    @GetMapping("/list/prefix")
//    public Mono<Result> getFileListByPrefix(
//            @RequestParam String prefix,
//            @RequestParam(defaultValue = "false") boolean recursive
//    ) {
//        return minIOUtil.getAllObjectsByPrefix(prefix, recursive)
//                .collectList() // Flux<Item> → Mono<List<Item>>
//                .map(fileList -> fileList.isEmpty() ? Result.ok("无匹配文件").put("fileList", fileList)
//                        : Result.ok("前缀查询文件列表成功").put("fileList", fileList))
//                .onErrorResume(e -> Mono.just(Result.fail("前缀查询文件列表失败：" + e.getMessage())));
//    }
//
//    /**
//     * 删除单个文件
//     * @param fileName 文件完整路径
//     */
//    @DeleteMapping
//    public Mono<Result> deleteFile(@RequestParam String fileName) {
//        return minIOUtil.removeObject(fileName)
//                .map(success -> success ? Result.ok("文件删除成功").put("success", true)
//                        : Result.fail("文件删除失败").put("success", false))
//                .onErrorResume(e -> Mono.just(Result.fail("删除文件失败：" + e.getMessage())));
//    }
//
//    /**
//     * 批量删除文件
//     * @param fileNames 文件完整路径数组（如：["test-dir/123.jpg", "test-dir/456.png"]）
//     */
//    @DeleteMapping("/batch")
//    public Mono<Result> batchDeleteFile(@RequestParam String... fileNames) {
//        return minIOUtil.removeObjects(fileNames)
//                .map(success -> success ? Result.ok("批量文件删除成功").put("success", true)
//                        : Result.fail("批量文件删除失败").put("success", false))
//                .onErrorResume(e -> Mono.just(Result.fail("批量删除文件失败：" + e.getMessage())));
//    }
//}

package org.example.file.controller;

import io.minio.StatObjectResponse;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import org.example.common.utils.Result;
import org.example.file.service.FileService;
import org.example.file.utils.MinIOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * MinIO 操作接口（适配 Result 统一返回规范）
 * 前缀：/api/minio，分模块管理桶/文件操作
 */
@RestController
@RequestMapping("/file")
public class MinIOController {

    private final MinIOUtil minIOUtil;

    // 构造器注入工具类（Spring 自动装配）
    public MinIOController(MinIOUtil minIOUtil) {
        this.minIOUtil = minIOUtil;
    }

    // ========================== 桶操作接口 ==========================
    /**
     * 查看桶是否存在
     */
    @GetMapping("/bucket/exists")
    public Result checkBucketExists() {
        boolean exists = minIOUtil.bucketExists();
        return Result.ok(exists ? "桶已存在" : "桶不存在")
                .put("exists", exists);
    }

    /**
     * 创建存储桶
     */
    @PutMapping("/bucket")
    public Result createBucket() {
        boolean success = minIOUtil.createBucket();
        return success ? Result.ok("桶创建成功").put("success", true)
                : Result.fail("桶创建失败").put("success", false);
    }

    /**
     * 删除存储桶（需桶内无文件）
     */
    @DeleteMapping("/bucket")
    public Result removeBucket() {
        boolean success = minIOUtil.removeBucket();
        return success ? Result.ok("桶删除成功").put("success", true)
                : Result.fail("桶删除失败（可能存在文件）").put("success", false);
    }

    /**
     * 获取桶策略
     */
    @GetMapping("/bucket/policy")
    public Result getBucketPolicy() {
        String policy = minIOUtil.getBucketPolicy();
        return policy != null ? Result.ok("桶策略获取成功").put("policy", policy)
                : Result.fail("桶策略获取失败");
    }

    /**
     * 获取当前配置的桶信息
     */
    @GetMapping("/bucket")
    public Result getCurrentBucket() {
        Bucket bucket = minIOUtil.getBucket();
        return bucket != null ? Result.ok("桶信息获取成功").put("bucket", bucket)
                : Result.fail("桶信息获取失败");
    }

    /**
     * 获取所有存储桶
     */
    @GetMapping("/buckets")
    public Result getAllBuckets() {
        List<Bucket> buckets = minIOUtil.getAllBuckets();
        return buckets != null ? Result.ok("所有桶信息获取成功").put("buckets", buckets)
                : Result.fail("所有桶信息获取失败");
    }

    /**
     * 创建目录（文件夹）
     * @param directoryName 目录路径（如："test-dir"、"a/b/c"）
     */
    @PutMapping("/bucket/directory")
    public Result createDirectory(@RequestParam String directoryName) {
        boolean success = minIOUtil.createDirectory(directoryName);
        return success ? Result.ok("目录创建成功").put("success", true)
                : Result.fail("目录创建失败").put("success", false);
    }

    // ========================== 文件操作接口 ==========================
    /**
     * 判断文件是否存在
     * @param objectName 文件完整路径（如："test-dir/123.jpg"）
     */
    @GetMapping("/exists")
    public Result checkFileExists(@RequestParam String objectName) {
        boolean exists = minIOUtil.objectExist(objectName);
        return Result.ok(exists ? "文件已存在" : "文件不存在")
                .put("exists", exists);
    }

    /**
     * 判断文件夹是否存在
     * @param folderName 文件夹名称（不带前后斜杠，如："test-dir"）
     */
    @GetMapping("/folder/exists")
    public Result checkFolderExists(@RequestParam String folderName) {
        boolean exists = minIOUtil.folderExist(folderName);
        return Result.ok(exists ? "文件夹已存在" : "文件夹不存在")
                .put("exists", exists);
    }

    /**
     * 文件上传（指定文件夹和目标文件名，无需后缀）
     * @param file 待上传文件
     * @param folderName 目录（可为空，如："test-dir"）
     * @param aimFileName 目标文件名（无后缀，如："my-file"）
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false) String aimFileName
    ) {
        String fileUrl = minIOUtil.putObject(file, folderName, aimFileName);
        return fileUrl != null ? Result.ok("文件上传成功").put("fileUrl", fileUrl)
                : Result.fail("文件上传失败");
    }

    /**
     * 文件上传（不指定文件夹，仅指定目标文件名）
     */
    @PostMapping(value = "/upload/no-folder", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadFileNoFolder(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String fileName
    ) {
        String fileUrl = minIOUtil.putObject(file, fileName);
        return fileUrl != null ? Result.ok("文件上传成功").put("fileUrl", fileUrl)
                : Result.fail("文件上传失败");
    }

    /**
     * 文件上传（不指定文件夹和文件名，自动生成UUID文件名）
     */
    @PostMapping(value = "/upload/auto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadFileAuto(@RequestParam("file") MultipartFile file) {
        String fileUrl = minIOUtil.putObject(file);
        return fileUrl != null ? Result.ok("文件上传成功").put("fileUrl", fileUrl)
                : Result.fail("文件上传失败");
    }

    /**
     * 拷贝文件（从其他桶拷贝到当前配置桶）
     * @param srcBucketName 源桶名称
     * @param srcObjectName 源文件完整路径
     * @param targetObjectName 目标文件完整路径（当前桶内）
     */
    @PostMapping("/copy")
    public Result copyFile(
            @RequestParam String srcBucketName,
            @RequestParam String srcObjectName,
            @RequestParam String targetObjectName
    ) {
        boolean success = minIOUtil.copyObject(srcBucketName, srcObjectName, targetObjectName);
        return success ? Result.ok("文件拷贝成功").put("success", true)
                : Result.fail("文件拷贝失败").put("success", false);
    }

    /**
     * 文件下载（浏览器下载，支持中文文件名）
     * @param fileName 文件完整路径（如："test-dir/123.jpg"）
     * @param response 响应对象（输出文件流）
     */
    @GetMapping("/download")
    public void downloadFile(@RequestParam String fileName, HttpServletResponse response) {
        minIOUtil.getObject(fileName, response);
    }

    /**
     * 获取文件信息（大小、创建时间等）
     * @param objectName 文件完整路径
     */
    @GetMapping("/info")
    public Result getFileInfo(@RequestParam String objectName) {
        StatObjectResponse fileInfo = minIOUtil.getObjectInfo(objectName);
        return fileInfo != null ? Result.ok("文件信息获取成功").put("fileInfo", fileInfo)
                : Result.fail("文件信息获取失败");
    }

    /**
     * 获取文件临时访问URL（有效期1小时）
     * @param fileName 文件完整路径
     */
    @GetMapping("/url")
    public Result getFileUrl(@RequestParam String fileName) {
        String fileUrl = minIOUtil.getObjectUrl(fileName);
        return fileUrl != null ? Result.ok("文件URL获取成功（1小时内有效）").put("fileUrl", fileUrl)
                : Result.fail("文件URL获取失败");
    }

    /**
     * 获取桶内所有文件列表
     */
    @GetMapping("/list")
    public Result getBucketFileList() {
        List<Item> fileList = minIOUtil.getBucketObjects();
        return fileList != null ? Result.ok("桶内文件列表获取成功").put("fileList", fileList)
                : Result.fail("桶内文件列表获取失败");
    }

    /**
     * 根据前缀查询文件列表（支持递归/非递归）
     * @param prefix 文件前缀（如："test-dir/"）
     * @param recursive 是否递归查询（true=递归，false=仅当前目录）
     */
    @GetMapping("/list/prefix")
    public Result getFileListByPrefix(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "false") boolean recursive
    ) {
        List<Item> fileList = minIOUtil.getAllObjectsByPrefix(prefix, recursive);
        return fileList != null ? Result.ok("前缀查询文件列表成功").put("fileList", fileList)
                : Result.fail("前缀查询文件列表失败");
    }

    /**
     * 删除单个文件
     * @param fileName 文件完整路径
     */
    @DeleteMapping
    public Result deleteFile(@RequestParam String fileName) {
        boolean success = minIOUtil.removeObject(fileName);
        return success ? Result.ok("文件删除成功").put("success", true)
                : Result.fail("文件删除失败").put("success", false);
    }

    /**
     * 批量删除文件
     * @param fileNames 文件完整路径数组（如：["test-dir/123.jpg", "test-dir/456.png"]）
     */
    @DeleteMapping("/batch")
    public Result batchDeleteFile(@RequestParam String... fileNames) {
        boolean success = minIOUtil.removeObjects(fileNames);
        return success ? Result.ok("批量文件删除成功").put("success", true)
                : Result.fail("批量文件删除失败").put("success", false);
    }

    @Autowired
    private FileService fileService;
    @PostMapping(value = "/chunk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadChunk(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunkNum") int chunkNum,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam(value = "folderName", required = false) String folderName,
            @RequestParam(value = "aimFileName", required = false) String aimFileName,
            @RequestPart("chunkFile") MultipartFile chunkFile
    ) {
        return fileService.saveChunk(fileMd5, chunkNum, chunkFile)
                ? Result.ok("分块上传成功")
                : Result.fail("分块上传失败");
    }

    @GetMapping("/chunk/query")
    public Result queryUploadedChunks(
            @RequestParam("fileMd5") String fileMd5) {
        int uploadedChunks = fileService.countUploadedChunks(fileMd5);
        return Result.ok("查询成功").put("data", uploadedChunks);
    }

    @PostMapping("/chunk/merge")
    public Result mergeChunks(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam(value = "folderName", required = false) String folderName,
            @RequestParam(value = "aimFileName", required = false) String aimFileName,
            @RequestParam("originalFileName") String originalFileName
    ) {
        String fileUrl = fileService.mergeChunks(fileMd5, folderName, aimFileName, originalFileName);
        return fileUrl != null ? Result.ok("文件合并成功").put("fileUrl", fileUrl)
                : Result.fail("文件合并失败");
    }
}
