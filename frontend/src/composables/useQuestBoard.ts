import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import type { CreateQuestRequest, ProgressionResult, UpdateQuestRequest } from '@/api/demo.types';
import type { BoardColumn, BoardColumnId, QuestFormValue } from '@/components/quests/board.types';
import { boardColumns, toBoardQuest } from '@/fixtures/quest-board.fixture';
import { useDemoStore } from '@/stores/demo';

const PREFERENCES_KEY = 'vibesprint.quest-board.preferences.v1';
const COLUMN_IDS: BoardColumnId[] = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'TESTING', 'DONE'];

interface BoardPreferences {
  compact: boolean;
  showCompleted: boolean;
  columns: BoardColumn[];
}

export function useQuestBoard() {
  const store = useDemoStore();
  const {
    state,
    loading,
    error,
    realtimeStatus,
    completingQuestId,
    creatingQuest,
    mutatingQuestId,
  } = storeToRefs(store);
  const preferences = loadPreferences();
  const columns = ref(preferences.columns);
  const search = ref('');
  const compact = ref(preferences.compact);
  const showCompleted = ref(preferences.showCompleted);
  const searching = computed(() => search.value.trim().length > 0);
  const mutationPending = computed(
    () => creatingQuest.value || mutatingQuestId.value !== null || completingQuestId.value !== null,
  );
  const quests = computed(() => state.value?.quests.map(toBoardQuest) ?? []);
  const visibleColumns = computed(() =>
    columns.value.filter(
      (column) => column.visible && (column.id !== 'DONE' || showCompleted.value),
    ),
  );
  const filteredQuests = computed(() => {
    const term = search.value.trim().toLocaleLowerCase();
    if (!term) return quests.value;
    return quests.value.filter((entry) =>
      `${entry.quest.title} ${entry.description}`.toLocaleLowerCase().includes(term),
    );
  });

  let stopRealtime: (() => void) | undefined;

  watch(
    [columns, compact, showCompleted],
    () =>
      savePreferences({
        columns: columns.value,
        compact: compact.value,
        showCompleted: showCompleted.value,
      }),
    { deep: true },
  );

  onMounted(() => {
    stopRealtime = store.startRealtime();
    if (state.value === null) void store.loadState();
  });

  onUnmounted(() => stopRealtime?.());

  function count(id: BoardColumnId): number {
    return quests.value.filter((entry) => entry.columnId === id).length;
  }

  function hideReason(id: BoardColumnId): string {
    if (id === 'TODO' || id === 'DONE') return 'This column is required';
    return count(id) ? 'Move quests before hiding this column' : '';
  }

  function setVisible(id: BoardColumnId, visible: boolean): void {
    const column = columns.value.find((candidate) => candidate.id === id);
    if (column && (visible || !hideReason(id))) column.visible = visible;
  }

  function renameColumn(id: BoardColumnId, label: string): void {
    const column = columns.value.find((candidate) => candidate.id === id);
    if (column && label.trim()) column.label = label.trim();
  }

  function reorderColumn(id: BoardColumnId, direction: -1 | 1): void {
    const index = columns.value.findIndex((candidate) => candidate.id === id);
    const destination = index + direction;
    if (index < 0 || destination < 0 || destination >= columns.value.length) return;
    const [column] = columns.value.splice(index, 1);
    if (column) columns.value.splice(destination, 0, column);
  }

  function reveal(id: BoardColumnId): void {
    setVisible(id, true);
    if (id === 'DONE') showCompleted.value = true;
  }

  async function saveQuest(value: QuestFormValue, id?: number): Promise<boolean> {
    const request = normalizeForm(value);
    if (id === undefined) {
      if (request.status === 'DONE') return false;
      const success = await store.createQuest(request as CreateQuestRequest);
      if (success) reveal(request.status);
      return success;
    }
    const success = await store.updateQuest(id, request);
    if (success) reveal(request.status);
    return success;
  }

  async function deleteQuest(id: number): Promise<boolean> {
    return store.deleteQuest(id);
  }

  async function moveQuest(
    id: number,
    status: BoardColumnId,
    beforeQuestId: number | null,
  ): Promise<boolean | ProgressionResult> {
    if (searching.value || mutationPending.value || id === beforeQuestId) return false;
    const entry = quests.value.find((candidate) => candidate.quest.id === id);
    if (!entry || entry.columnId === 'DONE') return false;
    if (status === 'DONE') {
      const progression = await store.completeQuest(id);
      if (progression) reveal('DONE');
      return progression ?? false;
    }
    const success = await store.moveQuest(id, { status, beforeQuestId });
    if (success) reveal(status);
    return success;
  }

  function resetLayout(): void {
    columns.value = structuredClone(boardColumns);
    compact.value = false;
    showCompleted.value = true;
  }

  return {
    state,
    quests,
    columns,
    search,
    compact,
    showCompleted,
    searching,
    mutationPending,
    visibleColumns,
    filteredQuests,
    loading,
    error,
    realtimeStatus,
    completingQuestId,
    creatingQuest,
    mutatingQuestId,
    count,
    hideReason,
    setVisible,
    renameColumn,
    reorderColumn,
    saveQuest,
    deleteQuest,
    moveQuest,
    resetLayout,
    loadState: store.loadState,
    completeQuest: store.completeQuest,
  };
}

function normalizeForm(value: QuestFormValue): UpdateQuestRequest {
  const status = value.columnId;
  const progress =
    status === 'BACKLOG' || status === 'TODO'
      ? null
      : status === 'DONE'
        ? 100
        : Math.min(100, Math.max(0, Math.round(Number(value.progress ?? 0))));
  return {
    title: value.title.trim(),
    description: value.description.trim(),
    status,
    progress,
    xpReward: Math.round(Number(value.xpReward)),
    externalReference: value.externalReference.trim() || null,
  };
}

function loadPreferences(): BoardPreferences {
  const fallback = defaultPreferences();
  try {
    const stored = localStorage.getItem(PREFERENCES_KEY);
    if (!stored) return fallback;
    const value: unknown = JSON.parse(stored);
    if (
      !isRecord(value) ||
      typeof value.compact !== 'boolean' ||
      typeof value.showCompleted !== 'boolean'
    ) {
      return fallback;
    }
    if (!Array.isArray(value.columns) || value.columns.length !== COLUMN_IDS.length)
      return fallback;
    const columns = value.columns.filter(isStoredColumn);
    const ids = columns.map(({ id }) => id);
    if (columns.length !== COLUMN_IDS.length || COLUMN_IDS.some((id) => !ids.includes(id)))
      return fallback;
    return { compact: value.compact, showCompleted: value.showCompleted, columns };
  } catch {
    return fallback;
  }
}

function savePreferences(preferences: BoardPreferences): void {
  try {
    localStorage.setItem(PREFERENCES_KEY, JSON.stringify(preferences));
  } catch {
    // Board preferences are optional; storage failures must not block the board.
  }
}

function defaultPreferences(): BoardPreferences {
  return { compact: false, showCompleted: true, columns: structuredClone(boardColumns) };
}

function isStoredColumn(value: unknown): value is BoardColumn {
  return (
    isRecord(value) &&
    COLUMN_IDS.includes(value.id as BoardColumnId) &&
    typeof value.label === 'string' &&
    value.label.trim().length > 0 &&
    value.label.length <= 32 &&
    typeof value.visible === 'boolean'
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
