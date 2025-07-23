package com.datn.datn.controller.admin;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.time.LocalDate;
import com.datn.datn.model.Vouchers;
import com.datn.datn.service.VoucherService;

@Controller
@RequestMapping("/vouchers")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping
    public String listVouchers(Model model) {
        model.addAttribute("voucher", new Vouchers()); // tạo mới form
        model.addAttribute("vouchers", voucherService.findAll()); // danh sách để hiển thị
        return "formVoucher";
    }

    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Vouchers voucher, Model model) {
        boolean isEdit = (voucher.getId() != null);
        boolean isCodeDuplicate = voucherService.existsByCode(voucher.getCode());

        // Validate ngày bắt đầu và ngày kết thúc
        LocalDate today = LocalDate.now();
        LocalDate start = voucher.getStartDate();
        LocalDate end = voucher.getEndDate();

        if (end.isBefore(today)) {
            model.addAttribute("voucher", voucher);
            model.addAttribute("vouchers", voucherService.findAll());
            model.addAttribute("error", "Ngày kết thúc không được ở quá khứ!");
            return "formVoucher";
        }

        if (start.isAfter(end)) {
            model.addAttribute("voucher", voucher);
            model.addAttribute("vouchers", voucherService.findAll());
            model.addAttribute("error", "Ngày bắt đầu không được sau ngày kết thúc!");
            return "formVoucher";
        }

        // Kiểm tra mã voucher trùng
        if (!isEdit && isCodeDuplicate) {
            model.addAttribute("voucher", voucher);
            model.addAttribute("vouchers", voucherService.findAll());
            model.addAttribute("error", "Mã voucher đã tồn tại!");
            return "formVoucher";
        }

        if (isEdit) {
            Vouchers existing = voucherService.findById(voucher.getId());
            if (existing != null && !existing.getCode().equals(voucher.getCode()) && isCodeDuplicate) {
                model.addAttribute("voucher", voucher);
                model.addAttribute("vouchers", voucherService.findAll());
                model.addAttribute("error", "Mã voucher đã tồn tại!");
                return "formVoucher";
            }
        }

        voucherService.save(voucher);
        return "redirect:/vouchers";
    }

    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Long id) {
        voucherService.delete(id);
        return "redirect:/vouchers";
    }

    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable Long id, Model model) {
        Vouchers voucher = voucherService.findById(id);
        model.addAttribute("voucher", voucher);
        model.addAttribute("vouchers", voucherService.findAll());
        return "formVoucher";
    }
}
