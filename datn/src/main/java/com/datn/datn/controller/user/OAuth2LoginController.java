package com.datn.datn.controller.user;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.datn.datn.model.Member;
import com.datn.datn.repository.MemberRepository;
import com.datn.datn.SecurityConfig;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class OAuth2LoginController {

        @Autowired
        private MemberRepository memberRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @GetMapping("/user")
        public String getUserInfo(@AuthenticationPrincipal OAuth2User principal,
                        HttpSession session,
                        Model model) {
                Member loggedInUser = (Member) session.getAttribute("loggedInUser");
                if (loggedInUser == null) {
                        return "redirect:/login";
                }

                String email = principal.getAttribute("email");
                String name = principal.getAttribute("name");

                model.addAttribute("user", loggedInUser);
                model.addAttribute("email", email);
                model.addAttribute("name", name);

                return "user/profile";
        }

        @GetMapping("/login-success")
        public String loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
                log.info("Google OAuth2 User Attributes: {}", oauth2User.getAttributes());

                if (oauth2User == null) {
                        redirectAttributes.addFlashAttribute("error", "Đăng nhập với Google thất bại");
                        return "redirect:/login";
                }

                String email = oauth2User.getAttribute("email");
                if (email == null || email.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Không thể lấy email từ Google");
                        return "redirect:/login";
                }

                String name = oauth2User.getAttribute("name") != null ? oauth2User.getAttribute("name")
                                : "Người dùng Google";
                String phone = oauth2User.getAttribute("phone_number");
                if (phone == null)
                        phone = oauth2User.getAttribute("phone");
                phone = phone != null ? phone : "";

                LocalDate birthday = null;
                try {
                        Map<String, Object> birthdayAttributes = oauth2User.getAttribute("birthdays");
                        if (birthdayAttributes != null) {
                                birthday = parseGoogleBirthday(birthdayAttributes);
                        }
                } catch (Exception e) {
                        log.warn("Không thể lấy thông tin birthday từ Google", e);
                }

                try {
                        // Thay đổi cách xử lý findByEmail
                        Optional<Member> optionalMember = memberRepository.findByEmail(email);
                        Member member;

                        if (optionalMember.isEmpty()) {
                                // Tạo mới tài khoản nếu không tồn tại
                                member = Member.builder()
                                                .email(email)
                                                .fullname(name)
                                                .birthday(birthday)
                                                .password(passwordEncoder.encode(generateShortRandomPassword()))
                                                .loginType("GOOGLE")
                                                .phone(phone)
                                                .role("CUSTOMER")
                                                .active(true)
                                                .verified(true)
                                                .build();

                                member = memberRepository.save(member);
                                log.info("Đã tạo mới tài khoản từ Google: {}, SĐT: {}", email, phone);
                        } else {
                                member = optionalMember.get();
                                if (!member.isActive()) {
                                        redirectAttributes.addFlashAttribute("error", "Tài khoản đã bị khóa");
                                        return "redirect:/login";
                                }
                        }

                        session.setAttribute("loggedInUser", member);
                        session.setAttribute("showLoginSuccess", true);
                        return "redirect:/updateif";

                } catch (Exception e) {
                        log.error("Lỗi khi xử lý đăng nhập Google", e);
                        redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống");
                        return "redirect:/login";
                }
        }

        private LocalDate parseGoogleBirthday(Map<String, Object> birthdayAttributes) {
                try {
                        if (birthdayAttributes.containsKey("date")) {
                                Map<String, Integer> date = (Map<String, Integer>) birthdayAttributes.get("date");
                                return LocalDate.of(
                                                date.getOrDefault("year", LocalDate.now().getYear()),
                                                date.getOrDefault("month", 1),
                                                date.getOrDefault("day", 1));
                        }
                } catch (Exception e) {
                        log.warn("Không thể parse thông tin birthday", e);
                }
                return null;
        }

        // ✅ Hàm sinh mật khẩu ngắn gọn (10 ký tự)
        private String generateShortRandomPassword() {
                String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                SecureRandom random = new SecureRandom();
                StringBuilder sb = new StringBuilder(10);
                for (int i = 0; i < 10; i++) {
                        sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
                }
                return sb.toString();
        }
}
