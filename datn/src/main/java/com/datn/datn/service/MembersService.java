package com.datn.datn.service;

import java.util.List;

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
    Member findByEmail(String email);
}

