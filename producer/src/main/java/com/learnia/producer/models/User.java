package com.learnia.producer.models;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.learnia.models.BaseEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class User extends BaseEntity {
    private UUID uuidRequest;
    private String description;
    private List<File> files;


    public static User toDomain(UUID uuidUser, UUID uuidRequest ,String description, List<File> files) {
        User user = new User();
        user.setUuid(uuidUser);
        user.setUuidRequest(uuidRequest);
        user.setDescription(description);
        user.setFiles(files);
        user.setCreatedAt(OffsetDateTime.now());

        return user;
    }

}
