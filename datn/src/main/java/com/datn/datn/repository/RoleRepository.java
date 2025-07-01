package com.datn.datn.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datn.datn.model.Roles;

public interface RoleRepository extends JpaRepository<Roles, Long> {
    Roles findByDescription(String description); // ✅ Sửa lại đúng với tên entity

}
