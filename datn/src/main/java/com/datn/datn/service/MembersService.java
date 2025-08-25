package com.datn.datn.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.datn.datn.model.Member;

@Service
public interface MembersService {

    List<Member> getAllEmployees();

    List<Member> searchByKeyword(String keyword);

    List<Member> searchByKeywordAndRoles(String keyword, List<String> roles);

    List<Member> findByRoles(List<String> roles);

    Member getEmployeeById(Long id);

    void save(Member member);

    void update(Member member);

    void deleteEmployee(Long id);

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    List<Member> findByActive(boolean active, String keyword);

    //

    // Employees pagination
    List<Member> searchEmployeesWithPagination(String keyword, int page, int size);

    long countEmployees(String keyword);

    List<Member> searchCustomers(String keyword);

    List<Member> searchCustomersWithPagination(String keyword, int page, int size);

    long countCustomers(String keyword);

    List<Member> findCustomersByActive(boolean active, String keyword);

}