package com.example.products.repository;

import com.example.products.model.Product;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository for Product entities using Panache.
 */
@ApplicationScoped
public class ProductRepository implements PanacheRepositoryBase<Product, String> {

    /**
     * Find all products in a specific category.
     */
    public List<Product> findByCategoryId(String categoryId) {
        return list("categoryId", categoryId);
    }
}
