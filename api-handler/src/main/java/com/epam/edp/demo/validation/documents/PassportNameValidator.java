package com.epam.edp.demo.validation.documents;

import com.epam.edp.demo.dto.document.GenerateUploadUrlRequestDTO;
import com.epam.edp.demo.entity.document.DocumentType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PassportNameValidator implements ConstraintValidator<ValidDocumentHolder, GenerateUploadUrlRequestDTO> {

    @Override
    public boolean isValid(GenerateUploadUrlRequestDTO request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        if (request.documentType() != DocumentType.PASSPORT) {
            return true;
        }

        return request.guestFirstName() != null
                && !request.guestFirstName().isBlank()
                && request.guestLastName() != null
                && !request.guestLastName().isBlank();
    }
}
