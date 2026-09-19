import type { Quest } from '@/api/demo.types';
import type { BoardColumn, BoardQuest } from '@/components/quests/board.types';

export const boardColumns: BoardColumn[] = [
  { id: 'TODO', label: 'Todo', visible: true },
  { id: 'IN_PROGRESS', label: 'In Progress', visible: true },
  { id: 'DONE', label: 'Done', visible: true },
];

const questPresentation: Record<number, Pick<BoardQuest, 'description' | 'progress'>> = {
  101: {
    description: 'Fix server-side validation and cover the payment edge cases.',
    progress: 72,
  },
  102: {
    description: 'Warn users before API traffic reaches the configured rate limit.',
    progress: null,
  },
  103: {
    description: 'Make login failures clearer and easier to resolve.',
    progress: 100,
  },
};

export function toBoardQuest(quest: Quest): BoardQuest {
  const presentation = questPresentation[quest.id];
  return {
    quest,
    columnId: quest.status,
    description: presentation?.description ?? 'Quest details are managed by the backend.',
    progress:
      quest.status === 'DONE'
        ? 100
        : quest.status === 'IN_PROGRESS'
          ? (presentation?.progress ?? 0)
          : null,
  };
}
