package com.datn.datn.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

}
