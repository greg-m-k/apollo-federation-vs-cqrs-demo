package com.example.categories.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Key;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eclipse.microprofile.graphql.Description;

/**
 * Category entity - owned by this subgraph.
 */
@Entity
@Table(name = "categories")
@Key(fields = @FieldSet("id"))
@Description("A product category for grouping related products")
public class Category extends PanacheEntityBase {

    @Id
    @org.eclipse.microprofile.graphql.Id
    @Description("Unique identifier for the category")
    public String id;

    @Column(nullable = false, unique = true)
    @Description("Category name")
    public String name;

    @Description("Optional description of the category")
    public String description;

    public Category() {
    }

    public Category(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
