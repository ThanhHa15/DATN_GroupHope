package com.datn.datn.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.datn.datn.model.Users;

public interface UsersRepository extends JpaRepository<Users, Integer> {
        Users findByEmailAndPassword(String email, String password);

        Users findByEmail(String email);

        List<Users> findByActivated(Boolean activated);

        @Query("SELECT u FROM Users u WHERE " +
                        "LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Users> searchEmployees(@Param("keyword") String keyword);

        @Query("SELECT u FROM Users u JOIN u.roleDetails rd JOIN rd.role r WHERE " +
                        "r.description = 'Staff' OR r.description = 'Administrator'")
        List<Users> findAllStaffAndAdmin();

        List<Users> findByFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
        List<Users> findByFullnameContainingIgnoreCase(String keyword);

}