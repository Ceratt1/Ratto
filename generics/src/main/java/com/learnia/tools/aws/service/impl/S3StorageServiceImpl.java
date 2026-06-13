package com.learnia.tools.aws.service.impl;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;

import org.springframework.core.io.buffer.DataBufferUtils;

import com.learnia.tools.aws.S3Tool;
import com.learnia.tools.aws.model.S3UploadRequest;
import com.learnia.tools.aws.properties.S3Properties;
import com.learnia.tools.aws.service.S3StorageService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class S3StorageServiceImpl implements S3StorageService {

    private final S3Tool s3Tool;
    private final S3Properties s3Properties;

    public S3StorageServiceImpl(S3Tool s3Tool, S3Properties s3Properties) {
        this.s3Tool = s3Tool;
        this.s3Properties = s3Properties;
    }

    @Override
    public Mono<Void> uploadFile(S3UploadRequest request) {
        validateBucketConfiguration();

        return DataBufferUtils.join(request.filePart().content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> Mono.fromRunnable(() -> s3Tool.putObject(
                                s3Properties.getBucket(),
                                request.key(),
                                new ByteArrayInputStream(bytes),
                                bytes.length,
                                request.filePart().headers().getContentType() != null
                                        ? request.filePart().headers().getContentType().toString()
                                        : "application/pdf"))
                        .subscribeOn(Schedulers.boundedElastic())
                        .then());
    }

    @Override
    public Mono<Void> uploadFiles(List<S3UploadRequest> requests) {
        return Flux.fromIterable(requests)
                .flatMap(this::uploadFile, 3)
                .then();
    }

    @Override
    public Mono<String> createPresignedUploadUrl(String key, String contentType) {
        validateBucketConfiguration();
        return Mono.fromCallable(() -> s3Tool.createPresignedPutUrl(
                        s3Properties.getBucket(),
                        key,
                        contentType,
                        Duration.ofMinutes(s3Properties.getPresignedUrlDurationMinutes())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> objectExists(String key) {
        validateBucketConfiguration();
        return Mono.fromCallable(() -> s3Tool.objectExists(s3Properties.getBucket(), key))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<byte[]> downloadFile(String key) {
        validateBucketConfiguration();
        return Mono.fromCallable(() -> s3Tool.getObject(s3Properties.getBucket(), key))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> uploadBytes(String key, byte[] content, String contentType) {
        validateBucketConfiguration();
        return Mono.fromRunnable(() -> s3Tool.putObject(
                        s3Properties.getBucket(),
                        key,
                        new ByteArrayInputStream(content),
                        content.length,
                        contentType))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void validateBucketConfiguration() {
        if (s3Properties.getBucket() == null || s3Properties.getBucket().isBlank()) {
            throw new IllegalStateException("Property aws.s3.bucket must be configured");
        }
    }
}
