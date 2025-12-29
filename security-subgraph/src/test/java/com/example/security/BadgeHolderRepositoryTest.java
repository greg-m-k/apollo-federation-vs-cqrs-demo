package com.example.security;

import com.example.security.model.BadgeHolder;
import com.example.security.model.BadgeHolder.AccessLevel;
import com.example.security.model.BadgeHolder.Clearance;
import com.example.security.repository.BadgeHolderRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BadgeHolderRepository using H2 in-memory database.
 * These tests run without Docker.
 */
@QuarkusTest
class BadgeHolderRepositoryTest {

    @Inject
    BadgeHolderRepository badgeHolderRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        badgeHolderRepository.deleteAll();

        BadgeHolder badge1 = new BadgeHolder("badge-001", "person-001", "B10001",
                AccessLevel.STANDARD, Clearance.NONE);
        BadgeHolder badge2 = new BadgeHolder("badge-002", "person-002", "B10002",
                AccessLevel.RESTRICTED, Clearance.CONFIDENTIAL);
        BadgeHolder badge3 = new BadgeHolder("badge-003", "person-003", "B10003",
                AccessLevel.ALL_ACCESS, Clearance.TOP_SECRET);
        badge3.active = false;

        badgeHolderRepository.persist(badge1);
        badgeHolderRepository.persist(badge2);
        badgeHolderRepository.persist(badge3);
    }

    @Test
    void testFindById() {
        BadgeHolder badgeHolder = badgeHolderRepository.findById("badge-001");

        assertThat(badgeHolder).isNotNull();
        assertThat(badgeHolder.personId).isEqualTo("person-001");
        assertThat(badgeHolder.badgeNumber).isEqualTo("B10001");
        assertThat(badgeHolder.accessLevel).isEqualTo(AccessLevel.STANDARD);
        assertThat(badgeHolder.clearance).isEqualTo(Clearance.NONE);
        assertThat(badgeHolder.active).isTrue();
    }

    @Test
    void testFindById_NotFound() {
        BadgeHolder badgeHolder = badgeHolderRepository.findById("non-existent");

        assertThat(badgeHolder).isNull();
    }

    @Test
    void testListAll() {
        List<BadgeHolder> badgeHolders = badgeHolderRepository.listAll();

        assertThat(badgeHolders).hasSize(3);
        assertThat(badgeHolders).extracting(b -> b.badgeNumber)
                .containsExactlyInAnyOrder("B10001", "B10002", "B10003");
    }

    @Test
    void testFindByPersonId() {
        Optional<BadgeHolder> badgeHolder = badgeHolderRepository.findByPersonId("person-002");

        assertThat(badgeHolder).isPresent();
        assertThat(badgeHolder.get().badgeNumber).isEqualTo("B10002");
        assertThat(badgeHolder.get().accessLevel).isEqualTo(AccessLevel.RESTRICTED);
    }

    @Test
    void testFindByPersonId_NotFound() {
        Optional<BadgeHolder> badgeHolder = badgeHolderRepository.findByPersonId("non-existent-person");

        assertThat(badgeHolder).isEmpty();
    }

    @Test
    void testFindByBadgeNumber() {
        Optional<BadgeHolder> badgeHolder = badgeHolderRepository.findByBadgeNumber("B10003");

        assertThat(badgeHolder).isPresent();
        assertThat(badgeHolder.get().id).isEqualTo("badge-003");
        assertThat(badgeHolder.get().clearance).isEqualTo(Clearance.TOP_SECRET);
    }

    @Test
    void testFindByBadgeNumber_NotFound() {
        Optional<BadgeHolder> badgeHolder = badgeHolderRepository.findByBadgeNumber("INVALID");

        assertThat(badgeHolder).isEmpty();
    }

    @Test
    void testFindByAccessLevel() {
        List<BadgeHolder> standardAccess = badgeHolderRepository.findByAccessLevel(AccessLevel.STANDARD);

        assertThat(standardAccess).hasSize(1);
        assertThat(standardAccess.get(0).badgeNumber).isEqualTo("B10001");
    }

    @Test
    void testFindByAccessLevel_NoResults() {
        List<BadgeHolder> visitorAccess = badgeHolderRepository.findByAccessLevel(AccessLevel.VISITOR);

        assertThat(visitorAccess).isEmpty();
    }

    @Test
    void testFindByClearance() {
        List<BadgeHolder> confidential = badgeHolderRepository.findByClearance(Clearance.CONFIDENTIAL);

        assertThat(confidential).hasSize(1);
        assertThat(confidential.get(0).id).isEqualTo("badge-002");
    }

    @Test
    void testFindByClearance_NoResults() {
        List<BadgeHolder> secret = badgeHolderRepository.findByClearance(Clearance.SECRET);

        assertThat(secret).isEmpty();
    }

    @Test
    void testFindAllActive() {
        List<BadgeHolder> activeBadges = badgeHolderRepository.findAllActive();

        assertThat(activeBadges).hasSize(2);
        assertThat(activeBadges).extracting(b -> b.badgeNumber)
                .containsExactlyInAnyOrder("B10001", "B10002");
        assertThat(activeBadges).allMatch(b -> b.active);
    }

    @Test
    @Transactional
    void testPersistBadgeHolder() {
        BadgeHolder newBadge = new BadgeHolder("badge-004", "person-004", "B10004",
                AccessLevel.VISITOR, Clearance.NONE);
        badgeHolderRepository.persist(newBadge);

        BadgeHolder found = badgeHolderRepository.findById("badge-004");
        assertThat(found).isNotNull();
        assertThat(found.badgeNumber).isEqualTo("B10004");
        assertThat(found.accessLevel).isEqualTo(AccessLevel.VISITOR);
        assertThat(found.active).isTrue();
    }

    @Test
    @Transactional
    void testUpdateBadgeHolder() {
        BadgeHolder badgeHolder = badgeHolderRepository.findById("badge-001");
        badgeHolder.accessLevel = AccessLevel.ALL_ACCESS;
        badgeHolder.clearance = Clearance.SECRET;
        badgeHolderRepository.persist(badgeHolder);

        BadgeHolder updated = badgeHolderRepository.findById("badge-001");
        assertThat(updated.accessLevel).isEqualTo(AccessLevel.ALL_ACCESS);
        assertThat(updated.clearance).isEqualTo(Clearance.SECRET);
    }

    @Test
    @Transactional
    void testDeleteBadgeHolder() {
        boolean deleted = badgeHolderRepository.deleteById("badge-001");

        assertThat(deleted).isTrue();
        assertThat(badgeHolderRepository.findById("badge-001")).isNull();
        assertThat(badgeHolderRepository.listAll()).hasSize(2);
    }

    @Test
    @Transactional
    void testRevokeBadge() {
        BadgeHolder badgeHolder = badgeHolderRepository.findById("badge-001");
        badgeHolder.active = false;
        badgeHolderRepository.persist(badgeHolder);

        BadgeHolder revoked = badgeHolderRepository.findById("badge-001");
        assertThat(revoked.active).isFalse();

        List<BadgeHolder> activeBadges = badgeHolderRepository.findAllActive();
        assertThat(activeBadges).hasSize(1);
    }
}
