package com.learnia.producer.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.codec.multipart.FilePart;

import com.learnia.producer.models.User;
import com.learnia.producer.models.dto.ConfirmDirectUploadRequest;
import com.learnia.producer.models.dto.DirectUploadRequest;
import com.learnia.producer.models.dto.PreparedUploadDto;

import reactor.core.publisher.Mono;

public interface IProducerService {
    
    Mono<User> uploadFilesAndSendToTopic(User user, List<FilePart> files);

    Mono<PreparedUploadDto> prepareDirectUpload(UUID uuidRequest, DirectUploadRequest request);

    Mono<User> confirmDirectUpload(UUID uuidRequest, ConfirmDirectUploadRequest request);
}
