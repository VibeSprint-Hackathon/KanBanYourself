package com.vibesprint.backend.raid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RaidService {

    private final RaidRepository raidRepository;

    public RaidService(RaidRepository raidRepository) {
        this.raidRepository = raidRepository;
    }

    @Transactional(readOnly = true)
    public List<RaidResponse> findAll() {
        return raidRepository.findAllByOrderByIdDesc().stream().map(RaidResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RaidResponse find(long raidId) {
        return RaidResponse.from(findRaid(raidId));
    }

    @Transactional
    public RaidResponse create(CreateRaidRequest request) {
        Raid raid = Raid.draft(
                request.name().trim(),
                request.description().trim(),
                request.maxHp(),
                normalizeReference(request.externalReference())
        );
        return RaidResponse.from(raidRepository.save(raid));
    }

    @Transactional
    public RaidResponse update(long raidId, UpdateRaidRequest request) {
        Raid raid = findRaidForUpdate(raidId);
        if ((raid.getStatus() == RaidStatus.COMPLETED || raid.getStatus() == RaidStatus.CANCELLED)
                && raid.getMaxHp() != request.maxHp()) {
            throw new RaidConflictException("HP cannot be changed for a finished Raid");
        }
        raid.update(
                request.name().trim(),
                request.description().trim(),
                request.maxHp(),
                normalizeReference(request.externalReference())
        );
        return RaidResponse.from(raid);
    }

    @Transactional
    public RaidResponse activate(long raidId) {
        Raid raid = findRaidForUpdate(raidId);
        if (raid.getStatus() != RaidStatus.DRAFT) {
            throw new RaidConflictException("Only a draft Raid can be activated");
        }
        raidRepository.findActiveForUpdate().ifPresent(active -> {
            throw new RaidConflictException("Raid " + active.getId() + " is already active");
        });
        raid.activate();
        try {
            raidRepository.saveAndFlush(raid);
        } catch (DataIntegrityViolationException exception) {
            throw new RaidConflictException("Another Raid is already active");
        }
        return RaidResponse.from(raid);
    }

    @Transactional
    public RaidResponse cancel(long raidId) {
        Raid raid = findRaidForUpdate(raidId);
        if (raid.getStatus() != RaidStatus.DRAFT && raid.getStatus() != RaidStatus.ACTIVE) {
            throw new RaidConflictException("Only a draft or active Raid can be cancelled");
        }
        raid.cancel();
        return RaidResponse.from(raid);
    }

    @Transactional
    public RaidResponse complete(long raidId) {
        Raid raid = findRaidForUpdate(raidId);
        if (raid.getStatus() != RaidStatus.ACTIVE) {
            throw new RaidConflictException("Only an active Raid can be completed");
        }
        raid.complete();
        return RaidResponse.from(raid);
    }

    @Transactional
    public void delete(long raidId) {
        Raid raid = findRaidForUpdate(raidId);
        if (raid.getStatus() != RaidStatus.DRAFT) {
            throw new RaidConflictException("Only a draft Raid can be deleted");
        }
        raidRepository.delete(raid);
    }

    private Raid findRaid(long raidId) {
        return raidRepository.findById(raidId).orElseThrow(() -> new RaidNotFoundException(raidId));
    }

    private Raid findRaidForUpdate(long raidId) {
        return raidRepository.findByIdForUpdate(raidId)
                .orElseThrow(() -> new RaidNotFoundException(raidId));
    }

    private String normalizeReference(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
