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
        file.setFileName(file.getUuid() + "-" + fileName);
        file.setS3Path(buildS3Path(uuidUser ,uuidRequest, fileName));
        return file;
    }

    private static String buildS3Path(String uuidUser ,String uuidRequest, String fileName) {
        return "requests/" + uuidUser + "/" + uuidRequest + "/" + fileName;
    }
}
