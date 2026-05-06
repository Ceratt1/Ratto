package com.learnia.producer.api.controllers;

import java.util.List;
import java.util.UUID;

import org.hibernate.validator.constraints.Length;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.learnia.producer.models.File;
import com.learnia.producer.models.User;
import com.learnia.producer.service.IProducerService;
import com.learnia.validation.ValidPdfFiles;

import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("v1/receiver")
public class ReceiverController {

    private final IProducerService service;

    public ReceiverController(IProducerService service) {
        this.service = service;
    }
    
    @PutMapping(value = "/{uuidRequest}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> receive(
            @PathVariable(value = "uuidRequest", required = true) String uuidRequest,
            @RequestPart(value = "uuidUser", required = true) String uuidUser,
            @RequestPart(value = "description", required = false) @Length(max = 200) String description,
            @ValidPdfFiles(maxSizeMb = 100) @RequestPart("files") List<FilePart> files) {

        User user = User.toDomain(
                UUID.fromString(uuidUser),
                UUID.fromString(uuidRequest),
                description,
                files.stream().map(file -> File.toDomain(uuidUser, uuidRequest, file.filename())).toList());

        return service.uploadFilesAndSendToTopic(user, files)
                .map(savedUser -> "Received request with uuidRequest: " + savedUser.getUuidRequest()
                        + ", uuidUser: " + savedUser.getUuid()
                        + ", description: " + savedUser.getDescription()
                        + ", and " + savedUser.getFiles().size() + " files.");
    }
}
