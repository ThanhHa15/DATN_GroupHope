package com.datn.datn.service;

import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.repository.ProductRepository;
import com.datn.datn.repository.ProductVariantRepository;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    public ChatbotResponse handleCustomerQuery(String query) {
        String normalizedQuery = normalizeQuery(query);

        // 🔍 Tìm kiếm fuzzy
        List<Product> matches = findFuzzyMatches(normalizedQuery);
        if (!matches.isEmpty()) {
            return createProductResponse(matches.get(0)); // có thể trả nhiều kết quả nếu muốn
        }

        // 💬 Kiểm tra các câu hỏi thông thường
        String defaultResponse = getDefaultResponse(query);
        if (defaultResponse != null) {
            return new ChatbotResponse(false, defaultResponse, null, null);
        }

        // ❌ Không tìm thấy
        return new ChatbotResponse(
                false,
                "Xin lỗi, tôi không tìm thấy sản phẩm phù hợp với '" + query + "'. Bạn có thể mô tả rõ hơn không?",
                null,
                null);
    }

    private String normalizeQuery(String query) {
        return removeAccents(query.toLowerCase())
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // Hàm bỏ dấu tiếng Việt
    private String removeAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    // Tìm kiếm gần đúng (fuzzy search)
    private List<Product> findFuzzyMatches(String query) {
        JaroWinklerSimilarity jw = new JaroWinklerSimilarity();

        return productRepository.findAll().stream()
                .filter(p -> {
                    String name = removeAccents(p.getProductName().toLowerCase());
                    // Nếu chứa trực tiếp
                    if (name.contains(query) || query.contains(name)) {
                        return true;
                    }
                    // Nếu độ tương đồng >= 0.6
                    return jw.apply(query, name) >= 0.6;
                })
                // Sắp xếp theo độ tương đồng giảm dần
                .sorted((p1, p2) -> {
                    double s1 = jw.apply(query, removeAccents(p1.getProductName().toLowerCase()));
                    double s2 = jw.apply(query, removeAccents(p2.getProductName().toLowerCase()));
                    return Double.compare(s2, s1);
                })
                .collect(Collectors.toList());
    }

    // service/ChatbotService.java
    private ChatbotResponse createProductResponse(Product product) {
        List<ProductVariant> variants = productVariantRepository.findByProduct(product);
        String message = variants.isEmpty()
                ? "Tìm thấy sản phẩm '" + product.getProductName() + "' nhưng hiện không có phiên bản nào có sẵn."
                : "Đây là các phiên bản có sẵn cho '" + product.getProductName() + "':";

        // ⚡ Trả về product nhưng đảm bảo imageUrl chỉ là tên file
        if (product.getImageUrl() != null) {
            String raw = product.getImageUrl().trim();
            // Loại bỏ prefix nếu có (images/, uploads/, /path/...)
            raw = raw.replaceAll("^(images/|uploads/)+", "");
            product.setImageUrl(raw);
        }

        return new ChatbotResponse(true, message, product, variants);
    }

    private String getDefaultResponse(String query) {
        String normalized = normalizeQuery(query);

        if (normalized.matches("(hello|hi|chào|xin chào).*")) {
            return "Xin chào! Tôi là trợ lý AI của Apple HOPEPHONE . Tôi có thể giúp gì cho bạn?";
        }

        if (normalized.matches(".*(cảm ơn|thanks|thank you).*")) {
            return "Không có gì! Nếu bạn cần thêm thông tin gì, cứ hỏi tôi nhé!";
        }

        if (normalized.matches(".*(giá|price|cost|bao nhiêu).*")) {
            return "Vui lòng cho tôi biết tên sản phẩm cụ thể bạn muốn hỏi giá.";
        }

        return null;
    }

    public static class ChatbotResponse {
        private boolean hasMatch;
        private String message;
        private Product product;
        private List<ProductVariant> variants;

        public ChatbotResponse(boolean hasMatch, String message, Product product, List<ProductVariant> variants) {
            this.hasMatch = hasMatch;
            this.message = message;
            this.product = product;
            this.variants = variants;
        }

        public boolean isHasMatch() {
            return hasMatch;
        }

        public String getMessage() {
            return message;
        }

        public Product getProduct() {
            return product;
        }

        public List<ProductVariant> getVariants() {
            return variants;
        }
    }
}