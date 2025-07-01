package com.datn.datn.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.datn.model.RoleDetail;

public interface RoleDetailRepository extends JpaRepository<RoleDetail, Integer> {
    List<RoleDetail> findByUserId(Integer userId);

    void deleteByUserId(Integer userId);
}
