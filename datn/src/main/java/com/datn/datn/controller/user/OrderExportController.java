package com.datn.datn.controller.user;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.datn.datn.model.Member;
import com.datn.datn.model.Order;
import com.datn.datn.model.OrderDetail;
import com.datn.datn.model.Product;
import com.datn.datn.model.ProductVariant;
import com.datn.datn.service.OrderService;
import com.itextpdf.io.font.PdfEncodings;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Controller
public class OrderExportController {
    @Autowired
    private OrderService orderService;

    // Endpoint để xuất file PDF đơn hàng
    @GetMapping("/order/pdf/{orderId}")
    public void exportToPDF(@PathVariable Long orderId, HttpServletResponse response) throws IOException {
        Order order = orderService.getOrderById(orderId);
        // Thiết lập header response
        response.setContentType("application/pdf");
        String headerValue = "attachment; filename=order_" +
                new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pdf";
        response.setHeader("Content-Disposition", headerValue);

        // Tạo tài liệu PDF
        PdfDocument pdf = new PdfDocument(new PdfWriter(response.getOutputStream()));
        try (Document document = new Document(pdf)) {

            // Thiết lập font chữ
            PdfFont font = PdfFontFactory.createFont(
                    "C://Users//hail1//Downloads//Roboto//Roboto-Italic-VariableFont_wdth,wght.ttf",
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            document.setFont(font);

            // Thêm thời gian mua hàng
            LocalDateTime orderDate = order.getOrderDate();
            String formatted = orderDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Paragraph orderTime = new Paragraph("Ngày đặt hàng: " + formatted)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(10);
            document.add(orderTime);
            /* ========== PHẦN TIÊU ĐỀ ========== */
            Paragraph shopName = new Paragraph("HopePhone")
                    .setBold() // In đậm
                    .setFontSize(22) // Cỡ chữ 22
                    .setTextAlignment(TextAlignment.CENTER); // Canh giữa
            document.add(shopName);

            // Lời cảm ơn
            Paragraph thanks = new Paragraph("Cảm ơn bạn đã đặt hàng")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15); // Khoảng cách dưới 15px
            document.add(thanks);

            // Thông báo email xác nhận
            String customerEmail = order.getMember().getEmail();
            document.add(new Paragraph("Một email đã được gửi tới " + customerEmail + ".")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Xin vui lòng kiểm tra email của bạn")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n")); // Xuống dòng

            /* ========== THÔNG TIN ĐƠN HÀNG ========== */
            document.add(new Paragraph("Đơn hàng #" + order.getOrderCode())
                    .setBold()
                    .setFontSize(13));

            // Tạo bảng sản phẩm
            float[] columnWidths = { 5, 1, 2 }; // Tỷ lệ cột: 5-1-2
            Table productTable = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

            // Tiêu đề bảng
            productTable.addHeaderCell(
                    new Cell().add(new Paragraph("Sản phẩm"))
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                            .setBold());
            productTable.addHeaderCell(
                    new Cell().add(new Paragraph("SL")) // Số lượng
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER));
            productTable.addHeaderCell(
                    new Cell().add(new Paragraph("Đơn giá"))
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                            .setBold()
                            .setTextAlignment(TextAlignment.RIGHT));

            // Dữ liệu sản phẩm mẫu

            // Format giá
            DecimalFormat df = new DecimalFormat("#,###");

            // Duyệt các sản phẩm trong đơn
            for (OrderDetail item : order.getOrderDetails()) {
                ProductVariant variant = item.getProductVariant();
                Product product = variant.getProduct();

                String productName = product.getProductName(); // Tên sản phẩm
                String storage = variant.getStorage(); // Storage
                String color = variant.getColor(); // Màu
                Integer quantity = item.getQuantity(); // Số lượng
                Double price = item.getPrice(); // Giá
                // Tính thành tiền = giá * số lượng
                Double totalPrice = price * quantity;

                // Ô tên sản phẩm + storage + màu
                Cell productCell = new Cell();
                productCell.add(new Paragraph(productName + " - " + storage).setBold());
                productCell.add(new Paragraph("Màu: " + color)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY));
                productTable.addCell(productCell);

                // Ô số lượng
                productTable.addCell(new Paragraph("x" + quantity)
                        .setTextAlignment(TextAlignment.CENTER));

