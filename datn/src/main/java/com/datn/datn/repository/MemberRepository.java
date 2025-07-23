package com.datn.datn.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.datn.model.Member;

import jakarta.persistence.LockModeType;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Đăng nhập bằng SDT hoặc Email
    @Query("SELECT m FROM Member m WHERE (m.phone = :input OR m.email = :input) AND m.password = :password")
    Optional<Member> loginByPhoneOrEmail(@Param("input") String input, @Param("password") String password);

    List<Member> findByRoleIn(List<String> roles);

    List<Member> findByFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(String nameKeyword, String emailKeyword);

    List<Member> findByRoleInAndFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            List<String> roles, String nameKeyword, String emailKeyword);

    Member findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.email = :email")
    Member findByEmailForUpdate(@Param("email") String email);
}