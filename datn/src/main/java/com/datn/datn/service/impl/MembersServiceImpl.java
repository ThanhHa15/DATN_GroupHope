package com.datn.datn.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datn.datn.model.Member;
import com.datn.datn.repository.MemberRepository;
import com.datn.datn.service.MembersService;

@Service
@Transactional
public class MembersServiceImpl implements MembersService {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public List<Member> getAllEmployees() {
        return memberRepository.findByRoleIn(List.of("ADMIN", "STAFF"));
    }

    @Override
    public List<Member> searchByKeyword(String keyword) {
        return memberRepository.findByFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);
    }

    @Override
    public List<Member> searchByKeywordAndRoles(String keyword, List<String> roles) {
        return memberRepository.findByRoleInAndFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                roles, keyword, keyword);
    }

    @Override
    public List<Member> findByRoles(List<String> roles) {
        return memberRepository.findByRoleIn(roles);
    }

    @Override
    public Member getEmployeeById(Long id) {
        return memberRepository.findById(id).orElse(null);
    }

    @Override
    public void save(Member member) {
        memberRepository.save(member);
    }

    @Override
    public void update(Member member) {
        memberRepository.save(member);
    }

    @Override
    public void deleteEmployee(Long id) {
        memberRepository.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    @Override
    public List<Member> findByActive(boolean active, String keyword) {
        return memberRepository.findByActive(active, keyword);
    }

    @Override
    public List<Member> searchEmployeesWithPagination(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return memberRepository.searchEmployeesWithPagination(List.of("ADMIN", "STAFF"), keyword, pageable);
    }

    @Override
    public long countEmployees(String keyword) {
        return memberRepository.countEmployeesByKeyword(List.of("ADMIN", "STAFF"), keyword);
    }

    @Override
    public List<Member> searchCustomers(String keyword) {
        return memberRepository.searchCustomers(keyword);
    }

    @Override
    public List<Member> searchCustomersWithPagination(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return memberRepository.searchCustomersWithPagination(keyword.trim(), pageable);
        } else {
            return memberRepository.findAllCustomersWithPagination(pageable);
        }
    }

    @Override
    public long countCustomers(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return memberRepository.countCustomersByKeyword(keyword.trim());
        } else {
            return memberRepository.countAllCustomers();
        }
    }

    @Override
    public List<Member> findCustomersByActive(boolean active, String keyword) {
        return memberRepository.findByActive(active, keyword);
    }
}