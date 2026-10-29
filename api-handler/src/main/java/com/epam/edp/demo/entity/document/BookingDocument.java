package com.epam.edp.demo.entity.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class BookingDocument {

    @Id
    private String documentId;
    @Indexed
    private String bookingId;
    private DocumentType documentType;
    private String fileName;
    private String s3Key;
    private DocumentStatus status;
}
