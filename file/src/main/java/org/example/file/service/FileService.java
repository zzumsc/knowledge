package org.example.file.service;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.example.file.utils.MinIOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileService {
    @Autowired
    private MinIOUtil minIOUtil;

    private final String tempChunkPath="D://knowledge";

    // ---------------------- 分块处理核心逻辑 ----------------------
    /**
     * 保存分块到本地临时目录
     */
    public boolean saveChunk(String fileMd5, int chunkNum, MultipartFile chunkFile) {
        try {
            long start = System.currentTimeMillis();
            // 按文件 MD5 创建独立目录（避免分块冲突）
            File chunkDir = new File(tempChunkPath + File.separator + fileMd5);
            if (!chunkDir.exists()) {
                chunkDir.mkdirs();
            }
            // 分块文件命名：chunk-{编号}（确保合并时有序）
            File targetChunkFile = new File(chunkDir, "chunk-" + chunkNum);
            chunkFile.transferTo(targetChunkFile);
            long end = System.currentTimeMillis();
            System.out.printf("分块%d 接收+保存耗时：%dms%n", chunkNum, end - start);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 统计已上传分块数
     */
    public int countUploadedChunks(String fileMd5) {
        File chunkDir = new File(tempChunkPath + File.separator + fileMd5);
        if (!chunkDir.exists()) {
            return 0;
        }
        // 过滤出分块文件（避免统计其他无关文件）
        File[] chunkFiles = chunkDir.listFiles((dir, name) -> name.startsWith("chunk-"));
        return chunkFiles == null ? 0 : chunkFiles.length;
    }

    /**
     * 合并分块并上传到 MinIO
     */
    public String mergeChunks(String fileMd5, String folderName, String aimFileName, String originalFileName) {
        try {
            File chunkDir = new File(tempChunkPath + File.separator + fileMd5);
            if (!chunkDir.exists()) {
                return null;
            }

            // 1. 读取所有分块并按编号排序
            File[] chunkFiles = chunkDir.listFiles((dir, name) -> name.startsWith("chunk-"));
            if (chunkFiles == null || chunkFiles.length == 0) {
                return null;
            }
            // 按分块编号升序排列（关键：避免文件合并错乱）
            Arrays.sort(chunkFiles, (f1, f2) -> {
                int num1 = Integer.parseInt(f1.getName().split("-")[1]);
                int num2 = Integer.parseInt(f2.getName().split("-")[1]);
                return num1 - num2;
            });

            // 2. 合并分块为完整文件（临时文件）
            String fileSuffix = originalFileName.substring(originalFileName.lastIndexOf(".")); // 提取文件后缀
            File mergedTempFile = File.createTempFile(fileMd5, fileSuffix);
            try (FileOutputStream outputStream = new FileOutputStream(mergedTempFile)) {
                for (File chunkFile : chunkFiles) {
                    try (FileInputStream inputStream = new FileInputStream(chunkFile)) {
                        byte[] buffer = new byte[1024 * 1024];
                        int len;
                        while ((len = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, len);
                        }
                    }
                }
            }
            String contentType = getContentType(fileSuffix);

            // 3. 封装 MultipartFile 时传入 Content-Type
            MultipartFile mergedFile = new MockMultipartFile(
                    "mergedFile", // 参数名（无关紧要，MinIO 主要看 contentType）
                    originalFileName, // 原始文件名（含后缀）
                    contentType, // 关键：传入非 null 的 Content-Type
                    new FileInputStream(mergedTempFile)
            );
            String fileUrl = minIOUtil.putObject(mergedFile, folderName, aimFileName);

            // 4. 清理临时文件（分块目录+合并后的临时文件）
            FileUtils.deleteDirectory(chunkDir);
            mergedTempFile.delete();

            return fileUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getContentType(String fileSuffix) {
        // 常见文件类型映射（可根据需求扩展）
        Map<String, String> contentTypeMap = new HashMap<>();
        contentTypeMap.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        contentTypeMap.put(".doc", "application/msword");
        contentTypeMap.put(".pdf", "application/pdf");
        contentTypeMap.put(".jpg", "image/jpeg");
        contentTypeMap.put(".png", "image/png");
        contentTypeMap.put(".mp4", "video/mp4");
        contentTypeMap.put(".zip", "application/zip");
        contentTypeMap.put(".txt", "text/plain");

        // 优先根据后缀匹配，无匹配则返回默认二进制流类型
        return contentTypeMap.getOrDefault(fileSuffix.toLowerCase(), "application/octet-stream");
    }
}