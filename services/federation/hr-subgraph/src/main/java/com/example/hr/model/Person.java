package com.example.hr.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Key;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eclipse.microprofile.graphql.Description;

import java.time.LocalDate;

/**
 * Person entity - owned by the HR subgraph.
 * This is the canonical source for person/identity data.
 */
@Entity
@Table(name = "persons")
@Key(fields = @FieldSet("id"))
@Description("A person in the HR system - the canonical source of identity")
public class Person extends PanacheEntityBase {

    @Id
    @org.eclipse.microprofile.graphql.Id
    @Description("Unique identifier for the person")
    public String id;

    @Column(nullable = false)
    @Description("Person's full name")
    public String name;

    @Column(nullable = false, unique = true)
    @Description("Person's email address")
    public String email;

    @Column(name = "hire_date")
    @Description("Date the person was hired")
    public LocalDate hireDate;

    @Column(nullable = false)
    @Description("Whether the person is currently active")
    public boolean active = true;

    public Person() {
    }

    public Person(String id, String name, String email, LocalDate hireDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.hireDate = hireDate;
        this.active = true;
    }
}
