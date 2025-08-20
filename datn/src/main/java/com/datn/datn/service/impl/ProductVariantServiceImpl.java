package com.datn.datn.service.impl;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.ProductVariantRepository;
import com.datn.datn.service.ProductVariantService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
                    BigDecimal discounted = variant.getDiscountedPrice();
                    Double price = variant.getPrice();

                    return discounted != null && price != null
                            && discounted.compareTo(BigDecimal.valueOf(price)) < 0;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVariant> find6UniqueOtherVariants(Integer excludeVariantId, Integer excludeProductId) {
        List<ProductVariant> allVariants = repo.findAll();

        // Lọc ra sản phẩm không trùng productId và variantId
        List<ProductVariant> filtered = allVariants.stream()
                .filter(pv -> !pv.getVariantID().equals(excludeVariantId)) // loại trừ variant hiện tại
                .filter(pv -> !pv.getProduct().getProductID().equals(excludeProductId)) // có thể loại trừ luôn sản phẩm
                                                                                        // hiện tại nếu muốn
                .collect(Collectors.toList());

        // Giữ lại duy nhất theo (productId + storage)
        Map<String, ProductVariant> uniqueVariants = filtered.stream()
                .collect(Collectors.toMap(
                        pv -> pv.getProduct().getProductID() + "_" + pv.getStorage(), // key duy nhất
                        pv -> pv,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        return uniqueVariants.values().stream()
                .limit(6)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVariant> filterByStorage(String[] storages) {
        if (storages == null || storages.length == 0) {
            return repo.findAll();
        }
        return repo.findByStorages(Arrays.asList(storages));
    }

    @Override
    public void removeDiscount(Integer variantId) {
        ProductVariant variant = getById(variantId);
        if (variant != null) {
            variant.setDiscount(null);
            variant.setDiscountStart(null);
            variant.setDiscountEnd(null);
            variant.setDiscountedPrice(BigDecimal.valueOf(variant.getPrice()));
            save(variant);
        }
    }

    @Override
    public void updateDiscount(Integer variantId, float discount, LocalDate start, LocalDate end) {
        ProductVariant variant = getById(variantId);
        if (variant != null) {
            variant.setDiscount(discount);
            variant.setDiscountStart(start);
            variant.setDiscountEnd(end);

            if (discount > 0) {
                BigDecimal price = BigDecimal.valueOf(variant.getPrice());
                BigDecimal discountRate = BigDecimal.valueOf(discount).divide(BigDecimal.valueOf(100));
                BigDecimal discounted = price.subtract(price.multiply(discountRate));
                variant.setDiscountedPrice(discounted);
            } else {
                variant.setDiscountedPrice(BigDecimal.valueOf(variant.getPrice()));
            }
            save(variant);
        }
    }

    // ----------------- PHẦN MỚI -----------------
    @Override
    public Page<ProductVariant> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    public Page<ProductVariant> searchVariantsByName(String keyword, Pageable pageable) {
        // Tìm cả theo màu và tên sản phẩm
        return repo.searchByNameOrColor(keyword, pageable);
    }

    @Override
    public Page<ProductVariant> getByProductId(Integer productId, Pageable pageable) {
        return repo.findByProduct_ProductID(productId, pageable);
    }

    @Override
    public Page<ProductVariant> filterByCategory(Integer categoryId, Pageable pageable) {
        return repo.findByCategory(categoryId, pageable);
    }

    // @Override
    // public Page<ProductVariant> getByStatus(String status, Pageable pageable) {
    // return repo.findByStatus(status, pageable);
    // }
    // ----------------- HẾT PHẦN MỚI -----------------

    @Override
    public Page<ProductVariant> filterByStatus(String status, Pageable pageable) {
        if (status == null || status.isEmpty()) {
            return repo.findAll(pageable);
        }

        if (status.equals("het")) {
            return repo.findOutOfStock(pageable);
        } else if (status.equals("con")) {
            return repo.findInStock(pageable);
        } else {
            return repo.findAll(pageable);
        }
    }

}
