package com.datn.datn.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.datn.model.Member;

import jakarta.persistence.LockModeType;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Đăng nhập bằng SDT hoặc Email
    @Query("SELECT m FROM Member m WHERE (m.phone = :input OR m.email = :input) AND m.password = :password")
    Optional<Member> loginByPhoneOrEmail(@Param("input") String input, @Param("password") String password);

    List<Member> findByRoleIn(List<String> roles);

    List<Member> findByFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(String nameKeyword, String emailKeyword);

    List<Member> findByRoleInAndFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            List<String> roles, String nameKeyword, String emailKeyword);

    Optional<Member> findByEmail(String email); // Thay đổi kiểu trả về

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.email = :email")
    Member findByEmailForUpdate(@Param("email") String email);

    // Optional<Member> findByEmailOrPhone(String email, String phone);

    @Query("SELECT m FROM Member m WHERE m.email = :email OR m.phone = :phone")
    Optional<Member> findByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);

    List<Member> findByActive(boolean active);

    // Tìm người dùng theo trạng thái active và keyword
    @Query("SELECT m FROM Member m WHERE (:keyword IS NULL OR m.fullname LIKE %:keyword%) AND m.active = :active")
    List<Member> findByActive(@Param("active") boolean active, @Param("keyword") String keyword);

    // Tìm người dùng theo keyword, không xét trạng thái
    @Query("SELECT m FROM Member m WHERE :keyword IS NULL OR m.fullname LIKE %:keyword%")
    List<Member> searchUsers(@Param("keyword") String keyword);

    // Thêm các method phân trang cho toàn bộ users
    @Query("SELECT m FROM Member m WHERE :keyword IS NULL OR m.fullname LIKE %:keyword% OR m.email LIKE %:keyword%")
    List<Member> searchUsersWithPagination(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT m FROM Member m")
    List<Member> findAllUsersWithPagination(Pageable pageable);

    @Query("SELECT COUNT(m) FROM Member m WHERE :keyword IS NULL OR m.fullname LIKE %:keyword% OR m.email LIKE %:keyword%")
    long countUsersByKeyword(@Param("keyword") String keyword);

    @Query("SELECT COUNT(m) FROM Member m")
    long countAllUsers();
// Phân trang cho nhân viên (ADMIN, STAFF) với keyword tùy chọn
    @Query("SELECT m FROM Member m WHERE m.role IN :roles AND (:keyword IS NULL OR m.fullname LIKE %:keyword% OR m.email LIKE %:keyword%)")
    List<Member> searchEmployeesWithPagination(@Param("roles") List<String> roles, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.role IN :roles AND (:keyword IS NULL OR m.fullname LIKE %:keyword% OR m.email LIKE %:keyword%)")
    long countEmployeesByKeyword(@Param("roles") List<String> roles, @Param("keyword") String keyword);

    //mới 
    // Thêm queries mới cho customers
    @Query("SELECT m FROM Member m WHERE m.role IN ('USER', 'CUSTOMER') AND (:keyword IS NULL OR m.fullname LIKE %:keyword% OR m.email LIKE %:keyword%)")
    List<Member> searchCustomers(@Param("keyword") String keyword);
    
    @Query("SELECT m FROM Member m WHERE m.role IN ('USER', 'CUSTOMER') AND (:keyword IS NULL OR m.fullname LIKE %:keyword% OR m.email LIKE %:keyword%)")
    List<Member> searchCustomersWithPagination(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT m FROM Member m WHERE m.role IN ('USER', 'CUSTOMER')")
    List<Member> findAllCustomersWithPagination(Pageable pageable);
    
    @Query("SELECT COUNT(m) FROM Member m WHERE m.role IN ('USER', 'CUSTOMER') AND (:keyword IS NULL OR m.fullname LIKE %:keyword% OR m.email LIKE %:keyword%)")
    long countCustomersByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(m) FROM Member m WHERE m.role IN ('USER', 'CUSTOMER')")
    long countAllCustomers();
    
    @Query("SELECT m FROM Member m WHERE m.role IN ('USER', 'CUSTOMER') AND (:keyword IS NULL OR m.fullname LIKE %:keyword%) AND m.active = :active")
    List<Member> findCustomersByActive(@Param("active") boolean active, @Param("keyword") String keyword);

    // Đếm tổng số khách hàng (CUSTOMER và active = true)
    @Query("SELECT COUNT(m) FROM Member m WHERE m.role = 'CUSTOMER' AND m.active = true")
    long countTotalCustomers();

    // Đếm số khách hàng mới (trong 30 ngày gần đây)
    // Nếu không có createdDate, có thể dùng cách khác hoặc bỏ qua
    @Query("SELECT COUNT(m) FROM Member m WHERE m.role = 'CUSTOMER' AND m.active = true")
    long countNewCustomers();

}