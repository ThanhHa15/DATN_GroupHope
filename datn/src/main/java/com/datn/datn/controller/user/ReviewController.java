package com.datn.datn.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datn.datn.model.Review;
import com.datn.datn.model.ReviewLike;
import com.datn.datn.model.Feedback;
import com.datn.datn.model.Member;
import com.datn.datn.repository.FeedbackRepository;
import com.datn.datn.repository.ReviewLikeRepository;
import com.datn.datn.repository.ReviewRepository;
import com.datn.datn.service.FileStorageService;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewRepository reviewRepository;
    private final FeedbackRepository feedbackRepository;
    private final FileStorageService fileStorageService;
    private final ReviewLikeRepository reviewLikeRepository;

    public ReviewController(ReviewRepository reviewRepository,
            FeedbackRepository feedbackRepository,
            FileStorageService fileStorageService,
            ReviewLikeRepository reviewLikeRepository) {
        this.reviewRepository = reviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.fileStorageService = fileStorageService;
        this.reviewLikeRepository = reviewLikeRepository;
    }

    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuth(HttpSession session) {
        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not logged in"));
        }
        return ResponseEntity.ok(Map.of("username", loggedInUser.getEmail()));
    }

    @PostMapping("/{reviewId}/feedback")
    public ResponseEntity<?> addFeedback(
            @PathVariable Long reviewId,
            @RequestParam String content,
            HttpSession session) {

        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để gửi phản hồi"));
        }

        try {
            Feedback feedback = new Feedback();
            feedback.setReviewId(reviewId);
            feedback.setUsername(loggedInUser.getEmail());
            feedback.setContent(content.trim());

            Feedback savedFeedback = feedbackRepository.save(feedback);
            return ResponseEntity.ok(savedFeedback);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to add feedback: " + e.getMessage()));
        }
    }

    @GetMapping("/{reviewId}/feedback")
    public ResponseEntity<?> getFeedback(@PathVariable Long reviewId) {
        try {
            List<Feedback> feedbacks = feedbackRepository.findByReviewId(reviewId);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get feedback"));
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReview(
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam("productId") Long productId,
            @RequestParam("rating") Integer rating,
            @RequestParam("comment") String comment,
            HttpSession session) {

        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để đánh giá sản phẩm"));
        }

        try {
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
            }

            if (comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Comment cannot be empty"));
            }

            if (images != null && images.length > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Maximum 5 images allowed"));
            }

            Review review = new Review();
            review.setProductId(productId);
            review.setUsername(loggedInUser.getEmail());
            review.setRating(rating);
            review.setComment(comment.trim());

            if (images != null && images.length > 0) {
                List<String> fileNames = new ArrayList<>();
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        String contentType = file.getContentType();
                        if (contentType == null || !contentType.startsWith("image/")) {
                            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
                        }

                        String fileName = fileStorageService.storeFile(file);
                        fileNames.add("/api/reviews/image/" + fileName);
                    }
                }
                review.setImages(String.join(",", fileNames));
            }

            Review savedReview = reviewRepository.save(review);
            return ResponseEntity.ok(savedReview);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create review: " + e.getMessage()));
        }
    }

    @GetMapping("/image/{fileName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            Resource resource = fileStorageService.loadFileAsResource(fileName);

            String contentType = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getReviewsByProduct(@PathVariable Long productId,
            @RequestParam(required = false) Integer rating) {
        try {
            List<Review> reviews;
            if (rating != null && rating >= 1 && rating <= 5) {
                reviews = reviewRepository.findByProductIdAndRating(productId, rating);
            } else {
                reviews = reviewRepository.findByProductId(productId);
            }
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to load reviews");
        }
    }

    @GetMapping("/stats/{productId}")
    public ResponseEntity<?> getReviewStats(@PathVariable Long productId) {
        Map<String, Object> stats = new HashMap<>();

        // 平均评分
        Double avgRating = reviewRepository.findAverageRatingByProductId(productId);
        stats.put("averageRating", avgRating != null ? avgRating : 0);

        // 各星级数量
        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            Long count = reviewRepository.countByProductIdAndRating(productId, i);
            ratingCounts.put(i, count != null ? count : 0L);
        }
        stats.put("ratingCounts", ratingCounts);

        // 总评论数
        Long total = reviewRepository.countByProductId(productId);
        stats.put("totalReviews", total != null ? total : 0L);

        return ResponseEntity.ok(stats);
    }

    

    @PostMapping("/{reviewId}/like")
    public ResponseEntity<?> likeReview(@PathVariable Long reviewId, HttpSession session) {
        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not logged in"));
        }

        try {
            ReviewLike like = new ReviewLike();
            like.setReviewId(reviewId);
            like.setUsername(loggedInUser.getEmail());

            ReviewLike savedLike = reviewLikeRepository.save(like);
            return ResponseEntity.ok(savedLike);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to like review: " + e.getMessage()));
        }
    }

    @GetMapping("/{reviewId}/like-count")
    public ResponseEntity<?> getLikeCount(@PathVariable Long reviewId) {
        try {
            Long count = reviewLikeRepository.countByReviewId(reviewId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get like count"));
        }
    }

    @GetMapping("/{reviewId}/has-liked")
    public ResponseEntity<?> checkUserLike(@PathVariable Long reviewId, HttpSession session) {
        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.ok(Map.of("hasLiked", false));
        }

        try {
            boolean hasLiked = reviewLikeRepository.existsByReviewIdAndUsername(reviewId, loggedInUser.getEmail());
            return ResponseEntity.ok(Map.of("hasLiked", hasLiked));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to check like status"));
        }
    }
    
}