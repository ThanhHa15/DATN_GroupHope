package com.datn.datn.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.datn.datn.model.*;

import java.util.stream.Collectors;

import com.datn.datn.repository.*;
import com.datn.datn.service.FileStorageService;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
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
    private final ProductRepository productRepository;

    public ReviewController(ReviewRepository reviewRepository,
            FeedbackRepository feedbackRepository,
            FileStorageService fileStorageService,
            ReviewLikeRepository reviewLikeRepository,
            ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.fileStorageService = fileStorageService;
        this.reviewLikeRepository = reviewLikeRepository;
        this.productRepository = productRepository;
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
    @Transactional
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
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));

            Feedback feedback = new Feedback();
            feedback.setReview(review);
            feedback.setUsername(loggedInUser.getEmail());
            feedback.setContent(content.trim());
            feedback.setCreatedAt(LocalDateTime.now());

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
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));
            List<Feedback> feedbacks = feedbackRepository.findByReview(review);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get feedback: " + e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReview(
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam("productId") Integer productId, // Đổi từ Integer sang Long
            @RequestParam("rating") Integer rating,
            @RequestParam("comment") String comment,
            HttpSession session) {

        try {
            // Kiểm tra đăng nhập
            Member loggedInUser = (Member) session.getAttribute("loggedInUser");
            if (loggedInUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Vui lòng đăng nhập"));
            }

            // Validate dữ liệu
            if (productId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Thiếu productId"));
            }

            // Tìm product
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            // Tạo review mới
            Review review = new Review();
            review.setProduct(product); // Đảm bảo product được set đúng
            review.setUsername(loggedInUser.getEmail());
            review.setRating(rating);
            review.setComment(comment.trim());
            review.setCreatedAt(LocalDateTime.now());

            // 5. Xử lý ảnh (nếu có)
            if (images != null && images.length > 0) {
                List<String> imageUrls = new ArrayList<>();
                for (MultipartFile image : images) {
                    if (!image.isEmpty() && image.getSize() > 0) {
                        if (!image.getContentType().startsWith("image/")) {
                            return ResponseEntity.badRequest()
                                    .body(Map.of("error", "Chỉ chấp nhận file ảnh"));
                        }
                        if (image.getSize() > 5 * 1024 * 1024) { // 5MB
                            return ResponseEntity.badRequest()
                                    .body(Map.of("error", "Ảnh không được vượt quá 5MB"));
                        }
                        String fileName = fileStorageService.storeFile(image);
                        imageUrls.add("/api/reviews/image/" + fileName);
                    }
                }
                if (!imageUrls.isEmpty()) {
                    review.setImages(String.join(",", imageUrls));
                }
            }

            // 6. Lưu review
            Review savedReview = reviewRepository.save(review);
            return ResponseEntity.ok(savedReview);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi khi tạo review: " + e.getMessage()));
        }
    }

    // @PostMapping("/api/reviews")
    // public ResponseEntity<?> submitReview(@RequestParam("rating") int rating,
    // @RequestParam("comment") String comment,
    // @RequestParam(value = "images", required = false) MultipartFile[] images) {
    // try {
    // Review review = reviewService.submitReview(rating, comment, images);
    // return ResponseEntity.ok(review);
    // } catch (Exception e) {
    // return ResponseEntity.internalServerError().body(Map.of(
    // "error", "Transaction failed",
    // "message", e.getMessage()));
    // }
    // }

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
    public ResponseEntity<?> getReviewsByProduct(
            @PathVariable Integer productId,
            @RequestParam(required = false) Optional<Integer> rating) {

        System.out.println("Fetching reviews for product: " + productId + ", rating: " + rating);

        try {
            List<Review> reviews = rating.isPresent()
                    ? reviewRepository.findByProductIdAndRating(productId, rating.get())
                    : reviewRepository.findByProductId(productId);

            // Map each Review -> simple Map (DTO-like) to avoid circular refs and bad JSON
            List<Map<String, Object>> out = reviews.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("username", r.getUsername());
                m.put("rating", r.getRating());
                m.put("comment", r.getComment());
                m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);

                // images field: convert comma-separated string -> array (consistent JSON)
                String imgs = r.getImages();
                List<String> urls = (imgs == null || imgs.trim().isEmpty())
                        ? Collections.emptyList()
                        : Arrays.stream(imgs.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
                m.put("images", urls);

                // feedbacks: load minimal fields only (NO review back-ref)
                List<Feedback> fbEntities = feedbackRepository.findByReview(r);
                List<Map<String, Object>> fbList = fbEntities.stream().map(fb -> {
                    Map<String, Object> fm = new HashMap<>();
                    fm.put("id", fb.getId());
                    fm.put("username", fb.getUsername());
                    fm.put("content", fb.getContent());
                    fm.put("createdAt", fb.getCreatedAt() != null ? fb.getCreatedAt().toString() : null);
                    return fm;
                }).collect(Collectors.toList());
                m.put("feedbacks", fbList);

                return m;
            }).collect(Collectors.toList());

            System.out.println("Found reviews (mapped): " + out.size());
            return ResponseEntity.ok(out);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch reviews: " + e.getMessage()));
        }
    }

    @GetMapping("/stats/{productId}")
    public ResponseEntity<?> getReviewStats(@PathVariable Integer productId) {
        try {
            Map<String, Object> stats = new HashMap<>();

            Double avgRating = reviewRepository.findAverageRatingByProductId(productId);
            stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0);

            Map<Integer, Long> ratingCounts = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                Long count = reviewRepository.countByProductIdAndRating(productId, i);
                ratingCounts.put(i, count != null ? count : 0L);
            }
            stats.put("ratingCounts", ratingCounts);

            Long total = reviewRepository.countByProductId(productId);
            stats.put("totalReviews", total != null ? total : 0L);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get review stats: " + e.getMessage()));
        }
    }

    @PostMapping("/{reviewId}/like")
    @Transactional
    public ResponseEntity<?> likeReview(@PathVariable Long reviewId, HttpSession session) {
        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not logged in"));
        }

        try {
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));

            boolean alreadyLiked = reviewLikeRepository.existsByReviewAndUsername(review, loggedInUser.getEmail());
            if (alreadyLiked) {
                // Nếu đã like thì bỏ like (xóa bản ghi)
                reviewLikeRepository.deleteByReviewAndUsername(review, loggedInUser.getEmail());
                return ResponseEntity.ok(Map.of("message", "unliked"));
            } else {
                // Nếu chưa like thì thêm mới
                ReviewLike like = new ReviewLike();
                like.setReview(review);
                like.setUsername(loggedInUser.getEmail());
                like.setCreatedAt(LocalDateTime.now());

                reviewLikeRepository.save(like);
                return ResponseEntity.ok(Map.of("message", "liked"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to like/unlike review: " + e.getMessage()));
        }
    }

    @GetMapping("/{reviewId}/like-count")
    public ResponseEntity<?> getLikeCount(@PathVariable Long reviewId) {
        try {
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));
            Long count = reviewLikeRepository.countByReview(review);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get like count: " + e.getMessage()));
        }
    }

    @GetMapping("/{reviewId}/has-liked")
    public ResponseEntity<?> checkUserLike(@PathVariable Long reviewId, HttpSession session) {
        Member loggedInUser = (Member) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.ok(Map.of("hasLiked", false));
        }

        try {
            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));
            boolean hasLiked = reviewLikeRepository.existsByReviewAndUsername(review, loggedInUser.getEmail());
            return ResponseEntity.ok(Map.of("hasLiked", hasLiked));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to check like status: " + e.getMessage()));
        }
    }
}