import type { Quest } from '@/api/demo.types';

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

// Presentation-only data. Domain state is always loaded from the backend.
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
