import type { Quest } from '@/api/demo.types';
import type { BoardColumn, BoardQuest } from '@/components/quests/board.types';

export const boardColumns: BoardColumn[] = [
  { id: 'BACKLOG', label: 'Backlog', visible: true },
  { id: 'TODO', label: 'Todo', visible: true },
  { id: 'IN_PROGRESS', label: 'In Progress', visible: true },
  { id: 'TESTING', label: 'Testing', visible: true },
  { id: 'DONE', label: 'Done', visible: true },
];

export function toBoardQuest(quest: Quest): BoardQuest {
  return {
    quest,
    columnId: quest.status,
    description: quest.description,
    progress: quest.progress,
  };
}
