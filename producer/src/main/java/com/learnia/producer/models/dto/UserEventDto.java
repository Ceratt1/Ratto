package com.learnia.producer.models.dto;

import java.util.List;
import java.util.UUID;

import com.learnia.producer.models.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserEventDto {

    private UUID uuid;
    private UUID uuidRequest;
    private String description;
    private List<FileEventDto> files;
    private String createdAt;

    public static UserEventDto from(User user) {
        return new UserEventDto(
            user.getUuid(),
            user.getUuidRequest(),
            user.getDescription(),
            user.getFiles() != null ? user.getFiles().stream().map(FileEventDto::from).toList() : List.of(),
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
