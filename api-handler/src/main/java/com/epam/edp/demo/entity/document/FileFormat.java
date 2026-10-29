package com.epam.edp.demo.entity.document;

import lombok.Getter;

@Getter
public enum FileFormat {
    pdf("application/pdf"),
    png("image/png"),
    jpg("image/jpeg"),
    jpeg("image/jpeg"),
    webp("image/webp");

    private final String contentType;

    FileFormat(String contentType){
        this.contentType = contentType;
    }
}
