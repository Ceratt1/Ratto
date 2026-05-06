package com.learnia.producer.models.dto;

import com.learnia.producer.models.File;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileEventDto {

    private String fileName;
    private String s3Path;

    public static FileEventDto from(File file) {
        return new FileEventDto(file.getFileName(), file.getS3Path());
    }
}
