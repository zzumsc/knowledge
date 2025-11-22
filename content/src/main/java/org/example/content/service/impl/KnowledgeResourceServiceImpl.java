package org.example.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.xml.bind.DatatypeConverter;
import org.example.common.utils.Result;
import org.example.common.utils.UserContext;
import org.example.content.clients.FileClient;
import org.example.content.clients.OrderClient;
import org.example.content.dao.KnowledgeResourceDao;
import org.example.content.pojo.Knowledge;
import org.example.content.pojo.KnowledgeResource;
import org.example.content.pojo.dto.KnowledgeDTO;
import org.example.content.pojo.dto.KnowledgeResourceDTO;
import org.example.content.pojo.vo.KnowledgeVO;
import org.example.content.service.IKnowledgeResourceService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;
import static org.example.content.util.utils.CHUNK_SIZE;
import static org.example.content.util.utils.LARGE_FILE_THRESHOLD;

@Service
public class KnowledgeResourceServiceImpl extends ServiceImpl<KnowledgeResourceDao, KnowledgeResource> implements IKnowledgeResourceService {
    @Resource
    FileClient fileClient;
    @Override
    public List<KnowledgeResource> getAllByKnowledgeId(Long id) {
        List<KnowledgeResource> kr=query().eq("knowledge_id",id).list();
        return kr;
    }

    //@Qualifier("uploadThreadPool")
    //private ExecutorService UPLOAD_THREAD_POOL;

    // 用于跟踪分块上传状态，避免重复合并（key: fileMd5, value: 已上传分块数）
    private final ConcurrentHashMap<String, AtomicInteger> uploadStatus = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result postByKnowledgeId(KnowledgeResourceDTO knowledge) {
        MultipartFile file = knowledge.getFile();
        String folderName = UserContext.getCurrentUser().toString();
        String originalFileName = knowledge.getFileName();
        String aimFileName;
        int lastDotIndex = originalFileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            aimFileName = originalFileName.substring(0, lastDotIndex);
        } else {
            aimFileName = originalFileName;
        }
        String fileMd5 = knowledge.getFileMd5();
        Integer chunkNum = knowledge.getChunkNum(); // 注意：分块编号建议从0开始，避免索引问题
        Integer totalChunks = knowledge.getTotalChunks();
        long startTotal = System.currentTimeMillis();

        try {
            if (totalChunks==1) {
                // 小文件逻辑不变
                Result uploadResult = fileClient.upload(file, folderName, aimFileName);
                return handleUploadResult(uploadResult, knowledge);
            } else {
                // 初始化分块状态跟踪器
                uploadStatus.putIfAbsent(fileMd5, new AtomicInteger(0));
                AtomicInteger uploadedCount = uploadStatus.get(fileMd5);

                //ServletRequestAttributes mainThreadAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                // 1. 提交当前分块上传任务
                //CompletableFuture<Void> currentChunkFuture = CompletableFuture.runAsync(() -> {
                    int retryCount = 0;
                    try {
                        while (retryCount < 3) {
                //            RequestContextHolder.setRequestAttributes(mainThreadAttributes, true);
                            try {
                                Result chunkResult = fileClient.uploadChunk(
                                        fileMd5, chunkNum, totalChunks, folderName, aimFileName, file
                                );
                                if (chunkResult.getCode() != 0) { // 假设code=0为成功
                                    throw new RuntimeException("分块 " + chunkNum + " 上传失败：" + chunkResult.getMsg());
                                }
                                break;
                            } catch (Exception e) {
                                retryCount++;
                                if (retryCount >= 3) {
                                    throw new CompletionException("分块 " + chunkNum + " 经3次重试仍失败", e);
                                }
                                Thread.sleep(1000L * retryCount);
                            }
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException("分块 " + chunkNum + " 处理失败", ex);
                    } finally {
                //        RequestContextHolder.resetRequestAttributes();
                    }
                //}, UPLOAD_THREAD_POOL);

//将该部分放在这里是便于后端测试上传+合并功能所需时间，正常应该是前端判断上传完后向后端发起另一个请求用于合并

                // 2. 等待当前分块完成，并更新已上传计数
                //currentChunkFuture.join();
                int currentCount = uploadedCount.incrementAndGet();

                // 3. 当所有分块上传完成后，自动触发合并
                if (currentCount == totalChunks) {
                    // 验证分块完整性（双重校验，避免并发问题）
                    Result queryResult = fileClient.queryUploadedChunks(fileMd5);
                    int uploadedChunks = (int) queryResult.getData().get("data");
                    if (uploadedChunks != totalChunks) {
                        // 清除状态，允许重新上传
                        uploadStatus.remove(fileMd5);
                        return Result.fail("分块校验不完整，已上传：" + uploadedChunks + "/" + totalChunks);
                    }

                    // 执行合并
                    Result mergeResult = fileClient.mergeChunks(fileMd5, folderName, aimFileName, originalFileName);
                    long endTotal = System.currentTimeMillis();
                    System.out.println("大文件合并总耗时：" + (endTotal - startTotal) + "ms");

                    // 合并完成后清除状态
                    uploadStatus.remove(fileMd5);
                    return handleUploadResult(mergeResult, knowledge);
                } else {
                    // 未完成所有分块，返回当前进度
                    return Result.ok("分块 " + chunkNum + " 上传成功，进度：" + currentCount + "/" + totalChunks);
                }
            }
        } catch (CompletionException e) {
            // 分块上传失败，清除状态
            uploadStatus.remove(fileMd5);
            return Result.fail("上传失败：" + e.getCause().getMessage());
        } catch (Exception e) {
            uploadStatus.remove(fileMd5);
            e.printStackTrace();
            return Result.fail("上传失败：" + e.getMessage());
        }
    }

