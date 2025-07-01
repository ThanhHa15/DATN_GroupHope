package com.datn.datn.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datn.datn.model.RoleDetail;
import com.datn.datn.model.Users;
import com.datn.datn.repository.RoleDetailRepository;
import com.datn.datn.repository.RoleRepository;
import com.datn.datn.repository.UsersRepository;
import com.datn.datn.service.UsersService;

@Service
@Transactional
public class UsersServiceImpl implements UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RoleDetailRepository roleDetailRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Override
    public Users login(String email, String password) {
        return usersRepository.findByEmailAndPassword(email, password);
    }

    @Override
    public Users save(Users user) {
        return usersRepository.save(user);
    }

    @Override
    public Users findByEmail(String email) {
        return usersRepository.findByEmail(email);
    }

    @Override
    public List<Users> getAllEmployees() {
        return usersRepository.findAllStaffAndAdmin();
    }

    @Override
    public Users getEmployeeById(Integer id) {
        return usersRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteEmployee(Integer id) {
        roleDetailRepo.deleteByUserId(id); // xóa quyền trước
        usersRepository.deleteById(id);
    }

    @Override
    public List<Users> searchByKeyword(String keyword) {
        return usersRepository.findByFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);
    }

    @Override
    public List<Users> findAll() {
        return usersRepository.findAll();
    }

    @Override
    public Users findById(int id) {
        return usersRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(int id) {
        roleDetailRepo.deleteByUserId(id); // xóa quyền trước
        usersRepository.deleteById(id);
    }

    @Override
    public List<Users> searchByName(String keyword) {
        return usersRepository.findByFullnameContainingIgnoreCase(keyword);
    }

    @Override
    public void saveUserWithRole(Users user, List<Integer> roleIds) {
        if (user.getId() != null) {
            roleDetailRepo.deleteByUserId(user.getId());
        }

        // Lưu user trước để lấy id
        usersRepository.save(user);

        // Gán quyền mới
        for (Integer roleId : roleIds) {
            RoleDetail rd = new RoleDetail();
            rd.setUser(user);
            rd.setRole(roleRepo.findById(Long.valueOf(roleId)).orElse(null));
            roleDetailRepo.save(rd);
        }
    }

}
