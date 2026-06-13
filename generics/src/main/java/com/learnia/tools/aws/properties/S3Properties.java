package com.learnia.tools.aws.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

    private String bucket;
    private String region;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccessEnabled;
    private long presignedUrlDurationMinutes = 15;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isPathStyleAccessEnabled() {
        return pathStyleAccessEnabled;
    }

    public void setPathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
        this.pathStyleAccessEnabled = pathStyleAccessEnabled;
    }

    public long getPresignedUrlDurationMinutes() {
        return presignedUrlDurationMinutes;
    }

    public void setPresignedUrlDurationMinutes(long presignedUrlDurationMinutes) {
        this.presignedUrlDurationMinutes = presignedUrlDurationMinutes;
    }
}
