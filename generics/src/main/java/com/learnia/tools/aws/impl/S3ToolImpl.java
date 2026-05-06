package com.learnia.tools.aws.impl;

import java.io.InputStream;

import com.learnia.tools.aws.S3Tool;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ToolImpl implements S3Tool {

    private final S3Client s3Client;

    public S3ToolImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void putObject(String bucket, String key, InputStream inputStream, long contentLength, String contentType) {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (contentType != null && !contentType.isBlank()) {
            requestBuilder.contentType(contentType);
        }

        s3Client.putObject(
                requestBuilder.build(),
                RequestBody.fromInputStream(inputStream, contentLength));
    }

    @Override
    public byte[] getObject(String bucket, String key) {
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());

        return response.asByteArray();
    }

    @Override
    public void deleteObject(String bucket, String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
