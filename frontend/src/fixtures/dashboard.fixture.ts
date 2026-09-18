import type { DemoState, ProgressionResult, Quest } from '@/api/demo.types';

// Local reset state from docs/stages/00-contracts/CONTRACTS.md.
export const demoState: DemoState = {
  player: {
    id: 1,
    name: 'Andrei',
    totalXp: 920,
    level: 4,
    nextLevelXp: 1000,
    title: 'Code Adventurer',
    cosmeticKey: 'base',
    characterState: 'coding',
  },
  quests: [
    {
      id: 101,
      title: 'Fix payment validation',
      status: 'IN_PROGRESS',
      xpReward: 180,
      assigneeId: 1,
      externalReference: null,
    },
  ],
  raid: {
    id: 201,
    name: 'Merge Conflict Hydra',
    maxHp: 1000,
    currentHp: 180,
    status: 'ACTIVE',
  },
  nextUnlock: { level: 5, cosmeticKey: 'rare-hoodie', displayName: 'Rare Hoodie' },
};

const completedQuest: Quest = { ...demoState.quests[0]!, status: 'DONE' };
const completedPlayer = {
  ...demoState.player,
  totalXp: 1100,
  level: 5,
  nextLevelXp: 1500,
  title: 'Code Raider',
  cosmeticKey: 'rare-hoodie',
  characterState: 'idle' as const,
};
const completedRaid = { ...demoState.raid, currentHp: 0, status: 'DEFEATED' as const };

export const appliedProgressionResult: ProgressionResult = {
  eventId: 'demo-payment-validation-1',
  applied: true,
  reason: null,
  xpGained: 180,
  raidDamage: 180,
  levelUp: true,
  unlockedCosmetic: { key: 'rare-hoodie', displayName: 'Rare Hoodie' },
  reaction: 'level-up',
  bossDefeated: true,
  quest: completedQuest,
  player: completedPlayer,
  raid: completedRaid,
};

export const repeatedProgressionResult: ProgressionResult = {
  ...appliedProgressionResult,
  applied: false,
  reason: 'ALREADY_COMPLETED',
  xpGained: 0,
  raidDamage: 0,
  levelUp: false,
  unlockedCosmetic: null,
  reaction: null,
};

export interface QuestPresentation {
  description: string;
  progressPercent: number;
  pullRequestLabel: string;
}

const questPresentations: Record<number, QuestPresentation> = {
  101: {
    description: 'Fix server-side validation and cover the payment edge cases.',
    progressPercent: 72,
    pullRequestLabel: '#142 Payment validation',
  },
};

// Screenshot-only presentation data is deliberately separate from the REST DTO.
export const dashboardPresentation = {
  sprintLabel: 'Sprint 04 · Day 12',
  dateLabel: '18 SEP',
  timeLabel: '20:44',
  player: {
    subtitle: 'Full-stack runner',
    status: 'Player ready',
    initials: 'AN',
    sidebarStatus: 'Ready to sprint',
    characterSrc: null as string | null,
    xpProgress: 0.92,
    xpToReward: 80,
  },
  raid: { playerDamage: 0, xpReward: 2500 },
  quests: questPresentations,
  github: { status: 'Connected', syncLabel: 'GitHub synced', commitsToday: 12 },
  activity: { title: 'Ready for the sprint', xpGained: 0, timeLabel: 'Now' },
};

export const questStatusLabels: Record<Quest['status'], string> = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  DONE: 'Completed',
};

export function formatNumber(value: number): string {
  return value.toLocaleString('en-US');
}
