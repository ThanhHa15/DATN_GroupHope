package com.datn.datn.service.impl;

import com.datn.datn.model.*;
import com.datn.datn.repository.*;
import com.datn.datn.service.WishlistService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Override
    public void addToWishlist(Member user, ProductVariant variant) {
        if (!wishlistRepository.existsByUserAndProductVariant(user, variant)) {
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            wishlist.setProductVariant(variant);
            wishlistRepository.save(wishlist);
        }
    }

    @Override
    public List<Wishlist> getWishlistByUser(Member user) {
        return wishlistRepository.findByUser(user);
    }

    @Override
    public List<ProductVariant> getWishlistByUserId(Long userId) {
        return wishlistRepository.findWishlistByUserId(userId);
    }

    @Override
    public boolean toggleWishlist(Member user, ProductVariant variant) {
        Wishlist existing = wishlistRepository.findByUserAndProductVariant(user, variant);
        if (existing != null) {
            wishlistRepository.delete(existing);
            return false;
        } else {
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            wishlist.setProductVariant(variant);
            wishlistRepository.save(wishlist);
            return true;
        }
    }

}
