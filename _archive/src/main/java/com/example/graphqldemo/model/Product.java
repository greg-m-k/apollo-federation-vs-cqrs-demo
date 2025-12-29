package com.example.graphqldemo.model;

import org.eclipse.microprofile.graphql.Description;

/**
 * A product item that can be queried.
 * Core entity for demonstrating GraphQL queries and relationships.
 */
@Description("A product item that can be queried")
public class Product {

    @Description("Unique identifier for the product")
    private String id;

    @Description("Product name")
    private String name;

    @Description("Optional product description")
    private String description;

    @Description("Product price (must be >= 0)")
    private Double price;

    @Description("Category ID this product belongs to")
    private String categoryId;

    public Product() {
    }

    public Product(String id, String name, String description, Double price, String categoryId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}
