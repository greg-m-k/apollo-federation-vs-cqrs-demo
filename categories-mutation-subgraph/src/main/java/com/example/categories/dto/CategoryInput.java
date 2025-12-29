package com.example.categories.dto;

import org.eclipse.microprofile.graphql.Description;

/**
 * Input DTO for creating or updating categories.
 */
@Description("Input for creating or updating a category")
public class CategoryInput {

    @Description("Category name")
    public String name;

    @Description("Optional category description")
    public String description;

    public CategoryInput() {
    }
}
