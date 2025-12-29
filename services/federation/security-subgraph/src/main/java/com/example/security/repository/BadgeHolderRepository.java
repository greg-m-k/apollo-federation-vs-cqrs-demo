package com.example.security.repository;

import com.example.security.model.BadgeHolder;
import com.example.security.model.BadgeHolder.AccessLevel;
import com.example.security.model.BadgeHolder.Clearance;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BadgeHolder entities using Panache.
 */
@ApplicationScoped
public class BadgeHolderRepository implements PanacheRepositoryBase<BadgeHolder, String> {

    /**
     * Find a badge holder by person ID.
     */
    public Optional<BadgeHolder> findByPersonId(String personId) {
        return find("personId", personId).firstResultOptional();
    }

    /**
     * Find a badge holder by badge number.
     */
    public Optional<BadgeHolder> findByBadgeNumber(String badgeNumber) {
        return find("badgeNumber", badgeNumber).firstResultOptional();
    }

    /**
     * Find all badge holders by access level.
     */
    public List<BadgeHolder> findByAccessLevel(AccessLevel accessLevel) {
        return find("accessLevel", accessLevel).list();
    }

    /**
     * Find all badge holders by clearance level.
     */
    public List<BadgeHolder> findByClearance(Clearance clearance) {
        return find("clearance", clearance).list();
    }

    /**
     * Find all active badge holders.
     */
    public List<BadgeHolder> findAllActive() {
        return find("active", true).list();
    }
}
