package com.datn.datn.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private MembersService membersService;

    @GetMapping
    public String showEmployeeList(@RequestParam(required = false) String keyword, Model model) {
        List<Member> employees = (keyword != null && !keyword.trim().isEmpty())
                ? membersService.searchByKeywordAndRoles(keyword.trim(), List.of("CUSTOMER"))
                : membersService.findByRoles(List.of("CUSTOMER"));

        model.addAttribute("employee", new Member());
        model.addAttribute("employees", employees);
        model.addAttribute("keyword", keyword);
        return "userslist";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Member employee = membersService.getEmployeeById(id);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng");
            return "redirect:/userslist";
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
                model.addAttribute("employees", membersService.findByRoles(List.of("CUSTOMER")));
                return "userslist";
            }
            if (newPassword == null || newPassword.isBlank()) {
                result.rejectValue("password", "error.member", "Mật khẩu không được để trống");
                model.addAttribute("employees", membersService.findByRoles(List.of("CUSTOMER")));
                return "userslist";
            }
            member.setPassword(newPassword);
            membersService.save(member);
            redirectAttributes.addFlashAttribute("success", "Thêm người dùng thành công");

        } else {
            Member existing = membersService.getEmployeeById(member.getId());
            if (existing == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng vui lòng nhập lại");
                return "redirect:/userslist";
            }
            if (newPassword != null && !newPassword.isBlank()) {
                if (!newPassword.equals(confirmPassword)) {
                    result.rejectValue("confirmPassword", "error.member", "Xác nhận mật khẩu không khớp");
                    model.addAttribute("employees", membersService.findByRoles(List.of("ADMIN", "STAFF")));
                    return "userslist";
                }
                if (!currentPassword.equals(existing.getPassword())) {
                    result.rejectValue("password", "error.member", "Mật khẩu hiện tại không đúng");
                    model.addAttribute("employees", membersService.findByRoles(List.of("ADMIN", "STAFF")));
                    return "userslist";
                }
                member.setPassword(newPassword);
            } else {
                member.setPassword(existing.getPassword());
            }
            membersService.update(member);
            redirectAttributes.addFlashAttribute("success", "Cập nhật người dùng thành công");
        }
        return "redirect:/userslist";
    }

    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            membersService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("success", "Xóa người dùng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Xóa người dùng thất bại: " + e.getMessage());
        }

        return "redirect:/userslist";
    }

    @PostMapping("/toggle/{id}")
    public String toggleAccountStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Member member = membersService.getEmployeeById(id);
            if (member == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng");
                return "redirect:/users";
            }

            member.setActive(!member.isActive()); // Đảo trạng thái true <=> false
            membersService.update(member);

            String action = member.isActive() ? "Mở khóa" : "Khóa";
            redirectAttributes.addFlashAttribute("success", action + " tài khoản thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi cập nhật tài khoản");
        }

        return "redirect:/admin/users";
    }

}
