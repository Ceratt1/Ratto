package com.learnia.producer.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.learnia.producer.models.User;
import com.learnia.producer.models.File;
import com.learnia.events.EventIdFactory;
import com.learnia.events.EventMetadata;
import com.learnia.events.EventTopics;
import com.learnia.events.EventTypes;
import com.learnia.events.PdfProcessingEvent;
import com.learnia.events.StudyLanguage;
import com.learnia.producer.models.dto.ConfirmDirectUploadRequest;
import com.learnia.producer.models.dto.ConfirmFileUploadRequest;
import com.learnia.producer.models.dto.DirectUploadFileRequest;
import com.learnia.producer.models.dto.DirectUploadRequest;
import com.learnia.producer.models.dto.PreparedFileUploadDto;
import com.learnia.producer.models.dto.PreparedUploadDto;
import com.learnia.producer.service.IProducerService;
import com.learnia.tools.aws.model.S3UploadRequest;
import com.learnia.tools.aws.service.S3StorageService;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Service
public class ProducerServiceImpl implements IProducerService {

    private final KafkaTemplate<String, PdfProcessingEvent> kafkaTemplate;
    private final S3StorageService s3StorageService;

    @Autowired
    public ProducerServiceImpl(
            KafkaTemplate<String, PdfProcessingEvent> kafkaTemplate,
            S3StorageService s3StorageService) {
        this.kafkaTemplate = kafkaTemplate;
        this.s3StorageService = s3StorageService;
    }

    @Override
    public Mono<User> uploadFilesAndSendToTopic(User user, List<FilePart> files) {
        List<S3UploadRequest> uploadRequests = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            uploadRequests.add(new S3UploadRequest(user.getFiles().get(i).getS3Path(), files.get(i)));
        }

        return s3StorageService.uploadFiles(uploadRequests)
                .then(publishFiles(user))
                .thenReturn(user);
    }

    @Override
    public Mono<PreparedUploadDto> prepareDirectUpload(
            UUID uuidUser,
            UUID uuidRequest,
            DirectUploadRequest request) {
        validateUniqueFileNames(request);
        validateStudyLanguage(request.studyLanguage());

        return Flux.fromIterable(request.files())
                .flatMapSequential(fileRequest -> {
                    File file = File.toDomain(
                            uuidUser.toString(),
                            uuidRequest.toString(),
                            fileRequest.fileName());
                    return s3StorageService.createPresignedUploadUrl(
                                    file.getS3Path(),
                                    fileRequest.resolvedContentType())
                            .map(url -> new PreparedFileUploadDto(
                                    file.getUuid(),
                                    fileRequest.fileName(),
                                    file.getS3Path(),
                                    url,
                                    fileRequest.resolvedContentType()));
                })
                .collectList()
                .map(files -> new PreparedUploadDto(uuidRequest, uuidUser, files));
    }

    @Override
    public Mono<User> confirmDirectUpload(
            UUID uuidUser,
            UUID uuidRequest,
            ConfirmDirectUploadRequest request) {
        validatePreparedFiles(uuidUser, uuidRequest, request);
        validateStudyLanguage(request.studyLanguage());
        User user = toUser(uuidUser, uuidRequest, request);

        return Flux.fromIterable(user.getFiles())
                .flatMap(file -> s3StorageService.objectExists(file.getS3Path())
                        .flatMap(exists -> exists
                                ? Mono.empty()
                                : Mono.error(new IllegalArgumentException(
                                        "File was not uploaded to S3: " + file.getS3Path()))))
                .then(publishFiles(user))
                .thenReturn(user);
    }

    private User toUser(UUID uuidUser, UUID uuidRequest, ConfirmDirectUploadRequest request) {
        List<File> files = request.files().stream()
                .map(file -> File.fromPreparedUpload(file.fileUuid(), file.fileName(), file.s3Path()))
                .toList();
        return User.toDomain(
                uuidUser,
                uuidRequest,
                request.workspaceId(),
                request.description(),
                request.studyLanguage(),
                files);
    }

    private Mono<Void> publishFiles(User user) {
        return Flux.fromIterable(user.getFiles())
                .concatMap(file -> Mono.fromFuture(() -> kafkaTemplate.send(
                        EventTopics.PDF_PROCESSING_REQUESTED,
                        file.getUuid().toString(),
                        toEvent(user, file))))
                .then();
    }

    private PdfProcessingEvent toEvent(User user, File file) {
        return new PdfProcessingEvent(
                new EventMetadata(
                        EventIdFactory.forFile(EventTypes.PDF_PROCESSING_REQUESTED, file.getUuid()),
                        user.getUuidRequest(),
                        null,
                        EventTypes.PDF_PROCESSING_REQUESTED,
                        "producer",
                        "1",
                        OffsetDateTime.now(ZoneOffset.UTC).toString()),
                user.getUuid(),
                user.getUuidRequest(),
                file.getUuid(),
                user.getWorkspaceId(),
                file.getFileName(),
                file.getS3Path(),
                file.getExtractedTextS3Path(),
                user.getDescription(),
                user.getStudyLanguage(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
    }

    private void validateStudyLanguage(String studyLanguage) {
        if (!StudyLanguage.isSupported(studyLanguage)) {
            throw new IllegalArgumentException("studyLanguage must be one of en, pt-BR, es");
        }
    }

    private void validateUniqueFileNames(DirectUploadRequest request) {
        HashSet<String> fileNames = new HashSet<>();
        for (DirectUploadFileRequest file : request.files()) {
            if (!fileNames.add(file.fileName())) {
                throw new IllegalArgumentException("Duplicate fileName: " + file.fileName());
            }
        }
    }

    private void validatePreparedFiles(
            UUID uuidUser,
            UUID uuidRequest,
            ConfirmDirectUploadRequest request) {
        HashSet<UUID> fileUuids = new HashSet<>();
        for (ConfirmFileUploadRequest file : request.files()) {
            String expectedPath = "requests/" + uuidUser + "/" + uuidRequest + "/"
                    + file.fileUuid() + "/original.pdf";
            if (!expectedPath.equals(file.s3Path())) {
                throw new IllegalArgumentException("Invalid S3 path for file: " + file.fileName());
            }
            if (!fileUuids.add(file.fileUuid())) {
                throw new IllegalArgumentException("Duplicate fileUuid: " + file.fileUuid());
            }
        }
    }
}
