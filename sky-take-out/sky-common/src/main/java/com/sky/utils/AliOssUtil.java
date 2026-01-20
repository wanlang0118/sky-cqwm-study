package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 阿里云OSS工具类
 * 用于实现文件上传到阿里云对象存储服务
 */
@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    // OSS服务的访问域名
    private String endpoint;
    // 访问身份验证中用到的AccessKeyId
    private String accessKeyId;
    // 访问身份验证中用到的AccessKeySecret
    private String accessKeySecret;
    // OSS的存储空间名称
    private String bucketName;

    /**
     * 文件上传到阿里云OSS
     *
     * @param bytes 文件的字节数组
     * @param objectName 文件在OSS中的存储路径和名称
     * @return 文件的访问URL
     */
    public String upload(byte[] bytes, String objectName) {

        // 获取当前日期,格式为yyyy/MM/dd
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String datePath = now.format(formatter);

        // 构建带日期路径的文件名: yyyy/MM/dd/原文件名
        String filePathWithDate = datePath + "/" + objectName;

        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求,上传文件到OSS(使用带日期路径的文件名)
            ossClient.putObject(bucketName, filePathWithDate, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            // 捕获OSS服务端异常
            System.out.println("捕获到OSS异常,请求已到达OSS服务器,但因某些原因被拒绝");
            System.out.println("错误信息:" + oe.getErrorMessage());
            System.out.println("错误代码:" + oe.getErrorCode());
            System.out.println("请求ID:" + oe.getRequestId());
            System.out.println("主机ID:" + oe.getHostId());
        } catch (ClientException ce) {
            // 捕获客户端异常
            System.out.println("捕获到客户端异常,客户端在与OSS通信时遇到严重的内部问题,例如无法访问网络");
            System.out.println("错误信息:" + ce.getMessage());
        } finally {
            // 关闭OSSClient,释放资源
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 文件访问路径规则: https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(filePathWithDate);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}
