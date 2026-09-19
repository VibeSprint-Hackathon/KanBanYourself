import type { Quest } from '@/api/demo.types';

export type BoardColumnId = 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'TESTING' | 'DONE';
export interface BoardColumn {
  id: BoardColumnId;
  label: string;
  visible: boolean;
}
// Placement and presentation are local UI data, separate from the frozen Quest DTO.
export interface BoardQuest {
  quest: Quest;
  columnId: BoardColumnId;
  description: string;
  progress: number | null;
}
export interface QuestFormValue {
  title: string;
  description: string;
  xpReward: number;
  columnId: BoardColumnId;
  progress: number | null;
  externalReference: string;
}
export type ColumnAction = 'rename' | 'left' | 'right' | 'hide';

export const columnVisuals: Record<
  BoardColumnId,
  {
    label: string;
    status: string;
    color: string;
    progress: boolean;
    empty: string;
  }
> = {
  BACKLOG: {
    label: 'Backlog',
    status: 'Backlog',
    color: '#849aa9',
    progress: false,
    empty: 'Drop a Quest here',
  },
  TODO: {
    label: 'Todo',
    status: 'Ready to start',
    color: '#59758d',
    progress: false,
    empty: 'Drop a Quest here',
  },
  IN_PROGRESS: {
    label: 'In Progress',
    status: 'In progress',
    color: '#19a6d5',
    progress: true,
    empty: 'Release to start progress',
  },
  TESTING: {
    label: 'Testing',
    status: 'Testing',
    color: '#a57a24',
    progress: true,
    empty: 'Ready for review',
  },
  DONE: {
    label: 'Done',
    status: 'Completed',
    color: '#329c70',
    progress: true,
    empty: 'Completed Quests appear here',
  },
};

export function displayedProgress(entry: BoardQuest): number | null {
  if (entry.columnId === 'IN_PROGRESS') return entry.progress ?? 0;
  return entry.columnId === 'DONE'
    ? 100
    : columnVisuals[entry.columnId].progress
      ? entry.progress
      : null;
}

export function validQuestForm(value: QuestFormValue): boolean {
  return (
    value.title.trim().length > 0 &&
    Number.isInteger(value.xpReward) &&
    value.xpReward > 0 &&
    (value.progress === null ||
      (Number.isFinite(value.progress) && value.progress >= 0 && value.progress <= 100))
  );
}

export function safeReferenceUrl(reference: string | null): string | undefined {
  if (!reference) return undefined;
  try {
    const url = new URL(reference);
    return ['https:', 'http:'].includes(url.protocol) ? url.href : undefined;
  } catch {
    return undefined;
  }
}
