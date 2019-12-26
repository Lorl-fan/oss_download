package com.xian.Utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BaseInfo {
    @Value("${endpoint}")
    public String endpoint;
    @Value("${accessKeyId}")
    public String accessKeyId;
    @Value("${accessKeySecret}")
    public String accessKeySecret;
    @Value("${bucketName}")
    public String bucketName;
    @Value("${localFileDir}")
    public String localFileDir;
    @Value("${ossDownLoadLog}")
    public String ossDownLoadLog;

    @Override
    public String toString() {
        return "BaseInfo{" +
                "endpoint='" + endpoint + '\'' +
                ", accessKeyId='" + accessKeyId + '\'' +
                ", accessKeySecret='" + accessKeySecret + '\'' +
                ", bucketName='" + bucketName + '\'' +
                ", localFileDir='" + localFileDir + '\'' +
                ", ossDownLoadLog='" + ossDownLoadLog + '\'' +
                '}';
    }
}


