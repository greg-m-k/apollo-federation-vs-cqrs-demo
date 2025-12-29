package com.example.security.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Key;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eclipse.microprofile.graphql.Description;

/**
 * BadgeHolder entity - owned by the Security subgraph.
 * Correlates to Person via personId but owns its own security data.
 */
@Entity
@Table(name = "badge_holders")
@Key(fields = @FieldSet("id"))
@Description("A badge holder record in the Security system")
public class BadgeHolder extends PanacheEntityBase {

    @Id
    @org.eclipse.microprofile.graphql.Id
    @Description("Unique identifier for the badge holder record")
    public String id;

    @Column(name = "person_id", nullable = false)
    @Description("Reference to the Person in HR system")
    public String personId;

    @Column(name = "badge_number", nullable = false, unique = true)
    @Description("Physical badge number")
    public String badgeNumber;

    @Column(name = "access_level", nullable = false)
    @Enumerated(EnumType.STRING)
    @Description("Building access level")
    public AccessLevel accessLevel = AccessLevel.STANDARD;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Description("Security clearance level")
    public Clearance clearance = Clearance.NONE;

    @Column(nullable = false)
    @Description("Whether the badge is currently active")
    public boolean active = true;

    public BadgeHolder() {
    }

    public BadgeHolder(String id, String personId, String badgeNumber, AccessLevel accessLevel, Clearance clearance) {
        this.id = id;
        this.personId = personId;
        this.badgeNumber = badgeNumber;
        this.accessLevel = accessLevel;
        this.clearance = clearance;
        this.active = true;
    }

    public enum AccessLevel {
        VISITOR,      // Lobby only
        STANDARD,     // Normal employee areas
        RESTRICTED,   // Secure areas
        ALL_ACCESS    // Full building access
    }

    public enum Clearance {
        NONE,
        CONFIDENTIAL,
        SECRET,
        TOP_SECRET
    }
}
