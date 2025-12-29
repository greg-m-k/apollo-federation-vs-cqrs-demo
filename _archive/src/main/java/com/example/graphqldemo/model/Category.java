package com.example.graphqldemo.model;

import org.eclipse.microprofile.graphql.Description;

/**
 * A product category for grouping related products.
 * Demonstrates one-to-many relationships in GraphQL.
 */
@Description("A product category for grouping related products")
public class Category {

    @Description("Unique identifier for the category")
    private String id;

    @Description("Category name (e.g., 'Electronics', 'Books')")
    private String name;

    @Description("Optional description of the category")
    private String description;

    public Category() {
    }

    public Category(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
}
