package com.example.graphqldemo.repository;

import com.example.graphqldemo.model.Product;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for Product entities.
 * Uses ConcurrentHashMap for thread-safe storage.
 */
@ApplicationScoped
public class ProductRepository {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    /**
     * Get all products.
     *
     * @return list of all products
     */
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    /**
     * Find a product by ID.
     *
     * @param id the product ID
     * @return optional containing the product if found
     */
    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    /**
     * Find all products in a category.
     *
     * @param categoryId the category ID
     * @return list of products in the category
     */
    public List<Product> findByCategory(String categoryId) {
        return products.values().stream()
                .filter(p -> categoryId.equals(p.getCategoryId()))
                .collect(Collectors.toList());
    }

    /**
     * Save a product.
     *
     * @param product the product to save
     * @return the saved product
     */
    public Product save(Product product) {
        products.put(product.getId(), product);
        return product;
    }

    /**
     * Delete all products (for testing).
     */
    public void deleteAll() {
        products.clear();
    }
}
