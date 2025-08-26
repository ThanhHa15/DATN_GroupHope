package com.datn.datn.repository;

import com.datn.datn.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BestSellerRepository extends JpaRepository<Product, Integer> {

   @Query(value = """
    SELECT 
        p.productID AS productId,
        p.product_name AS productName,
        pv.storage + ' - ' + pv.color AS config,
        c.name AS categoryName,
        pv.price AS price,
        ISNULL(SUM(od.quantity), 0) AS totalSold,
        pv.imagesno2 AS imageUrl,
        CASE
            WHEN p.status = 1 THEN N'Đang bán'
            ELSE N'Ngừng bán'
        END AS status
    FROM Products p
    JOIN Categories c ON p.CategoryID = c.categoryID
    JOIN Product_variants pv ON pv.ProductID = p.productID
    LEFT JOIN order_details od ON od.variant_id = pv.variantID
    LEFT JOIN orders o ON o.id = od.order_id
    WHERE (:keyword IS NULL OR p.product_name LIKE %:keyword%)
      AND (:categoryId IS NULL OR c.categoryID = :categoryId)
    GROUP BY p.productID, p.product_name, pv.storage, pv.color, c.name, pv.price, p.status, pv.imagesno2
    HAVING 
        (:status IS NULL) OR
        (:status = 'selling' AND ISNULL(SUM(od.quantity), 0) > 0) OR
        (:status = 'notselling' AND ISNULL(SUM(od.quantity), 0) = 0)
    ORDER BY totalSold DESC
    """,
    countQuery = """
    SELECT COUNT(DISTINCT p.productID) 
    FROM Products p
    JOIN Categories c ON p.CategoryID = c.categoryID
    JOIN Product_variants pv ON pv.ProductID = p.productID
    LEFT JOIN order_details od ON od.variant_id = pv.variantID
    LEFT JOIN orders o ON o.id = od.order_id
    WHERE (:keyword IS NULL OR p.product_name LIKE %:keyword%)
      AND (:categoryId IS NULL OR c.categoryID = :categoryId)
    GROUP BY p.productID
    HAVING 
        (:status IS NULL) OR
        (:status = 'selling' AND ISNULL(SUM(od.quantity), 0) > 0) OR
        (:status = 'notselling' AND ISNULL(SUM(od.quantity), 0) = 0)
    """,
    nativeQuery = true)
Page<Object[]> getBestSellingProducts(
    @Param("keyword") String keyword, 
    @Param("categoryId") Integer categoryId, 
    @Param("status") String status, 
    Pageable pageable);
}