package com.epam.edp.demo.controller;

import com.epam.edp.demo.config.WebMvcConfig;
import com.epam.edp.demo.dto.document.GenerateUploadUrlRequestDTO;
import com.epam.edp.demo.dto.document.GenerateUploadUrlResponseDTO;
import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.document.DocumentValidationException;
import com.epam.edp.demo.filter.JwtAuthenticationFilter;
import com.epam.edp.demo.service.document.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class})
class BookingDocumentControllerTest {

    private static RequestPostProcessor authedUser() {
        User user = User.builder()
                .id("user-1")
                .email("jane@example.com")
                .role(Role.CUSTOMER)
                .build();
        CustomUserDetails details = new CustomUserDetails(user);
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
            return request;
        };
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    //---- POST /{id}/documents/upload-url ----

    @Test
    void generateUploadUrls_validRequest_returns200WithDocuments() throws Exception {
        GenerateUploadUrlResponseDTO responseDTO = GenerateUploadUrlResponseDTO.builder()
                .documentId("doc-1")
                .uploadUrl("https:upload.url")
                .build();

        when(documentService.generateUploadUrl(any(UserContext.class), eq("booking-1"), any(GenerateUploadUrlRequestDTO.class)))
                .thenReturn(responseDTO);

        String body = """
                {
                  "documents": [
                    {
                      "fileFormat": "jpeg",
                      "guestFirstName": "John",
                      "guestLastName": "Doe",
                      "documentType": "PASSPORT",
                      "fileSize": 100
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/bookings/booking-1/documents/upload-url")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].documentId").value("doc-1"))
                .andExpect(jsonPath("$.documents[0].uploadUrl").value("https:upload.url"));

        verify(documentService).generateUploadUrl(any(UserContext.class), eq("booking-1"), any());
    }

    @Test
    void generateUploadUrls_multipleDocuments_callsServiceForEach() throws Exception {
        GenerateUploadUrlResponseDTO response1 = GenerateUploadUrlResponseDTO.builder()
                .documentId("doc-1").uploadUrl("https:url1").build();
        GenerateUploadUrlResponseDTO response2 = GenerateUploadUrlResponseDTO.builder()
                .documentId("doc-2").uploadUrl("https:url2").build();

        when(documentService.generateUploadUrl(any(UserContext.class), eq("b1"), any()))
                .thenReturn(response1)
                .thenReturn(response2);

        String body = """
                {
                  "documents": [
                    { "fileFormat": "pdf", "guestFirstName": "John", "guestLastName": "Doe", "documentType": "PASSPORT", "fileSize": 1000 },
                    { "fileFormat": "jpg", "documentType": "PAYMENT", "fileSize": 100 }
                  ]
                }
                """;

        mockMvc.perform(post("/bookings/b1/documents/upload-url")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents.length()").value(2));
    }

    @Test
    void generateUploadUrls_emptyDocumentsList_returns400() throws Exception {
        String body = """
                {
                  "documents": []
                }
                """;

        mockMvc.perform(post("/bookings/booking-1/documents/upload-url")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    @Test
    void generateUploadUrls_nullDocumentsList_returns400() throws Exception {
        String body = """
                {}
                """;

        mockMvc.perform(post("/bookings/booking-1/documents/upload-url")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    @Test
    void generateUploadUrls_multiplePaymentDocuments_returns400() throws Exception {
        String body = """
                {
                  "documents": [
                    { "fileFormat": "pdf", "documentType": "PAYMENT", "fileSize": 3 },
                    { "fileFormat": "jpg", "documentType": "PAYMENT", "fileSize": 2 }
                  ]
                }
                """;

        mockMvc.perform(post("/bookings/booking-1/documents/upload-url")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    @Test
    void generateUploadUrls_serviceThrowsDocumentValidationException_returns400() throws Exception {
        when(documentService.generateUploadUrl(any(UserContext.class), any(), any()))
                .thenThrow(new DocumentValidationException("File format is not correct"));

        String body = """
                {
                  "documents": [
                    { "fileFormat": "exe", "documentType": "PASSPORT", "fileSize": 5 }
                  ]
                }
                """;

        mockMvc.perform(post("/bookings/booking-1/documents/upload-url")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    //---- POST /documents/complete ----

    @Test
    void completeUpload_allDocumentsUploaded_returnsEmptyFailedList() throws Exception {
        when(documentService.verifyDocumentExistsInS3ById(any())).thenReturn(Optional.empty());

        String requestBody = """
                {
                  "documentIds": [
                    "doc-1",
                    "doc-2"
                  ]
                }
                """;

        mockMvc.perform(post("/documents/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failedDocNames").isArray())
                .andExpect(jsonPath("$.failedDocNames.length()").value(0));
    }

    @Test
    void completeUpload_someDocumentsFailed_returnsFailedNames() throws Exception {
        when(documentService.verifyDocumentExistsInS3ById("doc-1")).thenReturn(Optional.empty());
        when(documentService.verifyDocumentExistsInS3ById("doc-2")).thenReturn(Optional.of("PASSPORT John Doe.pdf"));

        String requestBody = """
                {
                  "documentIds": [
                    "doc-1",
                    "doc-2"
                  ]
                }
                """;

        mockMvc.perform(post("/documents/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failedDocNames[0]").value("PASSPORT John Doe.pdf"));
    }

    @Test
    void completeUpload_emptyList_returns400() throws Exception {
        String body = "[]";

        mockMvc.perform(post("/documents/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    @Test
    void completeUpload_tooManyDocuments_returns400() throws Exception {
        List<String> ids = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        String body = objectMapper.writeValueAsString(ids);

        mockMvc.perform(post("/documents/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    @Test
    void completeUpload_serviceThrowsDocumentValidationException_returns400() throws Exception {
        when(documentService.verifyDocumentExistsInS3ById(any()))
                .thenThrow(new DocumentValidationException("Document not found"));

        String body = objectMapper.writeValueAsString(List.of("missing-doc"));

        mockMvc.perform(post("/documents/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
