package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.review.ReviewListResponseDTO;
import com.epam.edp.demo.dto.review.ReviewResponseDTO;
import com.epam.edp.demo.entity.review.Review;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewMapperTest {

    @Test
    void toReviewResponse_passesFieldsThrough() {
        Review review = review("Jane Doe", "https://img/jane.jpg", "2026-06-01T10:00:00Z", 5, "Great tour");

        ReviewResponseDTO dto = ReviewMapper.toReviewResponse(review);

        assertThat(dto.authorName()).isEqualTo("Jane Doe");
        assertThat(dto.authorImageUrl()).isEqualTo("https://img/jane.jpg");
        assertThat(dto.createdAt()).isEqualTo("2026-06-01T10:00:00Z");
        assertThat(dto.rate()).isEqualTo(5);
        assertThat(dto.reviewContent()).isEqualTo("Great tour");
    }

    @Test
    void toReviewResponse_allowsNullOptionalFields() {
        Review review = review("Jane Doe", null, "2026-06-01T10:00:00Z", 4, null);

        ReviewResponseDTO dto = ReviewMapper.toReviewResponse(review);

        assertThat(dto.authorImageUrl()).isNull();
        assertThat(dto.reviewContent()).isNull();
    }

    @Test
    void toReviewListResponse_computesTotalPagesWithCeilingDivision() {
        List<Review> reviews = List.of(
                review("A", null, "t1", 5, "c1"),
                review("B", null, "t2", 4, "c2"));

        ReviewListResponseDTO response = ReviewMapper.toReviewListResponse(reviews, 1, 2, 5);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(2);
        assertThat(response.totalItems()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.reviews()).hasSize(2);
    }

    @Test
    void toReviewListResponse_totalPagesIsZeroWhenPageSizeIsZero() {
        ReviewListResponseDTO response = ReviewMapper.toReviewListResponse(List.of(), 1, 0, 10);

        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.reviews()).isEmpty();
    }

    @Test
    void toReviewListResponse_totalPagesIsZeroWhenTotalItemsIsZero() {
        ReviewListResponseDTO response = ReviewMapper.toReviewListResponse(List.of(), 1, 4, 0);

        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.totalItems()).isEqualTo(0);
    }

    @Test
    void toReviewListResponse_totalPagesIsExactWhenItemsDivideEvenly() {
        ReviewListResponseDTO response = ReviewMapper.toReviewListResponse(List.of(), 1, 4, 8);

        assertThat(response.totalPages()).isEqualTo(2);
    }

    @Test
    void toUpdateResponse_keepsExistingContentWhenBodyContentIsNull() {
        Review existing = review("Jane", null, "2026-06-01T10:00:00Z", 3, "old content");

        ReviewResponseDTO dto = ReviewMapper.toUpdateResponse(existing, 5, null);

        assertThat(dto.rate()).isEqualTo(5);
        assertThat(dto.reviewContent()).isEqualTo("old content");
    }

    @Test
    void toUpdateResponse_clearsContentWhenBodyContentIsBlank() {
        Review existing = review("Jane", null, "2026-06-01T10:00:00Z", 5, "old content");

        ReviewResponseDTO dto = ReviewMapper.toUpdateResponse(existing, 5, "   ");

        assertThat(dto.reviewContent()).isNull();
    }

    @Test
    void toUpdateResponse_replacesContentWhenBodyContentIsPresent() {
        Review existing = review("Jane", null, "2026-06-01T10:00:00Z", 5, "old content");

        ReviewResponseDTO dto = ReviewMapper.toUpdateResponse(existing, 4, "new content");

        assertThat(dto.rate()).isEqualTo(4);
        assertThat(dto.reviewContent()).isEqualTo("new content");
    }

    private static Review review(String authorName, String authorImageUrl, String createdAt,
                                  Integer rate, String content) {
        Review review = new Review();
        review.setAuthorName(authorName);
        review.setAuthorImageUrl(authorImageUrl);
        review.setCreatedAt(createdAt);
        review.setRate(rate);
        review.setReviewContent(content);
        return review;
    }
}
