package com.example.backend.tools;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * OSS 工具类
 * 时间: 2026-03-28
 */
@Component
public class OssTool {

    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name:}")
    private String bucketName;

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 访问URL
     */
    public String upload(MultipartFile file) throws Exception {
        if (endpoint == null || endpoint.isEmpty()) {
            // 本地测试返回假地址
            return "http://localhost/" + file.getOriginalFilename();
        }
        
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            InputStream inputStream = file.getInputStream();
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + ext;
            
            ossClient.putObject(bucketName, fileName, inputStream);
            
            // https://bucket-name.endpoint/fileName
            String url = "https://" + bucketName + "." + endpoint.replace("https://", "").replace("http://", "") + "/" + fileName;
            return url;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件访问URL
     * 时间: 2026-04-14
     */
    public void delete(String fileUrl) {
        if (endpoint == null || endpoint.isEmpty() || fileUrl == null || fileUrl.isEmpty() || fileUrl.startsWith("http://localhost/")) {
            // 本地测试或者无效URL直接返回
            return;
        }
        
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // 从 URL 中提取文件名
            // 例如: https://bucket-name.endpoint/fileName
            String prefix = "https://" + bucketName + "." + endpoint.replace("https://", "").replace("http://", "") + "/";
            if (fileUrl.startsWith(prefix)) {
                String fileName = fileUrl.substring(prefix.length());
                ossClient.deleteObject(bucketName, fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
