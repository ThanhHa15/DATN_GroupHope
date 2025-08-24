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

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    @Autowired
    private MembersService membersService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping
    public String showEmployeeList(@RequestParam(required = false) String keyword, Model model, HttpSession session,
            RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (role == null || !role.equals("ADMIN")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            return "redirect:/access-denied";
        }

        List<Member> employees = (keyword != null && !keyword.trim().isEmpty())
                ? membersService.searchByKeywordAndRoles(keyword.trim(), List.of("ADMIN", "STAFF"))
                : membersService.findByRoles(List.of("ADMIN", "STAFF"));

        model.addAttribute("employee", new Member());
        model.addAttribute("employees", employees);
        model.addAttribute("keyword", keyword);
        return "views/admin/list";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Member employee = membersService.getEmployeeById(id);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên");
            return "redirect:/admin/employees";
        }

        model.addAttribute("employee", employee);
        model.addAttribute("employees", membersService.getAllEmployees());
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
            RedirectAttributes redirectAttributes,
            Model model) {

        boolean isCreate = (member.getId() == null);

        if (isCreate) {
            if (membersService.existsByEmail(member.getEmail())) {
                result.rejectValue("email", "error.member", "Email đã tồn tại");
                model.addAttribute("employees", membersService.findByRoles(List.of("ADMIN", "STAFF")));
                return "views/admin/list";
            }
            if (newPassword == null || newPassword.isBlank()) {
                result.rejectValue("password", "error.member", "Mật khẩu không được để trống");
                model.addAttribute("employees", membersService.findByRoles(List.of("ADMIN", "STAFF")));
                return "views/admin/list";
            }
            member.setPassword(passwordEncoder.encode(newPassword));
            membersService.save(member);
            redirectAttributes.addFlashAttribute("success", "Thêm nhân viên thành công");

        } else {
            Member existing = membersService.getEmployeeById(member.getId());
            if (existing == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên");
                return "redirect:/admin/employees";
            }
            if (member.getBirthday() == null) {
                member.setBirthday(existing.getBirthday());
            }

            if (newPassword != null && !newPassword.isBlank()) {
                if (!newPassword.equals(confirmPassword)) {
                    result.rejectValue("confirmPassword", "error.member", "Xác nhận mật khẩu không khớp");
                    model.addAttribute("employees", membersService.findByRoles(List.of("ADMIN", "STAFF")));
                    return "views/admin/list";
                }
                if (!currentPassword.equals(existing.getPassword())) {
                    result.rejectValue("password", "error.member", "Mật khẩu hiện tại không đúng");
                    model.addAttribute("employees", membersService.findByRoles(List.of("ADMIN", "STAFF")));
                    return "views/admin/list";
                }
                member.setPassword(newPassword);
            } else {
                member.setPassword(existing.getPassword());
            }
            Member exis = membersService.getEmployeeById(member.getId());
            if (existing == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên");
                return "redirect:/admin/employees";
            }

            // Cập nhật các trường cần thiết
            exis.setFullname(member.getFullname());
            exis.setEmail(member.getEmail());
            exis.setPhone(member.getPhone());
            exis.setBirthday(member.getBirthday());
            exis.setRole(member.getRole());
            exis.setActive(member.isActive());
            // exis.setVerified(member.isVerified());
            // ... các field khác như address nếu có

            if (newPassword != null && !newPassword.isBlank()) {
                existing.setPassword(newPassword);
            }

            membersService.update(existing); // Sử dụng entity có version đúng

            redirectAttributes.addFlashAttribute("success", "Cập nhật nhân viên thành công");
        }
        return "redirect:/admin/employees";
    }

    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            membersService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("success", "Xóa nhân viên thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Xóa nhân viên thất bại: " + e.getMessage());
        }

        return "redirect:/admin/employees";
    }
}