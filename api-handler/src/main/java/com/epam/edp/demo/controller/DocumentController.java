package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.document.CompleteUploadCheckRequest;
import com.epam.edp.demo.dto.document.GenerateUploadUrlResponseDTO;
import com.epam.edp.demo.dto.document.GenerateUploadUrlsRequestDTO;
import com.epam.edp.demo.dto.document.GenerateUploadUrlsResponseDTO;
import com.epam.edp.demo.service.document.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("bookings/{id}/documents/upload-url")
    @Operation(
            summary = "Generate upload URLs for documents",
            description = "Provides presigned S3 URLs for document upload"
    )
    @ApiResponse(responseCode = "200", description = "Upload URLs returned")
    @ApiResponse(responseCode = "400", description = "Booking not found | Guest not found in booking")
    @ApiResponse(responseCode = "403", description = "Customer can upload for only their bookings | Only customer is allowed to upload")
    @ApiResponse(responseCode = "409", description = "Can upload documents only for bookings in state BOOKED")
    public GenerateUploadUrlsResponseDTO generateUploadUrls(@CurrentUser UserContext user,
                                                            @PathVariable String id,
                                                            @Valid @RequestBody GenerateUploadUrlsRequestDTO request) {
        log.info("Incoming request: POST /bookings/{}/documents/upload-url", id);

        List<GenerateUploadUrlResponseDTO> documents = request.documents().stream()
                .map(documentRequest -> documentService.generateUploadUrl(user, id, documentRequest))
                .toList();

        log.info("Response: 200 OK – doc url(s) for booking {} were generated", id);
        return new GenerateUploadUrlsResponseDTO(documents);
    }

    @PostMapping("/documents/complete")
    @Operation(
            summary = "Check if documents were uploaded successfully",
            description = "Checks if documents were uploaded to S3 successfully"
    )
    @ApiResponse(responseCode = "200", description = "List of documents that failed to upload")
    @ApiResponse(responseCode = "400", description = "Document not found")
    public Map<String, List<String>> completeUpload(@Valid @RequestBody CompleteUploadCheckRequest request) {
        log.info("Incoming request: POST /documents/complete");

        List<String> failedDocNames = new ArrayList<>();

        for (String documentId : request.documentIds()) {
            documentService.verifyDocumentExistsInS3ById(documentId)
                    .ifPresent(failedDocNames::add);
        }

        log.info("Response: 200 OK – document upload completed, failed documents count: {}", failedDocNames.size());
        return Map.of("failedDocNames", failedDocNames);
    }
}
