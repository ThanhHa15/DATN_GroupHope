package com.datn.datn.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;

import com.datn.datn.model.Member;
import com.datn.datn.repository.MemberRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository userRepo;
    private final JavaMailSender mailSender;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthService.class);
    private final EntityManager entityManager;

    public void register(String fullname, String phone, LocalDate birthday, String email, String password,
            String confirmPassword) {
        String otp = String.valueOf(new Random().nextInt(899999) + 100000);
        Member existing = userRepo.findByEmail(email);
        Member user = existing != null ? existing : Member.builder().email(email).build();
        user.setFullname(fullname);
        user.setPhone(phone);
        user.setBirthday(birthday);
        user.setPassword(password);
        user.setOtp(otp);
        user.setRole("CUSTOMER"); // Mặc định là CUSTOMER, có thể thay đổi sau
        user.setConfirmPassword(confirmPassword);
        user.setVerified(false);
        userRepo.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("OTP xác minh");
        message.setText("Mã OTP của bạn là: " + otp);
        mailSender.send(message);
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        try {
            // Sử dụng native query để tránh cache Hibernate
            Member member = entityManager.createQuery(
                    "SELECT m FROM Member m WHERE m.email = :email", Member.class)
                    .setParameter("email", email)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult();

            if (member == null || !otp.equals(member.getOtp())) {
                return false;
            }

            // Cập nhật trực tiếp bằng native query
            int updated = entityManager.createNativeQuery(
                    "UPDATE members SET verified = 1, otp = NULL WHERE email = ?")
                    .setParameter(1, email)
                    .executeUpdate();

            entityManager.flush();
            entityManager.clear(); // Clear cache

            logger.info("Updated {} records for email: {}", updated, email);
            return updated > 0;

        } catch (NoResultException e) {
            logger.error("Member not found: {}", email);
            return false;
        } catch (Exception e) {
            logger.error("Verification failed", e);
            throw new RuntimeException("Xác thực thất bại", e);
        }
    }

    public String generateOtp() {
        // Tạo mã OTP 6 chữ số ngẫu nhiên
        Random random = new Random();
        int otpNumber = 100000 + random.nextInt(900000);
        return String.valueOf(otpNumber);
    }

    public void sendOtp(String email, String otp) {
        Member member = userRepo.findByEmail(email); // Đổi memberRepository thành userRepo
        if (member != null) {
            member.setOtp(otp);
            userRepo.save(member); // Đổi memberRepository thành userRepo
        }

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
            helper.setText(htmlContent, true); // Gửi dưới dạng HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi khi gửi email OTP", e);
        }

    }
    // public void sendOtp(String email) {
    // // Tạo và lưu mã OTP, sau đó gửi email/sms
    // }

}