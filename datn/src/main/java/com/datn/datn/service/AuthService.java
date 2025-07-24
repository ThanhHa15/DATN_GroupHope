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
                    "UPDATE members SET verified = true, otp = NULL WHERE email = :email AND otp = :otp")
                    .setParameter("email", email)
                    .setParameter("otp", otp)
                    .executeUpdate();

            // Đảm bảo thay đổi được flush ngay lập tức
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
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setTo(email);
            helper.setSubject("Mã OTP xác thực tài khoản");
            helper.setText("Mã OTP của bạn là: " + otp + ". Mã có hiệu lực trong 5 phút.");
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi khi gửi email OTP", e);
        }
    }
    // public void sendOtp(String email) {
    // // Tạo và lưu mã OTP, sau đó gửi email/sms
    // }

}