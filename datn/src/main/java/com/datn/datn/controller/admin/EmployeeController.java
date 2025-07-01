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

import com.datn.datn.model.Roles;
import com.datn.datn.model.Users;
import com.datn.datn.repository.RoleRepository;
import com.datn.datn.service.UsersService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    @Autowired
    private UsersService usersService;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public String showEmployeeList(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String keyword,
            Model model) {
        // Users currentUsers = usersService.getLoggedInUser();

        List<Users> employees = (keyword != null && !keyword.trim().isEmpty())
                ? usersService.searchByKeyword(keyword.trim())
                : usersService.getAllEmployees();

        model.addAttribute("employee", new Users());
        model.addAttribute("employees", employees);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("keyword", keyword);
        // model.addAttribute("currentUser", currentUsers);
        return "views/admin/list";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Users employee = usersService.getEmployeeById(id);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên");
            return "redirect:/admin/employees";
        }

        // Users currentUser = usersService.getLoggedInUser();

        List<Integer> selectedRoleIds = employee.getRoleDetails()
                .stream()
                .map(rd -> rd.getRole().getId().intValue())
                .toList();

        model.addAttribute("employee", employee);
        model.addAttribute("employees", usersService.getAllEmployees());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("selectedRoleIds", selectedRoleIds);
        // model.addAttribute("currentUser", currentUser);
        return "views/admin/list";
    }

    @PostMapping("/save")
    public String saveEmployee(
            @Valid @ModelAttribute("employee") Users user,
            BindingResult result,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            @RequestParam(value = "roleIds", required = false) List<Integer> roleIds,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 👉 Xử lý roleIds null
        if (roleIds == null) {
            roleIds = List.of(); // Gán danh sách rỗng để tránh lỗi .iterator()
        }

        if (result.hasErrors()) {
            model.addAttribute("employees", usersService.getAllEmployees());
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("selectedRoleIds", roleIds); // giữ lại giá trị đã chọn
            return "views/admin/list";
        }

        try {
            user.setActivated(true);

            if (user.getId() == null) {
                // THÊM MỚI
                Roles staffRole = roleRepository.findByDescription("Staff");
                usersService.saveUserWithRole(user, List.of(staffRole.getId().intValue()));
                redirectAttributes.addFlashAttribute("success", "Thêm nhân viên thành công");
            } else {
                Users existingUser = usersService.getEmployeeById(user.getId());
                if (existingUser == null) {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên");
                    return "redirect:/admin/employees";
                }

                if (newPassword != null && !newPassword.isBlank()) {
                    if (currentPassword == null || !currentPassword.equals(existingUser.getPassword())) {
                        result.rejectValue("password", "error.user", "Mật khẩu hiện tại không đúng");
                    } else if (!newPassword.equals(confirmPassword)) {
                        result.rejectValue("password", "error.user", "Xác nhận mật khẩu không khớp");
                    } else {
                        user.setPassword(newPassword);
                    }
                } else {
                    user.setPassword(existingUser.getPassword());
                }

                if (result.hasErrors()) {
                    model.addAttribute("employees", usersService.getAllEmployees());
                    model.addAttribute("roles", roleRepository.findAll());
                    model.addAttribute("selectedRoleIds", roleIds);
                    return "views/admin/list";
                }

                // ✅ Lúc này roleIds luôn an toàn (không null)
                usersService.saveUserWithRole(user, roleIds);
                redirectAttributes.addFlashAttribute("success", "Cập nhật nhân viên thành công");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu nhân viên: " + e.getMessage());
        }

        return "redirect:/admin/employees";
    }

    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            usersService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("success", "Xóa nhân viên thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Xóa nhân viên thất bại: " + e.getMessage());
        }

        return "redirect:/admin/employees";
    }
}
