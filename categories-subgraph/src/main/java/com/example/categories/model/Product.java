package com.example.categories.model;

import io.smallrye.graphql.api.federation.Extends;
import io.smallrye.graphql.api.federation.External;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Key;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;

/**
 * Product entity stub - extends the Product from products-subgraph.
 * This allows the categories-subgraph to add the 'category' field to Product.
 */
@Extends
@Key(fields = @FieldSet("id"))
@Description("Product extended with category information")
public class Product {

    @Id
    @External
    @Description("Product ID (from products-subgraph)")
    private String id;

    @External
    @Description("Category ID (from products-subgraph)")
    private String categoryId;

    public Product() {
    }

    public Product(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}
