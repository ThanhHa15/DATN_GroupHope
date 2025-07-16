package com.datn.datn.controller.user;

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

        // Lấy duy nhất mỗi storage 1 variant (theo thứ tự xuất hiện)
        Map<String, ProductVariant> uniqueVariantsByStorage = variantsByProduct.stream()
                .collect(Collectors.toMap(
                        ProductVariant::getStorage,
                        pv -> pv,
                        (existing, replacement) -> existing, // giữ lại bản đầu tiên
                        LinkedHashMap::new));

        model.addAttribute("v", variant);
        model.addAttribute("sameStorageVariants", sameStorageVariants);
        model.addAttribute("uniqueVariantsByStorage", uniqueVariantsByStorage.values());
        variantsByProduct.forEach(pv -> System.out.println("Variant ID: " + pv.getVariantID() + " | Storage: "
                + pv.getStorage() + " | Product Name: " + pv.getProduct().getProductName()));
        List<ProductSpecification> specifications = productSpecificationService
                .getSpecificationsByProductId(variant.getProduct().getProductID());

        model.addAttribute("specifications", specifications);

        return "views/user/products-detail";

    }
}
