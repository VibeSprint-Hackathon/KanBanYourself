package com.vibesprint.backend.raid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "raid")
public class Raid {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "raid_id_generator")
    @SequenceGenerator(name = "raid_id_generator", sequenceName = "raid_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "max_hp", nullable = false)
    private int maxHp;

    @Column(name = "current_hp", nullable = false)
    private int currentHp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RaidStatus status;

    @Column(name = "external_reference")
    private String externalReference;

    protected Raid() {
    }

    public static Raid draft(String name, String description, int maxHp, String externalReference) {
        Raid raid = new Raid();
        raid.name = name;
        raid.description = description;
        raid.maxHp = maxHp;
        raid.currentHp = maxHp;
        raid.status = RaidStatus.DRAFT;
        raid.externalReference = externalReference;
        return raid;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public RaidStatus getStatus() {
        return status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void update(String name, String description, int newMaxHp, String externalReference) {
        this.name = name;
        this.description = description;
        this.externalReference = externalReference;

        if (status == RaidStatus.DRAFT) {
            maxHp = newMaxHp;
            currentHp = newMaxHp;
            return;
        }
        if (status == RaidStatus.ACTIVE) {
            int damageTaken = maxHp - currentHp;
            maxHp = newMaxHp;
            currentHp = Math.max(0, newMaxHp - damageTaken);
            if (currentHp == 0) {
                status = RaidStatus.COMPLETED;
            }
        }
    }

    public void activate() {
        status = RaidStatus.ACTIVE;
        currentHp = maxHp;
    }

    public void cancel() {
        status = RaidStatus.CANCELLED;
    }

    public void complete() {
        currentHp = 0;
        status = RaidStatus.COMPLETED;
    }

    public void applyDamage(int damage) {
        if (damage <= 0) {
            throw new IllegalArgumentException("Raid damage must be positive");
        }
        if (status != RaidStatus.ACTIVE) {
            throw new IllegalStateException("Only an active Raid can receive damage");
        }
        currentHp = Math.max(0, currentHp - damage);
        if (currentHp == 0) {
            status = RaidStatus.COMPLETED;
        }
    }
}
