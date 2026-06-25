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
    private UUID workspaceId;
    private String description;
    private String studyLanguage;
    private List<File> files;


    public static User toDomain(
            UUID uuidUser,
            UUID uuidRequest,
            UUID workspaceId,
            String description,
            String studyLanguage,
            List<File> files) {
        User user = new User();
        user.setUuid(uuidUser);
        user.setUuidRequest(uuidRequest);
        user.setWorkspaceId(workspaceId);
        user.setDescription(description);
        user.setStudyLanguage(studyLanguage);
        user.setFiles(files);
        user.setCreatedAt(OffsetDateTime.now());

        return user;
    }

}
