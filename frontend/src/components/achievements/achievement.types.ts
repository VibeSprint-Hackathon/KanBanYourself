export type AchievementFilter = 'ALL' | 'UNLOCKED' | 'LOCKED';

export type AchievementReward = { type: 'XP'; amount: number } | { type: 'TITLE'; label: string };

export interface Achievement {
  id: number;
  name: string;
  description: string;
  icon: string;
  unlocked: boolean;
  currentProgress: number;
  targetProgress: number;
  reward: AchievementReward;
  unlockedAt?: string;
  featured?: boolean;
  newlyUnlocked?: boolean;
}

export interface AchievementSummaryState {
  unlocked: number;
  total: number;
}

export interface AchievementUnlockResult {
  achievementId: number;
  unlockedAt: string;
  announcement: string;
}

export function achievementProgress(achievement: Achievement): number {
  if (achievement.targetProgress <= 0) return 0;
  return Math.min(1, Math.max(0, achievement.currentProgress / achievement.targetProgress));
}

export function achievementPercent(summary: AchievementSummaryState): number {
  if (summary.total <= 0) return 0;
  return Math.round((summary.unlocked / summary.total) * 100);
}

export function achievementRewardLabel(reward: AchievementReward): string {
  return reward.type === 'XP' ? `+${reward.amount.toLocaleString('en-US')} XP` : reward.label;
}
