package com.datn.datn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendNewPassword(String toEmail, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mật khẩu mới của bạn");
        message.setText("Mật khẩu mới của bạn là: " + newPassword);
        mailSender.send(message);
    }
}