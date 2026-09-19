import {
  mdiCreationOutline,
  mdiLightningBoltOutline,
  mdiShieldCheckOutline,
  mdiSignDirection,
  mdiSourceBranch,
  mdiTarget,
} from '@quasar/extras/mdi-v7';
import type {
  Achievement,
  AchievementSummaryState,
  AchievementUnlockResult,
} from '@/components/achievements/achievement.types';

export const achievementSummaryFixture: AchievementSummaryState = {
  unlocked: 12,
  total: 18,
};

export const achievementFixtures: Achievement[] = [
  {
    id: 501,
    name: 'First Quest',
    description: 'Complete your first Quest.',
    icon: mdiTarget,
    unlocked: true,
    currentProgress: 1,
    targetProgress: 1,
    reward: { type: 'XP', amount: 100 },
    unlockedAt: '12 Sep',
  },
  {
    id: 502,
    name: 'Merge Master',
    description: 'Complete 10 Quests.',
    icon: mdiSourceBranch,
    unlocked: true,
    currentProgress: 10,
    targetProgress: 10,
    reward: { type: 'XP', amount: 500 },
    unlockedAt: 'Today',
    featured: true,
  },
  {
    id: 503,
    name: 'Raid Breaker',
    description: 'Defeat your first Raid Boss.',
    icon: mdiShieldCheckOutline,
    unlocked: true,
    currentProgress: 1,
    targetProgress: 1,
    reward: { type: 'XP', amount: 750 },
    unlockedAt: '08 Sep',
  },
  {
    id: 504,
    name: 'Consistency',
    description: 'Complete a Quest 5 days in a row.',
    icon: mdiLightningBoltOutline,
    unlocked: false,
    currentProgress: 3,
    targetProgress: 5,
    reward: { type: 'XP', amount: 400 },
  },
  {
    id: 505,
    name: 'XP Hunter',
    description: 'Earn 10,000 XP.',
    icon: mdiCreationOutline,
    unlocked: false,
    currentProgress: 8_420,
    targetProgress: 10_000,
    reward: { type: 'XP', amount: 600 },
  },
  {
    id: 506,
    name: 'Code Pathfinder',
    description: 'Reach Level 25.',
    icon: mdiSignDirection,
    unlocked: false,
    currentProgress: 24,
    targetProgress: 25,
    reward: { type: 'TITLE', label: 'Night Coder' },
  },
];

export const achievementUnlockFixture: AchievementUnlockResult = {
  achievementId: 506,
  unlockedAt: 'Today',
  announcement: 'Achievement unlocked · Night Coder',
};

export const achievementPagePresentation = {
  sprintLabel: 'Sprint 04 · Day 12',
  dateLabel: '18 SEP',
  timeLabel: '20:44',
  syncLabel: 'GitHub synced',
};
