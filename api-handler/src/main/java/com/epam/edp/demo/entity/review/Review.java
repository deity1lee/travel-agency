package com.epam.edp.demo.entity.review;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "reviews")
@CompoundIndexes({
        @CompoundIndex(name = "tourId_createdAt", def = "{'tourId': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "tourId_rate", def = "{'tourId': 1, 'rate': 1}")
})
public class Review {

    @Id
    private String reviewId;

    private String tourId;

    @Indexed
    private String userId;

    private String authorName;
    private String authorImageUrl;
    private String createdAt;
    private String updatedAt;
    private Integer rate;
    private String reviewContent;
}
