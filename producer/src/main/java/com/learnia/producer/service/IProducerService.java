package com.learnia.producer.service;

import java.util.List;

import org.springframework.http.codec.multipart.FilePart;

import com.learnia.producer.models.User;

import reactor.core.publisher.Mono;

public interface IProducerService {
    
    User sendToTopic(User user);

    Mono<User> uploadFilesAndSendToTopic(User user, List<FilePart> files);

}
