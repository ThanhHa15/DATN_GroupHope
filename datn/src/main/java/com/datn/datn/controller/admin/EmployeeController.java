package com.datn.datn.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Member;
import com.datn.datn.service.MembersService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    @Autowired
    private MembersService membersService;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Method helper để tránh duplicate code cho pagination
     */
    private void addPaginationData(String keyword, int page, int size, Model model) {
        List<Member> employees = membersService.searchEmployeesWithPagination(keyword, page, size);
        long totalEmployees = membersService.countEmployees(keyword);
        int totalPages = (int) Math.ceil((double) totalEmployees / size);
        int currentPage = Math.max(0, page);
        if (totalPages > 0 && currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }

        model.addAttribute("employees", employees);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalUsers", totalEmployees);
        model.addAttribute("size", size);
        model.addAttribute("hasNext", currentPage < totalPages - 1);
        model.addAttribute("hasPrevious", currentPage > 0);
    }

    @GetMapping
    public String showEmployeeList(@RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        addPaginationData(keyword, page, size, model);
        model.addAttribute("employee", new Member());
        return "views/admin/list";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String keyword,
                            Model model, RedirectAttributes redirectAttributes) {
        Member employee = membersService.getEmployeeById(id);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên");
            return "redirect:/admin/employees";
        }

        addPaginationData(keyword, page, size, model);
        model.addAttribute("employee", employee);
        model.addAttribute("selectedRole", employee.getRole());
        
        return "views/admin/list";
    }

    @PostMapping("/save")
public String saveEmployee(
        @Valid @ModelAttribute("employee") Member member,
        BindingResult result,
        @RequestParam(required = false) String currentPassword,
        @RequestParam(required = false) String newPassword,
        @RequestParam(required = false) String confirmPassword,
        @RequestParam(required = false, defaultValue = "") String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        RedirectAttributes redirectAttributes,
        Model model) {

    boolean isCreate = (member.getId() == null);

    // VALIDATION CHUNG CHO CẢ THÊM MỚI VÀ SỬA
    // 1. Validate số điện thoại - ĐẶT RA NGOÀI, CHẠY CHO CẢ HAI TRƯỜNG HỢP
    if (member.getPhone() != null && !member.getPhone().isBlank()) {
        String phone = member.getPhone().trim();
        
        // FIXED: Chỉ cho phép đúng 10 chữ số, bắt đầu bằng 0
        String phonePattern = "^0[0-9]{9}$"; // ĐÚNG 10 số
        if (!phone.matches(phonePattern)) {
            result.rejectValue("phone", "error.member", 
                "Số điện thoại phải có đúng 10 chữ số và bắt đầu bằng 0");
        } else {
            // Kiểm tra đầu số hợp lệ Việt Nam
            String validPrefixes = "^0(3[2-9]|5[2689]|7[06789]|8[1-9]|9[0-9])[0-9]{7}$";
            if (!phone.matches(validPrefixes)) {
                result.rejectValue("phone", "error.member", 
                    "Đầu số không đúng định dạng Việt Nam");
            }
        }
    } else {
        // Số điện thoại bắt buộc
        result.rejectValue("phone", "error.member", 
            "Số điện thoại không được để trống");
    }

    // 2. Validate email trùng
    if (isCreate) {
        // Kiểm tra email đã tồn tại chưa
        if (membersService.existsByEmail(member.getEmail())) {
            result.rejectValue("email", "error.member", "Email đã tồn tại, vui lòng nhập email khác");
        }
        
        if (newPassword == null || newPassword.isBlank()) {
            result.rejectValue("password", "error.member", "Mật khẩu không được để trống");
        }
    }

    // 3. Validate cập nhật nhân viên
    if (!isCreate) {
        Member existing = membersService.getEmployeeById(member.getId());
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên");
            return "redirect:/admin/employees";
        }

        // Kiểm tra email trùng khi sửa (trừ email của chính nó)
        if (!member.getEmail().equals(existing.getEmail()) && 
            membersService.existsByEmail(member.getEmail())) {
            result.rejectValue("email", "error.member", "Email đã tồn tại, vui lòng nhập email khác");
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.equals(confirmPassword)) {
                result.rejectValue("confirmPassword", "error.member", "Xác nhận mật khẩu không khớp");
            }
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, existing.getPassword())) {
                result.rejectValue("password", "error.member", "Mật khẩu hiện tại không đúng");
            }
        }
    }

    // Nếu có lỗi thì dừng lại, không lưu
    if (result.hasErrors()) {
        // Lấy tất cả lỗi và gửi qua flash message
        StringBuilder errorMessage = new StringBuilder();
        result.getAllErrors().forEach(error -> {
            errorMessage.append(error.getDefaultMessage()).append(". ");
        });
        
        redirectAttributes.addFlashAttribute("error", errorMessage.toString());
        redirectAttributes.addFlashAttribute("employee", member);
        return "redirect:/admin/employees";
    }

    // Nếu không có lỗi mới lưu
    try {
        if (isCreate) {
            member.setPassword(passwordEncoder.encode(newPassword));
            membersService.save(member);
            redirectAttributes.addFlashAttribute("success", "Thêm nhân viên thành công");
        } else {
            Member existing = membersService.getEmployeeById(member.getId());
            
            existing.setFullname(member.getFullname());
            existing.setEmail(member.getEmail());
            existing.setPhone(member.getPhone());
            existing.setBirthday(member.getBirthday() != null ? member.getBirthday() : existing.getBirthday());
            existing.setRole(member.getRole());
            existing.setActive(member.isActive());

            if (newPassword != null && !newPassword.isBlank()) {
                existing.setPassword(passwordEncoder.encode(newPassword));
            }

            membersService.update(existing);
            redirectAttributes.addFlashAttribute("success", "Cập nhật nhân viên thành công");
        }
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Lưu thất bại: " + e.getMessage());
        return "redirect:/admin/employees";
    }

    return "redirect:/admin/employees" + 
           (keyword != null && !keyword.isEmpty() ? "?keyword=" + keyword : "");
}

    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Member employee = membersService.getEmployeeById(id);
            if (employee == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên");
                return "redirect:/admin/employees";
            }

            membersService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("success", "Xóa nhân viên thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Xóa nhân viên thất bại: " + e.getMessage());
        }

        return "redirect:/admin/employees";
    }
}