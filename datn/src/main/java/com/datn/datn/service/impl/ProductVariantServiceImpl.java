package com.datn.datn.service.impl;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.ProductVariantRepository;
import com.datn.datn.service.ProductVariantService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository repo;

    public ProductVariantServiceImpl(ProductVariantRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<ProductVariant> getAll() {
        return repo.findAll();
    }

    @Override
    public ProductVariant getById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public ProductVariant save(ProductVariant variant) {
        return repo.save(variant);
    }

    @Override
    public void delete(Integer id) {
        repo.deleteById(id);
    }

    @Override
    public List<ProductVariant> getRandomProductVariants(int limit) {
        return repo.findRandomProductVariants(limit);
    }

    @Override
    public List<ProductVariant> findByProductAndStorage(Product product, String storage) {
        return repo.findByProductAndStorage(product, storage);
    }

    @Override
    public List<ProductVariant> findByProduct(Product product) {
        return repo.findByProduct(product);
    }

    @Override
    public List<ProductVariant> findUniqueVariantsByProductAndStorage() {
        return repo.findUniqueVariantsByProductAndStorage();
    }

    @Override
    public List<ProductVariant> findDiscountedProducts() {
        LocalDate today = LocalDate.now();
        return repo.findAll().stream()
                .filter(p -> p.getDiscount() != null
                        && p.getDiscountStart() != null
                        && p.getDiscountEnd() != null
                        && !today.isBefore(p.getDiscountStart())
                        && !today.isAfter(p.getDiscountEnd()))
                .toList();
    }

    @Override
    public void applyDiscountToStorage(Integer productId, String storage, float discount, LocalDate start,
            LocalDate end) {
        List<ProductVariant> variants = repo.findByProduct_ProductIDAndStorage(productId, storage);

        for (ProductVariant variant : variants) {
            variant.setDiscount(discount);
            variant.setDiscountStart(start);
            variant.setDiscountEnd(end);

            if (discount > 0) {
                BigDecimal price = BigDecimal.valueOf(variant.getPrice()); // từ Double -> BigDecimal
                BigDecimal discountRate = BigDecimal.valueOf(discount).divide(BigDecimal.valueOf(100));
                BigDecimal discounted = price.subtract(price.multiply(discountRate));
                variant.setDiscountedPrice(discounted);
            } else {
                variant.setDiscountedPrice(BigDecimal.valueOf(variant.getPrice()));
            }
        }

        repo.saveAll(variants);
    }

    @Override
    public List<String> findStoragesByProductId(Integer productId) {
        return repo.findDistinctStorageByProduct_ProductID(productId);
    }

    @Override
    public List<ProductVariant> getDiscountedVariants() {
        return repo.findDiscountedVariants();
    }

    @Override
    public List<ProductVariant> findDiscountedVariants() {
        return repo.findAll().stream()
                .filter(variant -> {
                    Double discounted = variant.getDiscountedPrice();
                    Double price = variant.getPrice();
                    return discounted != null && price != null && discounted < price;
                })
                .collect(Collectors.toList());
    }

}
