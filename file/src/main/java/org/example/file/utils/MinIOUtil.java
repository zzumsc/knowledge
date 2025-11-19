//package org.example.file.utils;
//
//import io.minio.*;
//import io.minio.http.Method;
//import io.minio.messages.Bucket;
//import io.minio.messages.DeleteError;
//import io.minio.messages.DeleteObject;
//import io.minio.messages.Item;
//import lombok.extern.slf4j.Slf4j;
//import org.example.file.config.MinIOConfig;
//import org.reactivestreams.Publisher;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import org.springframework.web.ErrorResponseException;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.http.codec.multipart.FilePart;
//import reactor.core.CoreSubscriber;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//import java.util.stream.Collectors;
//
//import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
//
//@Slf4j
//@Component
//public class MinIOUtil {
//
//    private final MinioClient minioClient;
//    private final String bucketName;
//    private final boolean secure;
//    private final String endpoint;
//
//    // 构造器注入 Spring 管理的 MinioClient 和配置（无手动 new）
//    public MinIOUtil(MinioClient minioClient, MinIOConfig minIOConfig) {
//        this.minioClient = minioClient;
//        this.bucketName = minIOConfig.getBucketName();
//        this.secure = minIOConfig.isSecure();
//        this.endpoint = minIOConfig.getEndpoint();
//    }
//
//    // ========================== 桶操作（响应式）==========================
//    /**
//     * 查看bucket是否存在
//     */
//    public Mono<Boolean> bucketExists() {
//        return Mono.fromCallable(() -> minioClient.bucketExists(
//                        BucketExistsArgs.builder().bucket(bucketName).build()))
//                .subscribeOn(Schedulers.boundedElastic())
//                .onErrorResume(e -> {
//                    log.error("查看bucket是否存在失败", e);
//                    return Mono.just(false);
//                });
//    }
//
//    /**
//     * 创建存储bucket
//     */
//    public Mono<Boolean> createBucket() {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (exists) return Mono.just(true);
//                    return Mono.fromCallable(() -> {minioClient.makeBucket(
//                                MakeBucketArgs.builder().bucket(bucketName).build());
//                            return true;
//                        })
//                        .subscribeOn(Schedulers.boundedElastic())
//                        .map(voidResult -> true)
//                        .onErrorResume(e -> {
//                            log.error("创建桶失败", e);
//                            return Mono.just(false);
//                        });
//                });
//    }
//
//    /**
//     * 删除存储bucket（需桶内无文件）
//     */
//    public Mono<Boolean> removeBucket() {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return Mono.just(true);
//                    // 先查询桶内是否有文件
//                    return getBucketObjects()
//                            .collectList()
//                            .flatMap(items -> {
//                                if (!items.isEmpty()) return Mono.just(false);
//                                // 无文件则删除桶
//                                return Mono.fromCallable(() -> {
//                                            minioClient.removeBucket(
//                                                    RemoveBucketArgs.builder().bucket(bucketName).build());
//                                            return true;
//                                        })
//                                        .subscribeOn(Schedulers.boundedElastic())
//                                        .map(voidResult -> true)
//                                        .onErrorResume(e -> {
//                                            log.error("删除桶失败", e);
//                                            return Mono.just(false);
//                                        });
//                            });
//                });
//    }
//
//    /**
//     * 获取存储桶策略
//     */
//    public Mono<String> getBucketPolicy() {
//        return Mono.fromCallable(() -> minioClient.getBucketPolicy(
//                        GetBucketPolicyArgs.builder().bucket(bucketName).build()))
//                .subscribeOn(Schedulers.boundedElastic())
//                .onErrorResume(e -> {
//                    log.error("获取存储桶策略失败", e);
//                    return Mono.just(null);
//                });
//    }
//
//    /**
//     * 根据bucketName获取信息
//     */
//    public Mono<Bucket> getBucket() {
//        return Mono.fromCallable(minioClient::listBuckets)
//                .subscribeOn(Schedulers.boundedElastic())
//                .flatMap(buckets -> Mono.justOrEmpty(
//                        buckets.stream()
//                                .filter(b -> b.name().equals(bucketName))
//                                .findFirst()
//                ))
//                .onErrorResume(e -> {
//                    log.error("获取桶信息失败", e);
//                    return Mono.just(null);
//                });
//    }
//
//    /**
//     * 获取全部bucket
//     */
//    public Flux<Bucket> getAllBuckets() {
//        return Mono.fromCallable(minioClient::listBuckets)
//                .subscribeOn(Schedulers.boundedElastic())
//                .flatMapMany(Flux::fromIterable)
//                .onErrorResume(e -> {
//                    log.error("获取所有桶信息失败", e);
//                    return Flux.empty();
//                });
//    }
//
//    /**
//     * 创建文件夹或目录
//     */
//    public Mono<Boolean> createDirectory(String directoryName) {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return createBucket();
//                    return Mono.just(true);
//                })
//                .flatMap(ignore -> Mono.fromCallable(() -> {
//                            minioClient.putObject(PutObjectArgs.builder()
//                                    .bucket(bucketName)
//                                    .object(directoryName)
//                                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
//                                    .build());
//                            return true;
//                        })
//                        .subscribeOn(Schedulers.boundedElastic())
//                        .onErrorResume(e -> {
//                            log.error("创建目录失败", e);
//                            return Mono.just(false);
//                        }));
//    }
//
//    // ========================== 文件操作（响应式）==========================
//    /**
//     * 判断文件是否存在
//     */
//    public Mono<Boolean> objectExist(String objectName) {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return Mono.just(false);
//                    return Mono.fromCallable(() -> minioClient.statObject(
//                                    StatObjectArgs.builder().bucket(bucketName).object(objectName).build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .map(stat -> true)
//                            .onErrorResume(e -> {
//                                log.error("判断文件是否存在失败", e);
//                                return Mono.just(false);
//                            });
//                });
//    }
//
//    /**
//     * 判断文件夹是否存在
//     */
//    public Mono<Boolean> folderExist(String folderName) {
//        String finalfolderName = folderName;
//        String final2folderName = folderName+"/";
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return Mono.just(false);
//                    return Mono.fromCallable(() -> minioClient.listObjects(
//                                    ListObjectsArgs.builder()
//                                            .bucket(bucketName)
//                                            .prefix(finalfolderName)
//                                            .recursive(false)
//                                            .build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMapMany(Flux::fromIterable)
//                            .flatMap(result -> Mono.fromCallable(result::get))
//                            .any(item -> {
//                                return item.isDir() && final2folderName.equals(item.objectName());
//                            })
//                            .onErrorResume(e -> {
//                                log.error("判断文件夹是否存在失败", e);
//                                return Mono.just(false);
//                            });
//                });
//    }
//
//    /**
//     * 文件上传（支持 WebFlux FilePart，响应式）
//     * @param filePart 前端上传文件（WebFlux 原生类型）
//     * @param folderName 目录（可为空，自动处理斜杠）
//     * @param aimFileName 目标文件名（无后缀，可为空，自动生成UUID）
//     * @return 上传成功返回文件临时URL，失败返回null
//     */
//    public Mono<String> putObject(FilePart filePart, String folderName, String aimFileName) {
//        // 1. 响应式校验：文件非空 + 文件名有效（避免null/空字符串）
//        return Mono.justOrEmpty(filePart)
//                .flatMap(fp -> Mono.justOrEmpty(fp.filename())
//                        .filter(StringUtils::hasText)
//                        .switchIfEmpty(Mono.error(new IllegalArgumentException("文件名不能为空")))
//                        .map(filename -> new Object[]{fp, filename})) // 传递filePart和文件名
//                .flatMap(arr -> {
//                    FilePart fp = (FilePart) arr[0];
//                    String originalFilename = (String) arr[1];
//
//                    // 2. 文件名处理（容错：无后缀时默认用空后缀，避免数组越界）
//                    String suffix = originalFilename.lastIndexOf(".") > 0
//                            ? originalFilename.substring(originalFilename.lastIndexOf("."))
//                            : ""; // 无后缀文件（如README）直接用空后缀
//                    String finalFileName = StringUtils.hasText(aimFileName)
//                            ? aimFileName + suffix
//                            : UUID.randomUUID().toString() + suffix;
//
//                    // 3. 路径处理（避免开头/结尾多余斜杠，如 "test-dir/" → "test-dir"）
//                    String finalFolderName = StringUtils.hasText(folderName)
//                            ? folderName.replaceAll("/+$", "").replaceAll("^/+", "")
//                            : "";
//                    String lastFileName = StringUtils.hasText(finalFolderName)
//                            ? finalFolderName + "/" + finalFileName
//                            : finalFileName;
//
//                    // 4. 桶校验：不存在则创建
//                    return bucketExists()
//                            .flatMap(exists -> exists ? Mono.just(true) : createBucket())
//                            .flatMap(ignore -> {
//                                // 5. 响应式流处理：FilePart -> 字节数组（无中间列表，直接join Flux）
//                                Flux<DataBuffer> dataBufferFlux = fp.content().publishOn(Schedulers.boundedElastic());
//
//                                return DataBufferUtils.join(dataBufferFlux)
//                                        .map(buffer -> {
//                                            // DataBuffer → 字节数组（必须释放buffer资源）
//                                            byte[] bytes = new byte[buffer.readableByteCount()];
//                                            buffer.read(bytes);
//                                            DataBufferUtils.release(buffer); // 关键：释放缓冲区，避免内存泄漏
//                                            return bytes;
//                                        })
//                                        .defaultIfEmpty(new byte[0]) // 空文件（0字节）处理
//                                        .flatMap(bytes -> {
//                                            // 6. 字节数组 → InputStream（try-with-resources自动关闭流）
//                                            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
//                                                // 7. MinIO上传（阻塞操作→切换线程池，避免阻塞事件循环）
//                                                return Mono.fromCallable(() -> {
//                                                            // ContentType处理（容错：无ContentType时默认用application/octet-stream）
//                                                            MediaType contentType = fp.headers().getContentType();
//                                                            String contentTypeStr = contentType != null
//                                                                    ? contentType.toString()
//                                                                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;
//
//                                                            // 执行MinIO上传
//                                                            minioClient.putObject(PutObjectArgs.builder()
//                                                                    .bucket(bucketName)
//                                                                    .object(lastFileName)
//                                                                    .stream(inputStream, bytes.length, -1) // 字节长度明确，提升上传性能
//                                                                    .contentType(contentTypeStr)
//                                                                    .build());
//                                                            return lastFileName; // 上传成功返回文件路径
//                                                        })
//                                                        .subscribeOn(Schedulers.boundedElastic())
//                                                        .flatMap(this::getObjectUrl); // 上传成功后获取临时URL
//                                            } catch (IOException e) {
//                                                return Mono.error(new RuntimeException("流转换失败", e));
//                                            }
//                                        });
//                            });
//                })
//                // 6. 全局异常捕获（统一返回null，保持原有逻辑兼容）
//                .onErrorResume(e -> {
//                    log.error("文件上传失败：{}", e.getMessage(), e);
//                    return Mono.just(null);
//                });
//    }
//
//    /**
//     * 文件上传（不指定文件夹）
//     */
//    public Mono<String> putObject(FilePart filePart, String fileName) {
//        return putObject(filePart, null, fileName);
//    }
//
//    /**
//     * 文件上传（自动生成文件名）
//     */
//    public Mono<String> putObject(FilePart filePart) {
//        return putObject(filePart, null, null);
//    }
//
//    /**
//     *  InputStream 上传（兼容非文件流场景）
//     */
//    public Mono<String> putObject(InputStream inputStream, String aimFileName) {
//        if (inputStream == null || !StringUtils.hasText(aimFileName)) return Mono.just(null);
//
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return createBucket();
//                    return Mono.just(true);
//                })
//                .flatMap(ignore -> Mono.fromCallable(() -> {
//                            minioClient.putObject(PutObjectArgs.builder()
//                                    .bucket(bucketName)
//                                    .object(aimFileName)
//                                    .stream(inputStream, inputStream.available(), -1)
//                                    .build());
//                            inputStream.close();
//                            return aimFileName;
//                        })
//                        .subscribeOn(Schedulers.boundedElastic())
//                        .flatMap(this::getObjectUrl)
//                        .onErrorResume(e -> {
//                            log.error("文件上传失败", e);
//                            return Mono.just(null);
//                        }));
//    }
//
//    /**
//     * 拷贝文件
//     */
//    public Mono<Boolean> copyObject(String srcBucketName, String srcObjectName, String objectName) {
//        return Mono.fromCallable(() -> minioClient.copyObject(
//                        CopyObjectArgs.builder()
//                                .source(CopySource.builder().bucket(srcBucketName).object(srcObjectName).build())
//                                .bucket(bucketName)
//                                .object(objectName)
//                                .build()))
//                .subscribeOn(Schedulers.boundedElastic())
//                .map(voidResult -> true)
//                .onErrorResume(e -> {
//                    log.error("拷贝文件失败", e);
//                    return Mono.just(false);
//                });
//    }
//
//    public Mono<Void> getObject(String fileName, ServerHttpResponse response) {
//        // 1. 校验存储桶是否存在
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) {
//                        response.setStatusCode(HttpStatus.NOT_FOUND);
//                        return response.writeWith(Mono.just(
//                                response.bufferFactory().wrap("存储桶不存在".getBytes(StandardCharsets.UTF_8))
//                        ));
//                    }
//
//                    // 2. 获取文件元数据（文件大小）
//                    return Mono.fromCallable(() -> minioClient.statObject(
//                                    StatObjectArgs.builder().bucket(bucketName).object(fileName).build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMap(statResponse -> {
//                                long fileLength = statResponse.size();
//
//                                // 3. 获取MinIO文件流
//                                return Mono.fromCallable(() -> minioClient.getObject(
//                                                GetObjectArgs.builder().bucket(bucketName).object(fileName).build()))
//                                        .subscribeOn(Schedulers.boundedElastic())
//                                        .flatMap(objectResponse -> {
//                                            try (InputStream inputStream = objectResponse) {
//                                                // 4. 处理中文文件名+设置响应头
//                                                String originalFileName = fileName.contains("/")
//                                                        ? fileName.substring(fileName.lastIndexOf("/") + 1)
//                                                        : fileName;
//                                                String encodeFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8.name());
//
//                                                response.setStatusCode(HttpStatus.OK);
//                                                response.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
//                                                response.getHeaders().setContentLength(fileLength);
//                                                response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
//                                                        "attachment;filename*=UTF-8''" + encodeFileName);
//
//                                                // 5. 响应式流处理：直接返回 writeWith(Flux)，无需 flatMap！
//                                                Flux<DataBuffer> dataBufferFlux = DataBufferUtils.readInputStream(
//                                                                () -> inputStream,
//                                                                response.bufferFactory(),
//                                                                8192)
//                                                        .doOnTerminate(() -> {
//                                                            try {
//                                                                objectResponse.close(); // 释放MinIO流资源
//                                                            } catch (Exception e) {
//                                                                log.error("关闭MinIO文件流失败", e);
//                                                            }
//                                                        });
//
//                                                // 关键：直接将 Flux<DataBuffer> 传给 writeWith（类型完全匹配）
//                                                return response.writeWith(dataBufferFlux);
//                                            } catch (Exception e) {
//                                                log.error("文件流处理失败", e);
//                                                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
//                                                return response.writeWith(Mono.just(
//                                                        response.bufferFactory().wrap("文件下载失败".getBytes(StandardCharsets.UTF_8))
//                                                ));
//                                            }
//                                        });
//                            });
//                })
//                // 6. 异常处理：覆盖所有场景
//                .onErrorResume(ErrorResponseException.class, e -> {
//                    if (Objects.requireNonNull(e.getMessage()).contains("NoSuchKey")) {
//                        response.setStatusCode(HttpStatus.NOT_FOUND);
//                        return response.writeWith(Mono.just(
//                                response.bufferFactory().wrap("文件不存在".getBytes(StandardCharsets.UTF_8))
//                        ));
//                    }
//                    log.error("MinIO服务端错误", e);
//                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
//                    return response.writeWith(Mono.just(
//                            response.bufferFactory().wrap("文件下载失败".getBytes(StandardCharsets.UTF_8))
//                    ));
//                })
//                .onErrorResume(e -> {
//                    log.error("文件下载异常", e);
//                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
//                    return response.writeWith(Mono.just(
//                            response.bufferFactory().wrap("服务器异常".getBytes(StandardCharsets.UTF_8))
//                    ));
//                });
//    }
//
//    /**
//     * 以流的形式获取文件对象（响应式）
//     */
//    public Mono<InputStream> getObject(String objectName) {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return Mono.just(null);
//                    return Mono.fromCallable(() -> {
//                                StatObjectResponse stat = minioClient.statObject(
//                                        StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
//                                return stat.size() > 0 ? minioClient.getObject(
//                                        GetObjectArgs.builder().bucket(bucketName).object(objectName).build()) : null;
//                            })
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .onErrorResume(e -> {
//                                log.error("获取文件流失败", e);
//                                return Mono.just(null);
//                            });
//                });
//    }
//
//    /**
//     * 获取文件信息
//     */
//    public Mono<StatObjectResponse> getObjectInfo(String objectName) {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return Mono.just(null);
//                    return Mono.fromCallable(() -> minioClient.statObject(
//                                    StatObjectArgs.builder().bucket(bucketName).object(objectName).build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .onErrorResume(e -> {
//                                log.error("获取文件信息失败", e);
//                                return Mono.just(null);
//                            });
//                });
//    }
//
//    /**
//     * 获取文件临时访问URL（1小时有效）
//     */
//    public Mono<String> getObjectUrl(String fileName) {
//        if (fileName.startsWith("/")) fileName = fileName.substring(1);
//        String finalFileName = fileName;
//        return Mono.fromCallable(() -> {
//                    return minioClient.getPresignedObjectUrl(
//                            GetPresignedObjectUrlArgs.builder()
//                                    .bucket(bucketName)
//                                    .object(finalFileName)
//                                    .method(Method.GET)
//                                    .expiry(60 * 60) // 1小时
//                                    .build());
//                })
//                .subscribeOn(Schedulers.boundedElastic())
//                .onErrorResume(e -> {
//                    log.error("获取文件URL失败", e);
//                    return Mono.just(null);
//                });
//    }
//
//    /**
//     * 断点下载（响应式流）
//     */
//    public Mono<InputStream> getObject(String objectName, long offset, long length) {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return Mono.just(null);
//                    return Mono.fromCallable(() -> minioClient.getObject(
//                                    GetObjectArgs.builder()
//                                            .bucket(bucketName)
//                                            .object(objectName)
//                                            .offset(offset)
//                                            .length(length)
//                                            .build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .onErrorResume(e -> {
//                                e.printStackTrace();
//                                return Mono.just(null);
//                            });
//                });
//    }
//
//    /**
//     * 获取桶内所有文件对象（Flux 流式返回）
//     */
//    public Flux<Item> getBucketObjects() {
//        return bucketExists()
//                .flatMapMany(exists -> {
//                    if (!exists) return Flux.empty();
//                    return Mono.fromCallable(() -> minioClient.listObjects(
//                                    ListObjectsArgs.builder().bucket(bucketName).build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMapMany(Flux::fromIterable)
//                            .flatMap(result -> Mono.fromCallable(result::get))
//                            .onErrorResume(e -> {
//                                log.error("获取桶内文件失败", e);
//                                return Flux.empty();
//                            });
//                });
//    }
//
//    /**
//     * 获取路径下文件列表（流式）
//     */
//    public Flux<Item> getObjects(String prefix, boolean recursive) {
//        return bucketExists()
//                .flatMapMany(exists -> {
//                    if (!exists) return Flux.empty();
//                    return Mono.fromCallable(() -> minioClient.listObjects(
//                                    ListObjectsArgs.builder()
//                                            .bucket(bucketName)
//                                            .prefix(prefix)
//                                            .recursive(recursive)
//                                            .build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMapMany(Flux::fromIterable)
//                            .flatMap(result -> Mono.fromCallable(result::get))
//                            .onErrorResume(e -> {
//                                log.error("获取路径下文件失败", e);
//                                return Flux.empty();
//                            });
//                });
//    }
//
//    /**
//     * 按前缀查询文件列表（流式）
//     */
//    public Flux<Item> getAllObjectsByPrefix(String prefix, boolean recursive) {
//        return bucketExists()
//                .flatMapMany(exists -> {
//                    if (!exists) return Flux.empty();
//                    return Mono.fromCallable(() -> minioClient.listObjects(
//                                    ListObjectsArgs.builder()
//                                            .bucket(bucketName)
//                                            .prefix(prefix)
//                                            .recursive(recursive)
//                                            .build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMapMany(Flux::fromIterable)
//                            .flatMap(result -> Mono.fromCallable(result::get))
//                            .onErrorResume(e -> {
//                                log.error("按前缀查询文件失败", e);
//                                return Flux.empty();
//                            });
//                });
//    }
//
//    /**
//     * 删除单个文件
//     */
//    public Mono<Boolean> removeObject(String fileName) {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return Mono.just(false);
//                    return Mono.fromCallable(() -> {
//                                minioClient.removeObject(
//                                        RemoveObjectArgs.builder().bucket(bucketName).object(fileName).build());
//                                return true;
//                            })
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .map(voidResult -> true)
//                            .onErrorResume(e -> {
//                                log.error("删除文件失败", e);
//                                return Mono.just(false);
//                            });
//                });
//    }
//
//    /**
//     * 批量删除文件（响应式处理）
//     */
//    public Mono<Boolean> removeObjects(String... objects) {
//        return bucketExists()
//                .flatMap(exists -> {
//                    if (!exists) return Mono.just(false);
//                    List<DeleteObject> deleteObjects = Arrays.stream(objects)
//                            .map(DeleteObject::new)
//                            .collect(Collectors.toList());
//
//                    return Mono.fromCallable(() -> minioClient.removeObjects(
//                                    RemoveObjectsArgs.builder()
//                                            .bucket(bucketName)
//                                            .objects(deleteObjects)
//                                            .build()))
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMapMany(Flux::fromIterable)
//                            .flatMap(result -> Mono.fromCallable(result::get))
//                            .doOnNext(error -> log.error("删除文件失败：{}，原因：{}",
//                                    error.objectName(), error.message()))
//                            .then(Mono.just(true)) // 无论单个失败与否，整体返回成功（可根据需求调整）
//                            .onErrorResume(e -> {
//                                log.error("批量删除文件失败", e);
//                                return Mono.just(false);
//                            });
//                });
//    }
//}

package org.example.file.utils;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import com.google.common.io.ByteStreams;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.example.file.config.MinIOConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
public class MinIOUtil {
    private MinioClient minioClient;
    private String bucketName;

    public MinIOUtil(MinioClient minioClient, MinIOConfig minIOConfig) {
        this.minioClient = minioClient; // 注入 Spring 管理的 MinioClient（已配置好参数）
        this.bucketName = minIOConfig.getBucketName(); // 从配置类中获取桶名
    }

    //桶操作

    /**
     * 查看bucket是否存在
     * @return boolean
     */
    public boolean bucketExists() {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("查看bucket是否存在", e);
            return false;
        }
    }

    /**
     * 创建存储bucket
     * @return Boolean
     */
    public boolean createBucket() {
        if (bucketExists()) {
            return true;
        }
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("创建桶失败", e);
            return false;
        }
    }

    /**
     * 删除存储bucket
     *
     * @return boolean
     */
    public boolean removeBucket() {
        if (!bucketExists()) {
            return true;
        }
        //获取桶中所有的对象
        List<Item> items = getBucketObjects();
        if (items.size() > 0) {
            //有对象文件，则删除失败
            return false;
        }
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("根据名称删除桶失败", e);
            return false;
        }
    }

    /**
     * 获取存储桶策略
     *
     * @return json
     */
    public String getBucketPolicy() {
        String bucketPolicy = null;
        try {
            bucketPolicy = minioClient.getBucketPolicy(GetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("获取存储桶策略失败", e);
        }
        return bucketPolicy;
    }

    /**
     * 根据bucketName获取信息
     *
     * return 如果不存在返回null
     */
    public Bucket getBucket() {
        try {
            return minioClient.listBuckets()
                    .stream()
                    .filter(b -> b.name().equals(bucketName))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("根据bucketName获取桶信息", e);
        }
        return null;
    }

    /**
     * 获取全部bucket
     */
    public List<Bucket> getAllBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            log.error("获取所有的桶信息", e);
        }
        return null;
    }

    /**
     * 创建文件夹或目录
     *
     * @param directoryName 目录路径
     */
    public boolean createDirectory(String directoryName) {
        if (!bucketExists()) {
            createBucket();
        }

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(directoryName)
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("创建文件夹或目录失败", e);
            return false;
        }
    }

    // 文件操作

    /**
     * 判断文件是否存在
     *
     * @param objectName 对象
     * @return 存在返回true，不存在发生异常返回false
     */
    public boolean objectExist(String objectName) {
        if (!bucketExists()) {
            return false;
        }
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("判断文件是否存在失败", e);
            return false;
        }
    }

    /**
     * 判断文件夹是否存在【注意是文件夹而不是目录】
     * @param folderName 文件夹名称（去掉前后的/）
     * @return
     */
    public boolean folderExist(String folderName) {
        if (!bucketExists()) {
            return false;
        }
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(folderName)
                    .recursive(false)
                    .build());
            if (results != null) {
                for (Result<Item> result : results) {
                    Item item = result.get();
                    folderName += "/";
                    if (item.isDir() && folderName.equals(item.objectName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("判断文件夹是否存在失败", e);
            return false;
        }
        return false;
    }

    /**
     * 文件上传
     * @param multipartFile 待上传文件
     * @param folderName 目录
     * @param aimFileName 最终保存到minio中的文件名，不需要后缀
     * @return
     */
    public String putObject(MultipartFile multipartFile, String folderName, String aimFileName) {
        if (!bucketExists()) {
            createBucket();
        }

        if (!StringUtils.hasText(aimFileName)) {
            aimFileName = UUID.randomUUID().toString();
        }
        //获取文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        aimFileName += suffix;

        //带路径的文件名
        String lastFileName = "";
        if (StringUtils.hasText(folderName)) {
            lastFileName = "/" + folderName + "/" + aimFileName;
        } else {
            lastFileName = aimFileName;
        }

        try (InputStream inputStream = multipartFile.getInputStream();) {
            //上传文件到指定目录,文件名称相同会覆盖
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(lastFileName)
                    .stream(inputStream, multipartFile.getSize(), -1)
                    .contentType(multipartFile.getContentType())
                    .build());
            return getObjectUrl(lastFileName);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return null;
        }

    }

    /**
     * 上传文件【不指定文件夹】
     * @param multipartFile
     * @param fileName
     * @return
     */
    public String putObject(MultipartFile multipartFile, String fileName) {
        return putObject(multipartFile, null, fileName);
    }

    /**
     * 上传文件【不指定文件夹,不指定目标文件名】
     * @param multipartFile
     * @return
     */
    public String putObject(MultipartFile multipartFile) {
        return putObject(multipartFile, null, null);
    }

    /**
     * 自动创建桶并存储文件
     *
     * @param inputStream
     * @param aimFileName 必须，minio桶中文件的名字，需要带后缀
     * @return
     */
    public String putObject(InputStream inputStream, String aimFileName) {
        if (!bucketExists()) {
            createBucket();
        }
        try {
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(aimFileName)
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);
            inputStream.close();
            return getObjectUrl(aimFileName);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return null;
        }
    }

    /**
     * 拷贝文件
     *
     * @param objectName    文件名称
     * @param srcBucketName 目标bucket名称
     * @param srcObjectName 目标文件名称
     */
    public boolean copyObject(String srcBucketName, String srcObjectName, String objectName) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder()
                                    .bucket(srcBucketName)
                                    .object(srcObjectName)
                                    .build())
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return true;
        } catch (Exception e) {
            log.error("拷贝文件失败", e);
            return false;
        }
    }

    /**
     * 文件下载
     * @param fileName 文件名称
     * @param response response
     * @return Boolean
     */
    public void getObject(String fileName, HttpServletResponse response) {
        if (!bucketExists()) {
            return;
        }
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .build();

        try (ServletOutputStream outputStream = response.getOutputStream();
             GetObjectResponse objectResponse = minioClient.getObject(getObjectArgs)) {

            response.setCharacterEncoding("utf-8");
            //设置强行下载不打开
            //response.setContentType("application/force-download");
            //response.setContentType("APPLICATION/OCTET-STREAM");
            response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
            ByteStreams.copy(objectResponse, outputStream);
            outputStream.flush();
        } catch (Exception e) {
            log.error("文件下载失败", e);
        }
    }

    /**
     * 以流的形式获取一个文件对象
     *
     * @param objectName 对象名称
     * @return {@link InputStream}
     */
    public InputStream getObject(String objectName) {
        if (!bucketExists()) {
            return null;
        }
        try {
            StatObjectResponse statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            if (statObjectResponse.size() > 0) {
                // 获取objectName的输入流。
                return minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
            }
        } catch (Exception e) {
            log.error("文件下载失败", e);
        }
        return null;
    }

    /**
     * 获取文件信息, 如果抛出异常则说明文件不存在
     *
     * @param objectName 文件名称
     */
    public StatObjectResponse getObjectInfo(String objectName) {
        if (!bucketExists()) {
            return null;
        }

        StatObjectResponse statObjectResponse = null;
        try {
            statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("获取文件信息失败", e);
        }
        return statObjectResponse;
    }

    /**
     * 获取图片的路径
     *
     * @param fileName
     * @return
     */
    public String getObjectUrl(String fileName) {
        try {
            if (fileName.startsWith("/")) {
                fileName = fileName.substring(1);
            }
            GetPresignedObjectUrlArgs build = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .method(Method.GET)
                    //过期时间(分钟数)
                    .expiry(60 * 60)
                    .build();
            return minioClient.getPresignedObjectUrl(build);
        } catch (Exception e) {
            log.error("获取文件路径失败", e);
        }
        return null;
    }

    /**
     * 断点下载
     *
     * @param objectName 文件名称
     * @param offset     起始字节的位置
     * @param length     要读取的长度
     * @return 流
     */
    public InputStream getObject(String objectName, long offset, long length) {
        if (!bucketExists()) {
            return null;
        }
        GetObjectResponse objectResponse = null;
        try {
            objectResponse = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .offset(offset)
                    .length(length)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return objectResponse;
    }

    /**
     * 获取指定桶中的所有文件对象
     *
     * @return 存储bucket内文件对象信息
     */
    public List<Item> getBucketObjects() {
        if (!bucketExists()) {
            return null;
        }

        List<Item> items = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .build());
        if (results != null) {
            try {
                for (Result<Item> result : results) {
                    items.add(result.get());
                }
            } catch (Exception e) {
                log.error("获取指定桶中的所有文件对象", e);
            }
        }
        return items;
    }

    /**
     * 获取路径下文件列表
     *
     * @param prefix     路径名称
     * @param recursive  是否递归查找，如果是false,就模拟文件夹结构查找
     * @return 二进制流
     */
    public Iterable<Result<Item>> getObjects(String prefix, boolean recursive) {
        if (!bucketExists()) {
            return null;
        }
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(recursive)
                .build());
        return results;
    }

    /**
     * 根据文件前置查询文件
     *
     * @param prefix     前缀
     * @param recursive  是否递归查询
     * @return MinioItem 列表
     */
    public List<Item> getAllObjectsByPrefix(String prefix, boolean recursive) {
        if (!bucketExists()) {
            return null;
        }
        List<Item> items = new ArrayList<>();
        Iterable<Result<Item>> objectsIterator = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(recursive)
                        .build());
        if (objectsIterator != null) {
            try {
                for (Result<Item> o : objectsIterator) {
                    Item item = o.get();
                    items.add(item);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return items;
    }

    /**
     * 删除文件
     * @param fileName 文件名
     * @return
     * @throws Exception
     */
    public boolean removeObject(String fileName) {
        if (!bucketExists()) {
            return false;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("根据文件删除文件失败", e);
            return false;
        }
    }

    /**
     * 批量删除文件对象【没有测试成功】
     * @param objects 需要删除的文件列表
     */
    public boolean removeObjects(String... objects) {
        if (!bucketExists()) {
            return false;
        }
        List<DeleteObject> deleteObjects = new LinkedList<>();
        Arrays.stream(objects).forEach(s -> {
            deleteObjects.add(new DeleteObject(s));
        });

        Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                .bucket(bucketName)
                .objects(deleteObjects)
                .build());

        //Minio处理批量删除的时候, 采用的延迟执行, 需要通过迭代返回的Iterable<Result<DeleteError>>以执行删除
        if (results != null) {
            try {
                for (Result<DeleteError> result : results) {
                    DeleteError error = result.get();
                    log.error("Error in deleting object " + error.objectName() + "; " + error.message());
                }
            } catch (Exception e) {
                log.error("批量删除文件失败", e);
            }
        }

        return true;
    }

}