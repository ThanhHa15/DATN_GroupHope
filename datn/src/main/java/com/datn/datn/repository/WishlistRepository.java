package com.datn.datn.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datn.datn.model.Member;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.model.Wishlist;
import org.springframework.data.domain.Pageable;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    boolean existsByUserAndProductVariant(Member user, ProductVariant variant);

    List<Wishlist> findByUser(Member user);

    @Query("SELECT pv FROM Wishlist w JOIN w.productVariant pv WHERE w.user.id = :userId")
    List<ProductVariant> findWishlistByUserId(@Param("userId") Long userId);

    Wishlist findByUserAndProductVariant(Member user, ProductVariant variant);

    @Query("SELECT w.productVariant FROM Wishlist w WHERE w.user.id = :userId")
    Page<ProductVariant> findWishlistByUserId(@Param("userId") Long userId, Pageable pageable);

    void deleteByProductVariant_VariantID(Integer variantID);
}
