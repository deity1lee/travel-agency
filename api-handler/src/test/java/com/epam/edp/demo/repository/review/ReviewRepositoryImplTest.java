package com.epam.edp.demo.repository.review;

import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.entity.tourModel.ReviewSortBy;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.exception.review.DuplicateReviewException;
import com.epam.edp.demo.exception.review.OptimisticLockException;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;

    private ReviewRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ReviewRepositoryImpl(mongoTemplate, transactionTemplate);
        // Run the transactional callback inline so we can assert the operations inside it.
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void findById_matchesReviewIdAndTourId() {
        Review review = review("tour-1", "rev-1", 5);
        when(mongoTemplate.findOne(any(Query.class), eq(Review.class))).thenReturn(review);

        Optional<Review> result = repository.findById("tour-1", "rev-1");

        assertThat(result).contains(review);
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findOne(captor.capture(), eq(Review.class));
        Document filter = captor.getValue().getQueryObject();
        assertThat(filter.get("_id")).isEqualTo("rev-1");
        assertThat(filter.get("tourId")).isEqualTo("tour-1");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(mongoTemplate.findOne(any(Query.class), eq(Review.class))).thenReturn(null);

        assertThat(repository.findById("tour-1", "missing")).isEmpty();
    }

    @Test
    void getReviewsByTourId_ratingDesc_sortsByRateDescendingWithPaging() {
        when(mongoTemplate.find(any(Query.class), eq(Review.class)))
                .thenReturn(List.of(review("tour-1", "rev-1", 5)));

        List<Review> result = repository.getReviewsByTourId("tour-1", 2, 4, ReviewSortBy.RATING_DESC);

        assertThat(result).hasSize(1);
        Query query = captureFind();
        assertThat(query.getQueryObject().get("tourId")).isEqualTo("tour-1");
        assertThat(query.getSortObject()).isEqualTo(new Document("rate", -1).append("_id", 1));
        assertThat(query.getSkip()).isEqualTo(4L);
        assertThat(query.getLimit()).isEqualTo(4);
    }

    @Test
    void getReviewsByTourId_newest_sortsByCreatedAtDescending() {
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of());

        repository.getReviewsByTourId("tour-1", 1, 10, ReviewSortBy.NEWEST);

        assertThat(captureFind().getSortObject()).isEqualTo(new Document("createdAt", -1).append("_id", 1));
    }

    @Test
    void getReviewsByTourId_oldest_sortsByCreatedAtAscending() {
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of());

        repository.getReviewsByTourId("tour-1", 1, 10, ReviewSortBy.OLDEST);

        assertThat(captureFind().getSortObject()).isEqualTo(new Document("createdAt", 1).append("_id", 1));
    }

    @Test
    void getReviewsByTourId_ratingAsc_sortsByRateAscending() {
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of());

        repository.getReviewsByTourId("tour-1", 1, 10, ReviewSortBy.RATING_ASC);

        assertThat(captureFind().getSortObject()).isEqualTo(new Document("rate", 1).append("_id", 1));
    }

    @Test
    void getReviewsByUserId_filtersByUserId() {
        when(mongoTemplate.find(any(Query.class), eq(Review.class)))
                .thenReturn(List.of(review("tour-1", "rev-1", 5)));

        List<Review> result = repository.getReviewsByUserId("user-1");

        assertThat(result).hasSize(1);
        assertThat(captureFind().getQueryObject().get("userId")).isEqualTo("user-1");
    }

    @Test
    void getRatingAggregates_readsFromTour() {
        Tour tour = new Tour();
        tour.setRatingSum(20L);
        tour.setReviews(5);
        tour.setRating(4.0);
        when(mongoTemplate.findOne(any(Query.class), eq(Tour.class))).thenReturn(tour);

        RatingAggregates aggregates = repository.getRatingAggregates("tour-1");

        assertThat(aggregates.ratingSum()).isEqualTo(20L);
        assertThat(aggregates.reviews()).isEqualTo(5);
        assertThat(aggregates.rating()).isEqualTo(4.0);
    }

    @Test
    void getRatingAggregates_returnsZerosWhenTourMissing() {
        when(mongoTemplate.findOne(any(Query.class), eq(Tour.class))).thenReturn(null);

        RatingAggregates aggregates = repository.getRatingAggregates("tour-1");

        assertThat(aggregates).isEqualTo(new RatingAggregates(0L, 0, 0.0));
    }

    @Test
    void getRatingAggregates_treatsNullFieldsAsZero() {
        when(mongoTemplate.findOne(any(Query.class), eq(Tour.class))).thenReturn(new Tour());

        RatingAggregates aggregates = repository.getRatingAggregates("tour-1");

        assertThat(aggregates).isEqualTo(new RatingAggregates(0L, 0, 0.0));
    }

    @Test
    void getReviewCountForTour_returnsReviewCount() {
        Tour tour = new Tour();
        tour.setReviews(7);
        when(mongoTemplate.findOne(any(Query.class), eq(Tour.class))).thenReturn(tour);

        assertThat(repository.getReviewCountForTour("tour-1")).isEqualTo(7);
    }

    @Test
    void getReviewCountForTour_returnsZeroWhenMissing() {
        when(mongoTemplate.findOne(any(Query.class), eq(Tour.class))).thenReturn(null);

        assertThat(repository.getReviewCountForTour("tour-1")).isEqualTo(0);
    }

    @Test
    void createReview_insertsReviewAndUpdatesAggregateInTransaction() {
        Review review = review("tour-1", "rev-1", 5);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Tour.class)))
                .thenReturn(matched(1));

        repository.createReview(review, new RatingAggregateUpdate(20L, 5, 25L, 6, 4.16));

        verify(transactionTemplate).executeWithoutResult(any());
        verify(mongoTemplate).insert(review);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(Tour.class));

        Document filter = queryCaptor.getValue().getQueryObject();
        assertThat(filter.get("_id")).isEqualTo("tour-1");
        assertThat(filter.get("ratingSum")).isEqualTo(20L);
        assertThat(filter.get("reviews")).isEqualTo(5);
        Document set = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
        assertThat(set.get("ratingSum")).isEqualTo(25L);
        assertThat(set.get("reviews")).isEqualTo(6);
        assertThat(set.get("rating")).isEqualTo(4.16);
    }

    @Test
    void createReview_throwsDuplicateReviewWhenIdExists() {
        Review review = review("tour-1", "rev-1", 5);
        doThrow(new DuplicateKeyException("dup")).when(mongoTemplate).insert(review);

        assertThatThrownBy(() -> repository.createReview(review, new RatingAggregateUpdate(20L, 5, 25L, 6, 4.16)))
                .isInstanceOf(DuplicateReviewException.class);
    }

    @Test
    void createReview_throwsOptimisticLockWhenAggregateChanged() {
        Review review = review("tour-1", "rev-1", 5);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Tour.class)))
                .thenReturn(matched(0));

        assertThatThrownBy(() -> repository.createReview(review, new RatingAggregateUpdate(20L, 5, 25L, 6, 4.16)))
                .isInstanceOf(OptimisticLockException.class);
    }

    @Test
    void saveAll_bulkInsertsReviews_withoutTouchingRatingAggregates() {
        List<Review> reviews = List.of(review("tour-1", "rev-1", 5), review("tour-2", "rev-2", 4));

        repository.saveAll(reviews);

        verify(mongoTemplate).insert(reviews, Review.class);
        // plain bulk insert: no aggregate recompute and no transaction
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Tour.class));
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    @Test
    void saveAll_withEmptyList_doesNotInsert() {
        repository.saveAll(List.of());

        verify(mongoTemplate, never()).insert(anyList(), eq(Review.class));
    }

    @Test
    void updateReview_contentOnly_updatesReviewWithoutTransaction() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Review.class)))
                .thenReturn(matched(1));

        repository.updateReview("tour-1", "rev-1", 4, null, "new content", "2026-06-16", null);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(Review.class));

        Document filter = queryCaptor.getValue().getQueryObject();
        assertThat(filter.get("_id")).isEqualTo("rev-1");
        assertThat(filter.get("tourId")).isEqualTo("tour-1");
        assertThat(filter.get("rate")).isEqualTo(4);
        Document set = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
        assertThat(set.get("updatedAt")).isEqualTo("2026-06-16");
        assertThat(set.get("reviewContent")).isEqualTo("new content");
    }

    @Test
    void updateReview_contentOnly_blankContentUnsetsField() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Review.class)))
                .thenReturn(matched(1));

        repository.updateReview("tour-1", "rev-1", 4, null, "  ", "2026-06-16", null);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(Review.class));
        Document unset = (Document) updateCaptor.getValue().getUpdateObject().get("$unset");
        assertThat(unset.get("reviewContent")).isEqualTo(1);
    }

    @Test
    void updateReview_contentOnly_throwsOptimisticLockWhenRateChanged() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Review.class)))
                .thenReturn(matched(0));

        assertThatThrownBy(() -> repository.updateReview("tour-1", "rev-1", 4, null, "x", "2026-06-16", null))
                .isInstanceOf(OptimisticLockException.class);
    }

    @Test
    void updateReview_fullUpdate_updatesRateAndAggregateInTransaction() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Review.class)))
                .thenReturn(matched(1));
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Tour.class)))
                .thenReturn(matched(1));

        repository.updateReview("tour-1", "rev-1", 3, 5, "great", "2026-06-16",
                new RatingAggregateUpdate(20L, 5, 22L, 5, 4.4));

        verify(transactionTemplate).executeWithoutResult(any());
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Review.class));
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Tour.class));
    }

    @Test
    void updateReview_fullUpdate_throwsOptimisticLockWhenReviewRateChanged() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Review.class)))
                .thenReturn(matched(0));

        assertThatThrownBy(() -> repository.updateReview("tour-1", "rev-1", 3, 5, "great", "2026-06-16",
                new RatingAggregateUpdate(20L, 5, 22L, 5, 4.4)))
                .isInstanceOf(OptimisticLockException.class);
    }

    @Test
    void updateReview_fullUpdate_throwsOptimisticLockWhenAggregateChanged() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Review.class)))
                .thenReturn(matched(1));
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Tour.class)))
                .thenReturn(matched(0));

        assertThatThrownBy(() -> repository.updateReview("tour-1", "rev-1", 3, 5, "great", "2026-06-16",
                new RatingAggregateUpdate(20L, 5, 22L, 5, 4.4)))
                .isInstanceOf(OptimisticLockException.class);
    }

    private Query captureFind() {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Review.class));
        return captor.getValue();
    }

    private static UpdateResult matched(long count) {
        return UpdateResult.acknowledged(count, count, null);
    }

    private static Review review(String tourId, String reviewId, int rate) {
        Review review = new Review();
        review.setTourId(tourId);
        review.setReviewId(reviewId);
        review.setRate(rate);
        review.setCreatedAt("2026-06-16");
        review.setUpdatedAt("2026-06-16");
        return review;
    }
}
