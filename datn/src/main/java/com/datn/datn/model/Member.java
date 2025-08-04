package com.datn.datn.model;

import java.time.LocalDate;

import com.datn.datn.validator.MinAge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fullname", columnDefinition = "NVARCHAR(100)")
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không quá 100 ký tự")
    private String fullname;

    // @NotNull(message = "Ngày sinh không được để trống")
    // @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    // @MinAge(value = 16, message = "Thành viên phải đủ ít nhất 16 tuổi")
    private LocalDate birthday;

    // @NotBlank(message = "Số điện thoại không được để trống")
    // @Pattern(regexp = "^[0-9]{10,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Column(unique = true)
    private String email;

    // @NotBlank(message = "Mật khẩu không được để trống")
    // @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    // @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).+$", message = "Mật khẩu phải
    // chứa ít nhất 1 chữ cái và 1 số")
    private String password;

    @Transient
    // @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @Column(nullable = false)
    private String role; // "CUSTOMER", "STAFF", "ADMIN"

    @Column(nullable = false)
    private boolean active = true;

    private String otp;
    
    @Column(name = "verified", nullable = false, columnDefinition = "TINYINT(1) default 0")
    private Boolean verified = false;

    @Version
    private Integer version;

    @Column(nullable = false)
    private String loginType; 

}