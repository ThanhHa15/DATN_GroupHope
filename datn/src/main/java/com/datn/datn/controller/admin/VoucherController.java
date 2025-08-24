package com.datn.datn.controller.admin;

import java.time.LocalDate;
import java.util.List; // Thêm import List
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Vouchers;
import com.datn.datn.service.VoucherService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/vouchers")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping
    public String listVouchers(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (role == null || !role.equals("ADMIN")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            return "redirect:/access-denied";
        }
        model.addAttribute("voucher", new Vouchers()); // tạo mới form
        model.addAttribute("vouchers", voucherService.findAll()); // danh sách để hiển thị
        return "formVoucher";
    }

    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Vouchers voucher,
            Model model,
            RedirectAttributes redirectAttributes) {
        boolean isEdit = (voucher.getId() != null);
        boolean isCodeDuplicate = voucherService.existsByCode(voucher.getCode());

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
        redirectAttributes.addFlashAttribute("success",
                isEdit ? "Cập nhật voucher thành công!" : "Thêm voucher thành công!");
        return "redirect:/vouchers";
    }

    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            voucherService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa voucher thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Xóa voucher thất bại!");
        }
        return "redirect:/vouchers";
    }

    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable Long id, Model model) {
        Vouchers voucher = voucherService.findById(id);
        model.addAttribute("voucher", voucher);
        model.addAttribute("vouchers", voucherService.findAll());
        return "formVoucher";
    }

    // Đây là phần tìm kiếm
    @GetMapping("/search")
    public String searchVouchers(@RequestParam(required = false) String keyword, Model model) {
        List<Vouchers> vouchers = (keyword != null && !keyword.trim().isEmpty())
                ? voucherService.searchByKeyword(keyword.trim())
                : voucherService.findAll();

        model.addAttribute("voucher", new Vouchers()); // Reset form input
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("keyword", keyword);
        return "formVoucher";
    }

}
