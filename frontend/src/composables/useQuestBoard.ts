import { computed, ref } from 'vue';
import { boardColumns, boardQuests } from '@/fixtures/quest-board.fixture';
import {
  validQuestForm,
  type BoardColumnId,
  type QuestFormValue,
} from '@/components/quests/board.types';

export function useQuestBoard() {
  const quests = ref(structuredClone(boardQuests));
  const columns = ref(structuredClone(boardColumns));
  const search = ref('');
  const compact = ref(false);
  const showCompleted = ref(true);
  let nextId = Math.max(...boardQuests.map(({ quest }) => quest.id)) + 1;
  const searching = computed(() => search.value.trim().length > 0);
  const visibleColumns = computed(() =>
    columns.value.filter((c) => c.visible && (c.id !== 'DONE' || showCompleted.value)),
  );
  const filteredQuests = computed(() => {
    const term = search.value.trim().toLocaleLowerCase();
    return quests.value.filter((q) =>
      `${q.quest.title} ${q.description}`.toLocaleLowerCase().includes(term),
    );
  });

  function count(id: BoardColumnId) {
    return quests.value.filter((q) => q.columnId === id).length;
  }
  function hideReason(id: BoardColumnId): string {
    if (id === 'TODO' || id === 'DONE') return 'This column is required';
    return count(id) ? 'Move quests before hiding this column' : '';
  }
  function setVisible(id: BoardColumnId, visible: boolean) {
    const column = columns.value.find((c) => c.id === id);
    if (column && (visible || !hideReason(id))) column.visible = visible;
  }
  function renameColumn(id: BoardColumnId, label: string) {
    const column = columns.value.find((c) => c.id === id);
    if (column && label.trim()) column.label = label.trim();
  }
  function reorderColumn(id: BoardColumnId, direction: -1 | 1) {
    const index = columns.value.findIndex((c) => c.id === id);
    const other = index + direction;
    if (index < 0 || other < 0 || other >= columns.value.length) return;
    const [column] = columns.value.splice(index, 1);
    if (column) columns.value.splice(other, 0, column);
  }
  function reveal(id: BoardColumnId) {
    setVisible(id, true);
    if (id === 'DONE') showCompleted.value = true;
  }
  function saveQuest(value: QuestFormValue, id?: number): number | undefined {
    if (!validQuestForm(value)) return;
    const entry = id === undefined ? undefined : quests.value.find((q) => q.quest.id === id);
    if (id !== undefined && !entry) return;
    const fields = {
      title: value.title.trim(),
      xpReward: value.xpReward,
      externalReference: value.externalReference.trim() || null,
    };
    if (entry) {
      Object.assign(entry.quest, fields);
      entry.description = value.description.trim();
      entry.progress = value.progress;
      entry.columnId = value.columnId;
    } else {
      id = nextId++;
      quests.value.push({
        quest: { id, ...fields, status: 'TODO', assigneeId: 1 },
        columnId: value.columnId,
        description: value.description.trim(),
        progress: value.progress,
      });
    }
    reveal(value.columnId);
    search.value = '';
    return id;
  }
  function deleteQuest(id: number) {
    quests.value = quests.value.filter((q) => q.quest.id !== id);
  }
  // beforeId refers to the destination list BEFORE the move. Remove first to avoid
  // index shifts when reordering within the same column. DTO status stays untouched.
  function moveQuest(id: number, columnId: BoardColumnId, beforeId: number | null) {
    if (searching.value || id === beforeId) return;
    const entry = quests.value.find((q) => q.quest.id === id);
    if (!entry || !visibleColumns.value.some((c) => c.id === columnId)) return;
    const remaining = quests.value.filter((q) => q.quest.id !== id);
    const before =
      beforeId === null
        ? -1
        : remaining.findIndex((q) => q.quest.id === beforeId && q.columnId === columnId);
    entry.columnId = columnId;
    remaining.splice(before < 0 ? remaining.length : before, 0, entry);
    quests.value = remaining;
  }
  function resetLayout() {
    columns.value = structuredClone(boardColumns);
    showCompleted.value = true;
  }
  return {
    quests,
    columns,
    search,
    compact,
    showCompleted,
    searching,
    visibleColumns,
    filteredQuests,
    count,
    hideReason,
    setVisible,
    renameColumn,
    reorderColumn,
    saveQuest,
    deleteQuest,
    moveQuest,
    resetLayout,
  };
}
