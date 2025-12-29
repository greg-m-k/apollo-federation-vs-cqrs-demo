package com.example.products.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Key;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eclipse.microprofile.graphql.Description;

import java.math.BigDecimal;

/**
 * Product entity - owned by this subgraph.
 * The @Key annotation marks this as a federation entity.
 */
@Entity
@Table(name = "products")
@Key(fields = @FieldSet("id"))
@Description("A product item that can be queried")
public class Product extends PanacheEntityBase {

    @Id
    @org.eclipse.microprofile.graphql.Id
    @Description("Unique identifier for the product")
    public String id;

    @Column(nullable = false)
    @Description("Product name")
    public String name;

    @Description("Optional product description")
    public String description;

    @Column(nullable = false, precision = 10, scale = 2)
    @Description("Product price")
    public BigDecimal price;

    @Column(name = "category_id")
    @Description("Category ID (resolved by Categories subgraph)")
    public String categoryId;

    public Product() {
    }

    public Product(String id, String name, String description, BigDecimal price, String categoryId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
    }

    // Convenience method for GraphQL queries that return Double
    public Double getPriceAsDouble() {
        return price != null ? price.doubleValue() : null;
    }
}
