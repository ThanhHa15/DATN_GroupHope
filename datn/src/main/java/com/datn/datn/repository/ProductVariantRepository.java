package com.datn.datn.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

        @Query(value = "SELECT TOP (:limit) * FROM product_variants ORDER BY NEWID()", nativeQuery = true)
        List<ProductVariant> findRandomProductVariants(@Param("limit") int limit);

        @Query("SELECT pv FROM ProductVariant pv WHERE pv.product = :product AND pv.storage = :storage")
        List<ProductVariant> findByProductAndStorage(@Param("product") Product product,
                        @Param("storage") String storage);

        @Query(value = """
                        SELECT pv.*
                        FROM product_variants pv
                        JOIN (
                            SELECT productid, storage, MIN(variantid) AS min_variantid
                            FROM product_variants
                            GROUP BY productid, storage
                        ) grouped ON pv.productid = grouped.productid
                                 AND pv.storage = grouped.storage
                                 AND pv.variantid = grouped.min_variantid
                        """, nativeQuery = true)
        List<ProductVariant> findUniqueVariantsByProductAndStorage();

        List<ProductVariant> findByProduct(Product product);

        // Lấy các phiên bản cùng storage trong 1 sản phẩm
        List<ProductVariant> findByProduct_ProductIDAndStorage(Integer productId, String storage);

        @Query("SELECT DISTINCT v.storage FROM ProductVariant v WHERE v.product.productID = :productId")
        List<String> findDistinctStorageByProduct_ProductID(@Param("productId") Integer productId);

        @Query("SELECT v FROM ProductVariant v WHERE v.discount > 0 AND v.discountStart <= CURRENT_DATE AND v.discountEnd >= CURRENT_DATE")
        List<ProductVariant> findDiscountedVariants();

        @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE v.discount > 0 AND v.discountStart <= CURRENT_DATE AND v.discountEnd >= CURRENT_DATE")
        List<Product> findProductsWithActiveDiscount();

        @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.productID = :productId AND pv.variantID <> :excludeVariantId")
        List<ProductVariant> findByProductIdAndVariantIDNot(@Param("productId") Integer productId,
                        @Param("excludeVariantId") Integer excludeVariantId);

        @Query("SELECT pv FROM ProductVariant pv WHERE (:storages IS NULL OR pv.storage IN :storages)")
        List<ProductVariant> findByStorages(@Param("storages") List<String> storages);

        Page<ProductVariant> findAll(Pageable pageable);

        // Tìm kiếm theo tên hoặc màu
        Page<ProductVariant> findByProduct_ProductID(Integer productId, Pageable pageable);
        // Lấy các phiên bản theo danh mục sản phẩm
        // Page<ProductVariant> findByProduct_Category_CategoryID(Integer categoryId,
        // Pageable pageable);

        // Tìm theo tên sản phẩm hoặc màu
        @Query("SELECT v FROM ProductVariant v " +
                        "WHERE LOWER(v.color) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(v.product.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<ProductVariant> searchByNameOrColor(@Param("keyword") String keyword, Pageable pageable);

        // Lấy các phiên bản theo trạng thái
        @Query("SELECT v FROM ProductVariant v WHERE v.quantityInStock = 0")
        Page<ProductVariant> findOutOfStock(Pageable pageable);

        // Còn hàng
        @Query("SELECT v FROM ProductVariant v WHERE v.quantityInStock > 0")
        Page<ProductVariant> findInStock(Pageable pageable);

        // Lấy các phiên bản theo danh mục sản phẩm
        @Query("SELECT v FROM ProductVariant v " +
                        "JOIN v.product p " +
                        "JOIN p.category c " +
                        "WHERE (:categoryId = 0 OR c.categoryID = :categoryId)")
        Page<ProductVariant> findByCategory(@Param("categoryId") Integer categoryId, Pageable pageable);

        // hàm lọc tên sản phẩm từ A-Z

        boolean existsByProduct_ProductIDAndColorAndStorage(Integer productID, String color, String storage);

}
