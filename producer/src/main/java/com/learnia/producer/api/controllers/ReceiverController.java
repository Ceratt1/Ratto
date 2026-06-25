package com.learnia.producer.api.controllers;

import java.util.List;
import java.util.UUID;

import org.hibernate.validator.constraints.Length;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learnia.producer.models.File;
import com.learnia.producer.models.User;
import com.learnia.producer.models.dto.ConfirmDirectUploadRequest;
import com.learnia.producer.models.dto.DirectUploadRequest;
import com.learnia.producer.models.dto.PreparedUploadDto;
import com.learnia.producer.service.IProducerService;
import com.learnia.validation.ValidPdfFiles;

import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

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
            @RequestPart(value = "workspaceId", required = false) String workspaceId,
            @RequestPart(value = "description", required = false) @Length(max = 200) String description,
            @RequestPart(value = "studyLanguage", required = true)
            @Pattern(regexp = "^(en|pt-BR|es)$", message = "studyLanguage must be one of en, pt-BR, es")
            String studyLanguage,
            @ValidPdfFiles(maxSizeMb = 30, maxFiles = 1) @RequestPart("files") List<FilePart> files,
            @AuthenticationPrincipal Jwt jwt) {

        UUID uuidUser = UUID.fromString(jwt.getSubject());
        User user = User.toDomain(
                uuidUser,
                UUID.fromString(uuidRequest),
                workspaceId == null || workspaceId.isBlank() ? null : UUID.fromString(workspaceId),
                description,
                studyLanguage,
                files.stream().map(file -> File.toDomain(uuidUser.toString(), uuidRequest, file.filename())).toList());

        return service.uploadFilesAndSendToTopic(user, files)
                .map(savedUser -> "Received request with uuidRequest: " + savedUser.getUuidRequest()
                        + ", uuidUser: " + savedUser.getUuid()
                        + ", description: " + savedUser.getDescription()
                        + ", and " + savedUser.getFiles().size() + " files.");
    }

    @PostMapping(value = "/{uuidRequest}/uploads", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<PreparedUploadDto> prepareDirectUpload(
            @PathVariable UUID uuidRequest,
            @Valid @RequestBody DirectUploadRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.prepareDirectUpload(UUID.fromString(jwt.getSubject()), uuidRequest, request);
    }

    @PostMapping(value = "/{uuidRequest}/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> confirmDirectUpload(
            @PathVariable UUID uuidRequest,
            @Valid @RequestBody ConfirmDirectUploadRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.confirmDirectUpload(UUID.fromString(jwt.getSubject()), uuidRequest, request)
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).build());
    }
}
