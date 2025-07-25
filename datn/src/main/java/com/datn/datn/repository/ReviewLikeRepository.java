package com.datn.datn.repository;
import com.datn.datn.model.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    @Query("SELECT COUNT(l) FROM ReviewLike l WHERE l.reviewId = :reviewId")
    Long countByReviewId(@Param("reviewId") Long reviewId);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM ReviewLike l WHERE l.reviewId = :reviewId AND l.username = :username")
    boolean existsByReviewIdAndUsername(@Param("reviewId") Long reviewId, @Param("username") String username);

    @Query("DELETE FROM ReviewLike l WHERE l.reviewId = :reviewId AND l.username = :username")
    void deleteByReviewIdAndUsername(@Param("reviewId") Long reviewId, @Param("username") String username);
}
