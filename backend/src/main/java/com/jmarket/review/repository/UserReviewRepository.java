package com.jmarket.review.repository;

import com.jmarket.review.domain.ReviewSourceType;
import com.jmarket.review.domain.UserReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserReviewRepository extends JpaRepository<UserReview, Long> {
    boolean existsByReviewerIdAndSourceTypeAndSourceId(Long reviewerId, ReviewSourceType sourceType, Long sourceId);

    Optional<UserReview> findByReviewerIdAndSourceTypeAndSourceId(Long reviewerId, ReviewSourceType sourceType, Long sourceId);

    List<UserReview> findAllByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    long countByTargetUserId(Long targetUserId);

    @Query("select avg(r.rating) from UserReview r where r.targetUser.id = :targetUserId")
    Double averageRatingByTargetUserId(@Param("targetUserId") Long targetUserId);
}
