package com.datn.datn.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productID;

    @Column(length = 100, nullable = false)
    private String productName;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @ManyToOne
    @JoinColumn(name = "CategoryID", nullable = false)
    private Category category;

    @Column(length = 255)
    private String imageUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants;
    @Transient
    private List<String> storages;

    public List<String> getStorages() {
        return storages;
    }

    public void setStorages(List<String> storages) {
        this.storages = storages;
    }

}
