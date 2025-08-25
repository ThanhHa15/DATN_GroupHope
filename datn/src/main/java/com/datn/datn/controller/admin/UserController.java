package com.datn.datn.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Member;
import com.datn.datn.service.MembersService;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private MembersService membersService;

    @GetMapping
    public String showEmployeeList(
            @RequestParam(required = false) String keyword, 
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        List<Member> customers;
        long totalUsers;
        
        if (status != null && !status.isEmpty()) {
            // Lọc customers theo trạng thái - CHỈ LẤY KHÁCH HÀNG
            boolean isActive = "active".equals(status);
            
            // Lấy tất cả customers theo status, sau đó filter chỉ lấy USER/CUSTOMER
            List<Member> allMembers = membersService.findCustomersByActive(isActive, keyword);
            customers = allMembers.stream()
                .filter(member -> "USER".equals(member.getRole()) || "CUSTOMER".equals(member.getRole()))
                .collect(java.util.stream.Collectors.toList());
            
            totalUsers = customers.size();
            
            // Xử lý phân trang cho danh sách đã lọc
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, customers.size());
            
            if (startIndex < customers.size()) {
                customers = customers.subList(startIndex, endIndex);
            } else {
                customers = customers.subList(0, 0); // Empty list
            }
        } else {
            // Hiển thị tất cả customers với phân trang - CHỈ LẤY KHÁCH HÀNG
            List<Member> allCustomers = membersService.searchCustomersWithPagination(keyword, page, size);
            customers = allCustomers.stream()
                .filter(member -> "USER".equals(member.getRole()) || "CUSTOMER".equals(member.getRole()))
                .collect(java.util.stream.Collectors.toList());
            
            // Đếm tổng số customers (chỉ USER/CUSTOMER)
            long totalCount = membersService.countCustomers(keyword);
            // Lấy tất cả để filter và đếm chính xác
            List<Member> allForCount = membersService.searchCustomers(keyword);
            totalUsers = allForCount.stream()
                .filter(member -> "USER".equals(member.getRole()) || "CUSTOMER".equals(member.getRole()))
                .count();
        }

        // Tính toán thông tin phân trang
        int totalPages = (int) Math.ceil((double) totalUsers / size);
        int currentPage = page;
        
        // Đảm bảo page không âm
        if (currentPage < 0) {
            currentPage = 0;
        }
        
        // Đảm bảo page không vượt quá tổng số trang
        if (totalPages > 0 && currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }

        model.addAttribute("employee", new Member());
        model.addAttribute("employees", customers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        
        // Thêm thông tin phân trang
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("size", size);
        model.addAttribute("hasNext", currentPage < totalPages - 1);
        model.addAttribute("hasPrevious", currentPage > 0);
        
        return "views/admin/userslist";
    }

    @PostMapping("/toggle/{id}")
    public String toggleUserStatus(@PathVariable("id") Long id, 
                                  RedirectAttributes redirectAttributes) {
        try {
            // Lấy user từ service
            Member member = membersService.getEmployeeById(id);
            if (member == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy khách hàng");
                return "redirect:/admin/users";
            }
            
            // Kiểm tra chỉ cho phép toggle status của khách hàng
            if (!"USER".equals(member.getRole()) && !"CUSTOMER".equals(member.getRole())) {
                redirectAttributes.addFlashAttribute("error", "Không thể thay đổi trạng thái của admin/nhân viên");
                return "redirect:/admin/users";
            }
            
            // Đảo trạng thái active
            member.setActive(!member.isActive());
            membersService.update(member);
            
            redirectAttributes.addFlashAttribute("success", 
                member.isActive() ? "Mở khóa khách hàng thành công" : "Khóa khách hàng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}