package com.datn.datn.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.datn.datn.model.Users;

@Service
public interface UsersService {
    Users login(String email, String password);

    Users save(Users user);

    Users findByEmail(String email);

    List<Users> getAllEmployees();

    Users getEmployeeById(Integer id);

    void deleteEmployee(Integer id);

    List<Users> searchByKeyword(String keyword);

    List<Users> findAll();

    Users findById(int id);

    void deleteById(int id);

    List<Users> searchByName(String keyword);

    void saveUserWithRole(Users user, List<Integer> roleIds);

    // Users getLoggedInUser();

}
