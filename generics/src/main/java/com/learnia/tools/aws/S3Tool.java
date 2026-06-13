package com.learnia.tools.aws;

import java.io.InputStream;
import java.time.Duration;

public interface S3Tool {

    void putObject(String bucket, String key, InputStream inputStream, long contentLength, String contentType);

    byte[] getObject(String bucket, String key);

    void deleteObject(String bucket, String key);

    String createPresignedPutUrl(String bucket, String key, String contentType, Duration duration);

    boolean objectExists(String bucket, String key);
}
