package com.example.products.dto;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Name;

import java.math.BigDecimal;

/**
 * Input DTO for creating or updating products.
 * Named CreateProduct so SmallRye generates "CreateProductInput" in the schema.
 */
@Description("Input for creating or updating a product")
public class CreateProduct {

    @Description("Product name")
    public String name;

    @Description("Optional product description")
    public String description;

    @Description("Product price")
    public BigDecimal price;

    @Name("categoryId")
    @Description("Category ID for the product")
    public String categoryId;

    public CreateProduct() {
    }
}
