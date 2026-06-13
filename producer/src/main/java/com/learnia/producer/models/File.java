package com.learnia.producer.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.learnia.models.BaseEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class File extends BaseEntity {

    private String fileName;
    private String s3Path;

    public static File toDomain(String uuidUser ,String uuidRequest, String fileName) {
        File file = new File();
        file.setUuid(UUID.randomUUID());
        file.setCreatedAt(OffsetDateTime.now());
        file.setFileName(fileName);
        file.setS3Path(buildS3Path(uuidUser, uuidRequest, file.getUuid()));
        return file;
    }

    public static File fromPreparedUpload(UUID fileUuid, String fileName, String s3Path) {
        File file = new File();
        file.setUuid(fileUuid);
        file.setCreatedAt(OffsetDateTime.now());
        file.setFileName(fileName);
        file.setS3Path(s3Path);
        return file;
    }

    public String getExtractedTextS3Path() {
        return s3Path.substring(0, s3Path.lastIndexOf('/') + 1) + "extracted.txt";
    }

    private static String buildS3Path(String uuidUser, String uuidRequest, UUID fileUuid) {
        return "requests/" + uuidUser + "/" + uuidRequest + "/" + fileUuid + "/original.pdf";
    }
}
