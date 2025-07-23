package com.datn.datn.service;

import java.util.List;

import com.datn.datn.model.Member;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.model.Wishlist;

public interface WishlistService {
    void addToWishlist(Member user, ProductVariant variant);

    List<Wishlist> getWishlistByUser(Member user);

    List<ProductVariant> getWishlistByUserId(Long userId);

    boolean toggleWishlist(Member user, ProductVariant variant);

}
