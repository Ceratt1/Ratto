package com.learnia.tools.aws.model;

import org.springframework.http.codec.multipart.FilePart;

public record S3UploadRequest(String key, FilePart filePart) {
}