                // Ô giá
                productTable.addCell(new Paragraph(df.format(price) + "₫")
                        .setTextAlignment(TextAlignment.RIGHT));
            }

            document.add(productTable);

            // ========== BẢNG TỔNG CỘNG ==========
            Table totalTable = new Table(UnitValue.createPercentArray(new float[] { 7, 2 })).useAllAvailableWidth();

            // Tổng tiền sản phẩm
            Double totalProductsPrice = order.getOrderDetails().stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
            totalTable.addCell(createNoBorderCell("Tổng tiền sản phẩm").setTextAlignment(TextAlignment.RIGHT));
            totalTable.addCell(
                    createNoBorderCell(df.format(totalProductsPrice) + "₫").setTextAlignment(TextAlignment.RIGHT));

            // Phí vận chuyển
            Double shippingFee = 40000.0; // ví dụ cố định hoặc lấy từ order nếu có
            totalTable.addCell(createNoBorderCell("Phí vận chuyển").setTextAlignment(TextAlignment.RIGHT));
            totalTable.addCell(createNoBorderCell(df.format(shippingFee) + "₫").setTextAlignment(TextAlignment.RIGHT));

            // Tạm tính = Tổng tiền sản phẩm + Phí vận chuyển
            Double subTotal = totalProductsPrice + shippingFee;
            totalTable.addCell(createNoBorderCell("Tạm tính").setTextAlignment(TextAlignment.RIGHT));
            totalTable.addCell(createNoBorderCell(df.format(subTotal) + "₫").setTextAlignment(TextAlignment.RIGHT));

            // Giảm giá
            Double discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0;
            totalTable.addCell(createNoBorderCell("Giảm giá").setTextAlignment(TextAlignment.RIGHT));
            totalTable
                    .addCell(createNoBorderCell("-" + df.format(discount) + "₫").setTextAlignment(TextAlignment.RIGHT));

            // Tổng tiền = Tạm tính - Giảm giá
            Double finalTotal = subTotal - discount;
            totalTable.addCell(new Cell().add(new Paragraph("Tổng tiền").setBold())
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT));
            totalTable.addCell(new Cell().add(new Paragraph(df.format(finalTotal) + "₫").setBold())
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(totalTable);
            document.add(new Paragraph("\n")); // Xuống dòng

            /* ========== THÔNG TIN KHÁCH HÀNG ========== */
            Table infoTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 })).useAllAvailableWidth();

            // Thông tin mua hàng (bên trái)
            Member member = order.getMember(); // Lấy thông tin khách hàng
            Cell left = new Cell().add(new Paragraph("Thông tin mua hàng").setBold())
                    .setBorder(Border.NO_BORDER);
            left.add(new Paragraph(member.getFullname())); // Tên khách hàng
            left.add(new Paragraph(member.getEmail())); // Email
            left.add(new Paragraph(member.getPhone())); // Số điện thoại
            left.add(new Paragraph("Phương thức thanh toán: " + order.getPaymentMethod())); // Hình thức thanh toán
            infoTable.addCell(left);

            // Địa chỉ nhận hàng (bên phải)
            Cell right = new Cell().add(new Paragraph("Địa chỉ nhận hàng").setBold())
                    .setBorder(Border.NO_BORDER);

            // Tên người nhận: nếu có trường riêng, dùng order.getReceiverName(), nếu không
            // thì lấy member
            right.add(new Paragraph(member.getFullname()));

            // Địa chỉ nhận hàng
            right.add(new Paragraph(order.getAddress()));

            // Nếu muốn tách rõ thành các dòng: xã, huyện, tỉnh, bạn cần lưu từng phần trong
            // Order
            // Ví dụ: right.add(new Paragraph(order.getWard()));
            // right.add(new Paragraph(order.getDistrict()));
            // right.add(new Paragraph(order.getProvince()));

            // Số điện thoại người nhận
            right.add(new Paragraph(member.getPhone()));


            infoTable.addCell(right);

            document.add(infoTable);

        }
    }

    // Phương thức hỗ trợ tạo ô không viền
    private Cell createNoBorderCell(String text) {
        return new Cell().add(new Paragraph(text))
                .setBorder(Border.NO_BORDER)
                .setPadding(4); // Khoảng đệm 4px
    }
}