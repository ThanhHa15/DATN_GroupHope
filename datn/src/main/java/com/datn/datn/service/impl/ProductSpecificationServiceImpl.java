package com.datn.datn.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datn.datn.model.ProductSpecification;
import com.datn.datn.repository.ProductSpecificationRepository;
import com.datn.datn.service.ProductSpecificationService;

import java.util.List;

@Service
public class ProductSpecificationServiceImpl implements ProductSpecificationService {

    @Autowired
    private ProductSpecificationRepository specRepo;


    public List<ProductSpecification> findAll() {
        return specRepo.findAll();
    }
    @Override
    public List<ProductSpecification> getSpecificationsByProductId(Integer productid) {
        return specRepo.findByProductid(productid);
    }

    @Override
    public void saveSpecification(ProductSpecification spec) {
        specRepo.save(spec);
    }

    @Override
    public void deleteSpecification(Integer specId) {
        specRepo.deleteById(specId);
    }

    public void updateSpecificationValue(Integer specId, String newValue) {
        ProductSpecification s = specRepo.findById(specId).orElseThrow();
        s.setSpecValue(newValue);
        specRepo.save(s);
    }
}
