package com.datn.datn.controller.admin;

import org.hibernate.query.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class OrderAdminController {

    // Add methods to handle order management here

    @GetMapping("/admin-order")
    public String listOrders() {
        return "formOrder";
    }
    @GetMapping("/admin-orderdetail")
    public String viewOrderDetail() {
        return "formOrder-Detail";
    }

}
