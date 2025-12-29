package com.example.graphqldemo.graphql;

import com.example.graphqldemo.model.Category;
import com.example.graphqldemo.model.Product;
import com.example.graphqldemo.repository.CategoryRepository;
import com.example.graphqldemo.repository.ProductRepository;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.List;

/**
 * GraphQL API resource for Products and Categories.
 * Provides queries for listing and retrieving entities.
 */
@GraphQLApi
public class ProductGraphQL {

    @Inject
    ProductRepository productRepository;

    @Inject
    CategoryRepository categoryRepository;

    // ==================== Product Queries ====================

    @Query("products")
    @Description("Get all products. Returns a list of all available products with their details.")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Query("product")
    @Description("Get a single product by ID. Returns null if the product is not found.")
    public Product getProduct(@Name("id") @Description("The product ID") String id) {
        return productRepository.findById(id).orElse(null);
    }

    // ==================== Category Queries ====================

    @Query("categories")
    @Description("Get all categories. Returns a list of all product categories.")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Query("category")
    @Description("Get a single category by ID. Returns null if the category is not found.")
    public Category getCategory(@Name("id") @Description("The category ID") String id) {
        return categoryRepository.findById(id).orElse(null);
    }

    // ==================== Field Resolvers ====================

    @Description("List of products belonging to this category")
    public List<Product> products(@Source Category category) {
        return productRepository.findByCategory(category.getId());
    }

    @Description("Category this product belongs to")
    public Category category(@Source Product product) {
        if (product.getCategoryId() == null) {
            return null;
        }
        return categoryRepository.findById(product.getCategoryId()).orElse(null);
    }
}