    // 保留原有的handleUploadResult方法
    private Result handleUploadResult(Result uploadResult, KnowledgeResourceDTO knowledge) {
        if (uploadResult.getCode() == 0) {
            KnowledgeResource resource = new KnowledgeResource();
            resource.setKnowledgeId(knowledge.getKnowledgeId());
            resource.setUrl((String) uploadResult.getData().get("fileUrl"));
            resource.setFileName(knowledge.getFileName());
            save(resource);
            return Result.ok("文件上传成功").put("resource", resource);
        } else {
            return Result.fail(uploadResult.getMsg());
        }
    }

//    @Autowired
//    @Qualifier("uploadThreadPool")
//    private ExecutorService UPLOAD_THREAD_POOL;
//    @Override
//    public Result postByKnowledgeId(KnowledgeResourceDTO knowledge) {
//        MultipartFile file = knowledge.getFile();
//        String folderName = UserContext.getCurrentUser().toString();
//        String originalFileName = file.getOriginalFilename();
//        String aimFileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
//        long startTotal = System.currentTimeMillis();
//        try {
//            if (file.getSize() <= LARGE_FILE_THRESHOLD) {
//                // 小文件：保持原有串行逻辑
//                Result uploadResult = fileClient.upload(file, folderName, aimFileName);
//                return handleUploadResult(uploadResult, knowledge);
//            } else {
//                ServletRequestAttributes mainThreadAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//                System.out.println(mainThreadAttributes.getRequest());
//                String fileMd5 = calculateFileMd5(file.getInputStream());
//                int totalChunks = (int) Math.ceil((double) file.getSize() / CHUNK_SIZE);
//                // 1. 查询已上传分块（断点续传兼容）
//                Result queryResult = fileClient.queryUploadedChunks(fileMd5);
//                int uploadedChunks = (int) queryResult.getData().get("data");
//
//                // 2. 收集未上传的分块编号
//                List<Integer> unUploadedChunkNums = new ArrayList<>();
//                for (int chunkNum = uploadedChunks; chunkNum < totalChunks; chunkNum++) {
//                    unUploadedChunkNums.add(chunkNum);
//                }
//                if (unUploadedChunkNums.isEmpty()) {
//                    // 所有分块已上传，直接合并
//                    Result mergeResult = fileClient.mergeChunks(fileMd5, folderName, aimFileName, originalFileName);
//                    long endTotal = System.currentTimeMillis();
//                    System.out.println("大文件上传总耗时：" + (endTotal - startTotal) + "ms");
//                    return handleUploadResult(mergeResult, knowledge);
//                }
//
//                // 3. 并发上传未上传的分块（核心修改）
//                List<CompletableFuture<Void>> futures = new ArrayList<>();
//                for (int chunkNum : unUploadedChunkNums) {
//                    // 捕获循环变量（避免线程安全问题）
//                    int finalChunkNum = chunkNum;
//                    // 异步提交分块上传任务
//                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                        int retryCount = 0;
//                        try{
//                            while (retryCount < 3) {
//                                RequestContextHolder.setRequestAttributes(mainThreadAttributes, true);
//                                try {
//                                    // 拆分当前分块（原有 splitChunk 方法不变）
//                                    MultipartFile chunkFile = splitChunk(file, finalChunkNum);
//                                    // 调用 file 服务上传分块（原有逻辑不变）
//                                    Result chunkResult = fileClient.uploadChunk(
//                                            fileMd5, finalChunkNum, totalChunks, folderName, aimFileName, chunkFile
//                                    );
//                                    // 分块上传失败：抛出异常，让 CompletableFuture 捕获
//                                    if (chunkResult.getCode() == 1) {
//                                        throw new RuntimeException("分块 " + finalChunkNum + " 上传失败：" + chunkResult.getMsg());
//                                    }
//                                } catch (Exception e) {
//                                    // 分块上传失败：包装异常，后续统一处理
//                                    throw new CompletionException("分块 " + finalChunkNum + " 上传异常" + retryCount + 1 + "次", e);
//                                }
//                                retryCount++;
//                                try {
//                                    Thread.sleep(1000L * retryCount);
//                                } catch (InterruptedException e) {
//                                    throw new RuntimeException(e);
//                                }
//                            }
//                        }catch(Exception ex){
//                            throw new RuntimeException("分块 " + finalChunkNum + " 经 3 次重试仍失败");
//                        }finally {
//                            RequestContextHolder.resetRequestAttributes();
//                        }
//                    }, UPLOAD_THREAD_POOL); // 使用自定义线程池
//                    futures.add(future);
//                }
//
//                // 4. 等待所有分块上传完成（阻塞，直到所有线程结束）
//                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
////
////                for (int chunkNum : unUploadedChunkNums) {
////                    int retryCount = 0;
////                    boolean uploadSuccess = false;
////                    while (retryCount < 3) {
////                        try {
////                            // 拆分当前分块（原有 splitChunk 方法不变）
////                            MultipartFile chunkFile = splitChunk(file, chunkNum);
////                            // 调用 file 服务上传分块（主线程执行，上下文有效）
////                            Result chunkResult = fileClient.uploadChunk(
////                                    fileMd5, chunkNum, totalChunks, folderName, aimFileName, chunkFile
////                            );
////                            if (chunkResult.getCode() == 1) {
////                                throw new RuntimeException("分块 " + chunkNum + " 上传失败：" + chunkResult.getMsg());
////                            }
////                            uploadSuccess = true;
////                            break; // 上传成功，跳出重试
////                        } catch (Exception e) {
////                            retryCount++;
////                            System.out.println("分块 " + chunkNum + " 上传失败" + retryCount + "次：" + e.getMessage());
////                            if (retryCount >= 3) {
////                                throw new RuntimeException("分块 " + chunkNum + " 经 3 次重试仍失败", e);
////                            }
////                            Thread.sleep(1000L * retryCount); // 重试间隔
////                        }
////                    }
////                    if (!uploadSuccess) {
////                        return Result.fail("分块 " + chunkNum + " 上传失败");
////                    }
////                }
//
//                // 5. 所有分块上传成功，调用合并接口
//                Result mergeResult = fileClient.mergeChunks(fileMd5, folderName, aimFileName, originalFileName);
//                long endTotal = System.currentTimeMillis();
//                System.out.println("大文件上传总耗时：" + (endTotal - startTotal) + "ms");
//                return handleUploadResult(mergeResult, knowledge);
//            }
//        } catch (CompletionException e) {
//            // 捕获分块上传的异常（单个分块失败）
//            return Result.fail("上传失败：" + e.getCause().getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.fail("上传失败：" + e.getMessage());
//        }
//    }
    @Resource
    OrderClient orderClient;
    @Override
    public Result downloadByKnowledgeId(Long knowledgeId) {
        List<Long> l=orderClient.getMyOrderContent();
        if(!l.contains(knowledgeId)){return Result.fail("需要先购买该知识");}
        List<KnowledgeResource> kr=getAllByKnowledgeId(knowledgeId);
        List<Long> ids=new ArrayList<>();
        for (KnowledgeResource i : kr) {
            Result download = fileClient.download(i.getUrl());
            if(download.getCode()!=0){
                ids.add(i.getId());
            }
        }
        if(!ids.isEmpty()){return Result.fail("存在文件下载失败").put("knowledgeId",ids);}
        return Result.ok("下载成功");
    }
    /**
     * 处理上传结果（复用小文件和大文件的数据库保存逻辑）
     */
//    private Result handleUploadResult(Result uploadResult, KnowledgeResourceDTO knowledge) {
//        if (uploadResult.getCode()==0) {
//            String fileUrl = uploadResult.getData().get("fileUrl").toString();
//            KnowledgeResource kr = new KnowledgeResource();
//            BeanUtils.copyProperties(knowledge, kr);
//            kr.setUrl(fileUrl);
//            boolean save = save(kr);
//            return save ? Result.ok("上传成功").put("url", fileUrl) : Result.fail("上传失败：数据库保存失败");
//        }
//        return Result.fail("上传失败：" + uploadResult.getMsg());
//    }

    /**
     * 计算文件 MD5（唯一标识，用于分块关联和断点续传）
     */
    private String calculateFileMd5(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[1024 * 1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            md5.update(buffer, 0, len);
        }
        byte[] digest = md5.digest();
        return DatatypeConverter.printHexBinary(digest).toLowerCase();
    }

    /**
     * 拆分文件为分块
     */
    private MultipartFile splitChunk(MultipartFile sourceFile, int chunkNum) throws IOException {
        long start = chunkNum * CHUNK_SIZE;
        long end = Math.min(start + CHUNK_SIZE, sourceFile.getSize());
        byte[] chunkBytes = new byte[(int) (end - start)];

        // 读取指定区间的字节数据
        try (InputStream inputStream = sourceFile.getInputStream()) {
            inputStream.skip(start);
            inputStream.read(chunkBytes);
        }

        // 封装为 MultipartFile（适配 Feign 传输）
        return new MockMultipartFile(
                "chunkFile", // 分块参数名（需与 file 服务接口一致）
                sourceFile.getName() + ".chunk" + chunkNum, // 分块文件名（仅用于标识）
                sourceFile.getContentType(),
                chunkBytes
        );
    }
}
