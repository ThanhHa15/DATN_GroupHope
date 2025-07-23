package com.datn.datn.common;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.datn.datn.model.Member;

public class VnpayUtils {

    public static String createPaymentUrl(BigDecimal totalAmount, Member member) {
       String vnp_TmnCode = "ODNNT8LI";
        String vnp_HashSecret = "L0EQ58ZZ3TN5QRXXASM9WZA3SPE63NZJ";
        String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
String vnp_Returnurl = "http://localhost:8080/checkout/return";


        String vnp_TxnRef = String.valueOf(System.currentTimeMillis());
        String vnp_OrderInfo = "Thanh toan don hang";
        String vnp_OrderType = "other";
         String vnp_Amount = String.valueOf(totalAmount.multiply(new BigDecimal(100)).longValue());

        String vnp_Locale = "vn";
        String vnp_BankCode = "";
        String vnp_IpAddr = "127.0.0.1";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", vnp_Amount);
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", vnp_Locale);
        vnp_Params.put("vnp_ReturnUrl", vnp_Returnurl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = vnp_Params.get(fieldName);
            if ((value != null) && (!value.isEmpty())) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII)).append('&');
                query.append(fieldName).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII)).append('&');
            }
        }

        String queryString = query.substring(0, query.length() - 1);
        String secureHash = hmacSHA512(vnp_HashSecret, hashData.substring(0, hashData.length() - 1));
        return vnp_Url + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }
public static String hashAllFields(Map<String, String> fields) {
    List<String> fieldNames = new ArrayList<>(fields.keySet());
    Collections.sort(fieldNames);
    StringBuilder hashData = new StringBuilder();
for (int i = 0; i < fieldNames.size(); i++) {
        String fieldName = fieldNames.get(i);
        String fieldValue = fields.get(fieldName);
        if (fieldValue != null && !fieldValue.isEmpty()) {
            hashData.append(fieldName).append("=").append(fieldValue);
            if (i < fieldNames.size() - 1) {
                hashData.append("&");
            }
        }
    }
    return hashData.toString();
}

   public static String hmacSHA512(String key, String data) {
    try {
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
        hmac512.init(secretKey);
        byte[] bytes = hmac512.doFinal(data.getBytes());
        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }
        return hash.toString();
    } catch (Exception ex) {
        throw new RuntimeException("Cannot generate HMAC", ex);
    }
}
    
}