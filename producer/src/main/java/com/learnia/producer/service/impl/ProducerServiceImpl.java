package com.learnia.producer.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.learnia.producer.models.User;
import com.learnia.producer.models.dto.UserEventDto;
import com.learnia.producer.service.IProducerService;
import com.learnia.tools.aws.model.S3UploadRequest;
import com.learnia.tools.aws.service.S3StorageService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
@Service
public class ProducerServiceImpl implements IProducerService {

    private final KafkaTemplate<String, UserEventDto> kafkaTemplate;
    private final S3StorageService s3StorageService;

    private final String TOPIC = "knowledgement-topic";

    @Autowired
    public ProducerServiceImpl(
            KafkaTemplate<String, UserEventDto> kafkaTemplate,
            S3StorageService s3StorageService) {
        this.kafkaTemplate = kafkaTemplate;
        this.s3StorageService = s3StorageService;
    }

    @Override
    public User sendToTopic(User user) {
        kafkaTemplate.send(TOPIC, user.getUuidRequest().toString(), UserEventDto.from(user));
        return user;
    }

    @Override
    public Mono<User> uploadFilesAndSendToTopic(User user, List<FilePart> files) {
        List<S3UploadRequest> uploadRequests = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            uploadRequests.add(new S3UploadRequest(user.getFiles().get(i).getS3Path(), files.get(i)));
        }

        return s3StorageService.uploadFiles(uploadRequests)
                .then(Mono.fromCallable(() -> sendToTopic(user))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
