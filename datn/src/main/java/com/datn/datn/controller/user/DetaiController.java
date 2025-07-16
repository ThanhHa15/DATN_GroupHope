package com.datn.datn.controller.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.datn.datn.model.ProductSpecification;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.ProductSpecificationService;
import com.datn.datn.service.ProductVariantService;

@Controller
@RequestMapping("/detail")
public class DetaiController {

        private final ProductVariantService productVariantService;

        private final ProductSpecificationService productSpecificationService;

        public DetaiController(ProductVariantService productVariantService,
                        ProductSpecificationService productSpecificationService) {
                this.productVariantService = productVariantService;
                this.productSpecificationService = productSpecificationService;
        }

        @GetMapping("/{id}")
        public String detail(@PathVariable("id") Integer variantId, Model model) {
                ProductVariant variant = productVariantService.getById(variantId);

                if (variant == null) {
                        return "redirect:/error";
                }

                List<ProductVariant> sameStorageVariants = productVariantService.findByProductAndStorage(
                                variant.getProduct(), variant.getStorage());

                List<ProductVariant> variantsByProduct = productVariantService.findByProduct(variant.getProduct());

                Map<String, ProductVariant> uniqueVariantsByStorage = variantsByProduct.stream()
                                .collect(Collectors.toMap(
                                                ProductVariant::getStorage,
                                                pv -> pv,
                                                (existing, replacement) -> existing,
                                                LinkedHashMap::new));

                List<ProductSpecification> specifications = productSpecificationService
                                .getSpecificationsByProductId(variant.getProduct().getProductID());

                // 👉 Tận dụng hàm có sẵn trong HomeController
                List<ProductVariant> allUniqueVariants = productVariantService.findUniqueVariantsByProductAndStorage();

                // 👉 Lọc: KHÔNG cùng productId (tức loại bỏ mọi phiên bản của sp đang xem),
                // cùng danh mục
                List<ProductVariant> otherProducts = allUniqueVariants.stream()
                                .filter(pv -> !pv.getProduct().getProductID()
                                                .equals(variant.getProduct().getProductID())) // ✅ KHÁC sản phẩm đang
                                                                                              // xem
                                .filter(pv -> pv.getProduct().getCategory().getCategoryID()
                                                .equals(variant.getProduct().getCategory().getCategoryID())) // ✅ CÙNG
                                                                                                             // danh mục
                                .limit(5)
                                .collect(Collectors.toList()); 

                // Đưa vào model
                model.addAttribute("v", variant);
                model.addAttribute("sameStorageVariants", sameStorageVariants);
                model.addAttribute("uniqueVariantsByStorage", uniqueVariantsByStorage.values());
                model.addAttribute("specifications", specifications);
                model.addAttribute("products", otherProducts);

                return "views/user/products-detail";
        }

}
