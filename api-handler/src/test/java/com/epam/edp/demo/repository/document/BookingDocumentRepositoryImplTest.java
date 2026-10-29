package com.epam.edp.demo.repository.document;

import com.epam.edp.demo.entity.document.DocumentStatus;
import com.epam.edp.demo.entity.document.DocumentType;
import com.epam.edp.demo.entity.document.BookingDocument;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingDocumentRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private DocumentRepositoryImpl documentRepository;

    @BeforeEach
    void setUp() {
        documentRepository = new DocumentRepositoryImpl(mongoTemplate);
    }

    @Test
    void save_persistsDocument() {
        BookingDocument input = document("doc-1");

        documentRepository.save(input);

        verify(mongoTemplate).save(input);
    }

    @Test
    void findById_returnsDocument_whenMongoReturnsItem() {
        BookingDocument found = document("doc-1");
        when(mongoTemplate.findById("doc-1", BookingDocument.class)).thenReturn(found);

        Optional<BookingDocument> result = documentRepository.findById("doc-1");

        assertThat(result).contains(found);
        verify(mongoTemplate).findById("doc-1", BookingDocument.class);
    }

    @Test
    void findById_returnsEmpty_whenMongoReturnsNull() {
        when(mongoTemplate.findById("missing", BookingDocument.class)).thenReturn(null);

        assertThat(documentRepository.findById("missing")).isEmpty();
    }

    @Test
    void findByIds_returnsEmptyList_whenInputIdsAreEmpty() {
        List<BookingDocument> result = documentRepository.findByIds(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void findByIds_queriesMongoByDocumentIds() {
        BookingDocument first = document("doc-1");
        BookingDocument second = document("doc-2");
        BookingDocument third = document("doc-3");
        when(mongoTemplate.find(any(Query.class), eq(BookingDocument.class)))
                .thenReturn(List.of(first, second, third));

        List<BookingDocument> found = documentRepository.findByIds(List.of("doc-1", "doc-2", "doc-3"));

        assertThat(found).containsExactly(first, second, third);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(BookingDocument.class));
        Document query = captor.getValue().getQueryObject();
        Document idCriteria = (Document) query.get("_id");
        List<String> ids = ((List<?>) idCriteria.get("$in")).stream()
                .map(String::valueOf)
                .toList();
        assertThat(ids).isEqualTo(List.of("doc-1", "doc-2", "doc-3"));
    }

    @Test
    void delete_removesDocument() {
        BookingDocument input = document("doc-1");

        documentRepository.delete(input);

        verify(mongoTemplate).remove(input);
    }

    private static BookingDocument document(String documentId) {
        return BookingDocument.builder()
                .documentId(documentId)
                .bookingId("booking-1")
                .documentType(DocumentType.PASSPORT)
                .fileName(documentId + ".pdf")
                .s3Key("documents/" + documentId + ".pdf")
                .status(DocumentStatus.UPLOADED)
                .build();
    }


}
