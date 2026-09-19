import {
  mdiCreationOutline,
  mdiShieldCheckOutline,
  mdiSignDirection,
  mdiSourceBranch,
  mdiTarget,
} from '@quasar/extras/mdi-v7';

export type AchievementFilter = 'ALL' | 'UNLOCKED' | 'LOCKED';
export type AchievementCategory = 'QUEST' | 'XP' | 'LEVEL' | 'RAID' | 'GITHUB';

export interface AchievementState {
  key: string;
  name: string;
  description: string;
  category: AchievementCategory;
  currentProgress: number;
  targetProgress: number;
  unlocked: boolean;
  unlockedAt: string | null;
  rewardLabel: string;
  featured: boolean;
}

export interface Achievement extends AchievementState {
  icon: string;
}

export interface AchievementResponse {
  playerId: number;
  unlocked: number;
  total: number;
  achievements: AchievementState[];
}

export interface AchievementSummaryState {
  unlocked: number;
  total: number;
}

export function achievementProgress(achievement: Achievement): number {
  if (achievement.targetProgress <= 0) return 0;
  return Math.min(1, Math.max(0, achievement.currentProgress / achievement.targetProgress));
}

export function achievementPercent(summary: AchievementSummaryState): number {
  if (summary.total <= 0) return 0;
  return Math.round((summary.unlocked / summary.total) * 100);
}

export function achievementIcon(achievement: AchievementState): string {
  switch (achievement.category) {
    case 'QUEST': return mdiTarget;
    case 'XP': return mdiCreationOutline;
    case 'LEVEL': return mdiSignDirection;
    case 'GITHUB': return mdiSourceBranch;
    case 'RAID': return mdiShieldCheckOutline;
  }
}
