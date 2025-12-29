package com.example.security.model;

import io.smallrye.graphql.api.federation.Extends;
import io.smallrye.graphql.api.federation.External;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Key;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;

/**
 * Person entity stub - extends the Person from hr-subgraph.
 * This allows the security-subgraph to add badge data to Person.
 */
@Extends
@Key(fields = @FieldSet("id"))
@Description("Person extended with security badge information")
public class Person {

    @Id
    @External
    @Description("Person ID (from hr-subgraph)")
    private String id;

    public Person() {
    }

    public Person(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
