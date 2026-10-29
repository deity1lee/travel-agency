package com.epam.edp.demo.validation.documents;

import com.epam.edp.demo.dto.document.GenerateUploadUrlRequestDTO;
import com.epam.edp.demo.dto.document.GenerateUploadUrlsRequestDTO;
import com.epam.edp.demo.entity.document.DocumentType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DocumentsValidator
        implements ConstraintValidator<ValidDocuments, GenerateUploadUrlsRequestDTO> {

    private String message;

    @Override
    public void initialize(ValidDocuments annotation) {
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(GenerateUploadUrlsRequestDTO request, ConstraintValidatorContext context) {

        if (request == null || request.documents() == null) {
            return true;
        }

        boolean paymentFound = false;

        for (GenerateUploadUrlRequestDTO documentRequest : request.documents()) {
            if (DocumentType.PAYMENT == documentRequest.documentType()) {
                if (paymentFound) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(message)
                            .addPropertyNode("documents")
                            .addConstraintViolation();

                    return false;
                }
                paymentFound = true;
            }
        }

        return true;
    }
}
