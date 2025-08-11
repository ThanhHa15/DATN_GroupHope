package com.datn.datn.model;

    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDateTime;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Entity
    @Table(name = "review_likes")
    public class ReviewLike {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        // @Column(name = "review_id")
        // private Long reviewId;

        private String username;

        @Column(name = "created_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
        private LocalDateTime createdAt;

        // Quan hệ với bảng Review
        @ManyToOne
        @JoinColumn(name = "review_id")
        private Review review;

        public void setUsername(String username) {
            this.username = username;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }