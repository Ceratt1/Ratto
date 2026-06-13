package com.learnia.tools.aws.impl;

import java.io.InputStream;
import java.time.Duration;

import com.learnia.tools.aws.S3Tool;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3ToolImpl implements S3Tool {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3ToolImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
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

    @Override
    public String createPresignedPutUrl(String bucket, String key, String contentType, Duration duration) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        return s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .putObjectRequest(putObjectRequest)
                        .build())
                .url()
                .toString();
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }
}
