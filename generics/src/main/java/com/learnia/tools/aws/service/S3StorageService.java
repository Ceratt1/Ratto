package com.learnia.tools.aws.service;

import java.util.List;

import com.learnia.tools.aws.model.S3UploadRequest;

import reactor.core.publisher.Mono;

public interface S3StorageService {

    Mono<Void> uploadFile(S3UploadRequest request);

    Mono<Void> uploadFiles(List<S3UploadRequest> requests);
}
