import type { RaidPresentation, RaidPreviewState } from '@/components/raids/raid.types';

const activeRaid: RaidPresentation = {
  raid: {
    id: 401,
    name: 'The Legacy Hydra',
    status: 'ACTIVE',
    currentHp: 68_400,
    maxHp: 100_000,
  },
  playerDamage: 7_850,
  rewardXp: 2_500,
  completedAt: null,
};

export const raidFixtures: Record<Exclude<RaidPreviewState, 'EMPTY'>, RaidPresentation> = {
  ACTIVE: activeRaid,
  DAMAGED: {
    ...activeRaid,
    raid: { ...activeRaid.raid, currentHp: 60_550 },
  },
  DEFEATED: {
    ...activeRaid,
    raid: { ...activeRaid.raid, status: 'DEFEATED', currentHp: 0 },
    completedAt: 'Today',
  },
};

export const raidDamageFixture = {
  previousHp: 68_400,
  damageReceived: 7_850,
} as const;

export const completedRaidFixtures: RaidPresentation[] = [
  {
    raid: { id: 402, name: 'Code Golem', status: 'DEFEATED', currentHp: 0, maxHp: 72_000 },
    rewardXp: 1_800,
    playerDamage: 12_600,
    completedAt: '12 Sep',
  },
  {
    raid: { id: 403, name: 'Merge Kraken', status: 'DEFEATED', currentHp: 0, maxHp: 85_000 },
    rewardXp: 2_100,
    playerDamage: 9_420,
    completedAt: '05 Sep',
  },
];

// Change this single value when a non-interactive local preview is preferred.
export const initialRaidPreviewState: RaidPreviewState = 'ACTIVE';

export const raidPagePresentation = {
  sprintLabel: 'Sprint 04 · Day 12',
  dateLabel: '18 SEP',
  timeLabel: '20:44',
  syncLabel: 'GitHub synced',
};
