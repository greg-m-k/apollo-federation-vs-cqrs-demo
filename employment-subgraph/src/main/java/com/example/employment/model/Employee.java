package com.example.employment.model;

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
 * Employee entity - owned by the Employment subgraph.
 * Correlates to Person via personId but owns its own data.
 */
@Entity
@Table(name = "employees")
@Key(fields = @FieldSet("id"))
@Description("An employee record in the Employment system")
public class Employee extends PanacheEntityBase {

    @Id
    @org.eclipse.microprofile.graphql.Id
    @Description("Unique identifier for the employee record")
    public String id;

    @Column(name = "person_id", nullable = false)
    @Description("Reference to the Person in HR system")
    public String personId;

    @Column(nullable = false)
    @Description("Job title")
    public String title;

    @Column(nullable = false)
    @Description("Department name")
    public String department;

    @Column(nullable = false, precision = 10, scale = 2)
    @Description("Annual salary")
    public BigDecimal salary;

    @Column(nullable = false)
    @Description("Whether the employee is currently active")
    public boolean active = true;

    public Employee() {
    }

    public Employee(String id, String personId, String title, String department, BigDecimal salary) {
        this.id = id;
        this.personId = personId;
        this.title = title;
        this.department = department;
        this.salary = salary;
        this.active = true;
    }
}
