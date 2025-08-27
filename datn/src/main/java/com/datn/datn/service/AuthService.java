package com.datn.datn.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datn.datn.model.Member;
import com.datn.datn.repository.MemberRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final EntityManager entityManager;

    @Transactional
    public void register(String fullname, String phone, LocalDate birthday,
            String email, String rawPassword, String confirmPassword) {

        // Validate password match
        if (!rawPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp");
        }

        // Check if email already exists
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được đăng ký");
        }

        // Generate OTP
        String otp = generateOtp();

        // Create new user with encoded password
        Member user = Member.builder()
                .fullname(fullname)
                .phone(phone)
                .birthday(birthday)
                .email(email)
                .password(passwordEncoder.encode(rawPassword)) // Mã hóa mật khẩu
                .otp(otp)
                .role("CUSTOMER")
                .verified(false)
                .active(true)
                .loginType("SYSTEM")
                .build();

        memberRepository.save(user);
        sendOtpEmail(email, otp);
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        try {
            Member member = entityManager.createQuery(
                    "SELECT m FROM Member m WHERE m.email = :email", Member.class)
                    .setParameter("email", email)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult();

            if (member == null)
                return false;

            // Nếu OTP sai => cho nhập lại, không xóa
            if (!otp.equals(member.getOtp())) {
                return false;
            }

            if (member.getOtpExpiry().isBefore(LocalDateTime.now())) {
                return false; // Hết hạn
            }
            // OTP đúng => xác thực thành công
            member.setVerified(true);
            member.setOtp(null); // clear OTP sau khi thành công
            memberRepository.save(member);
            return true;

        } catch (NoResultException e) {
            log.error("Member not found: {}", email);
            return false;
        } catch (Exception e) {
            log.error("Verification failed for email: {}", email, e);
            throw new RuntimeException("Xác thực thất bại", e);
        }
    }

    public String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    public void sendOtp(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        String otp = member.getOtp(); // ✅ Lấy OTP đã lưu sẵn trong DB
        if (otp == null) {
            otp = generateOtp(); // Nếu DB chưa có OTP thì mới tạo mới
            member.setOtp(otp);
            memberRepository.save(member);
        }

        sendOtpEmail(email, otp); // ✅ Gửi đúng OTP đã lưu trong DB
    }

    private void sendOtpEmail(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            String htmlContent = """
                    <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <h2 style="color: #2d89ef;">HopePhone - Xác thực tài khoản</h2>
                            <p>Xin chào,</p>
                            <p>Chúng tôi đã nhận được yêu cầu xác thực tài khoản của bạn.</p>
                            <p><strong>Mã OTP của bạn là:</strong></p>
                            <h1 style="background-color: #f2f2f2; padding: 10px 20px; border-radius: 5px; color: #e60000; display: inline-block;">%s</h1>
                            <p>Mã này có hiệu lực trong <strong>5 phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                            <br>
                            <p>Trân trọng,<br><strong>Đội ngũ HopePhone</strong></p>
                        </body>
                    </html>
                    """
                    .formatted(otp);

            helper.setTo(email);
            helper.setSubject("HopePhone - Mã OTP xác thực tài khoản");
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", email, e);
            throw new RuntimeException("Lỗi khi gửi email OTP", e);
        }
    }
}