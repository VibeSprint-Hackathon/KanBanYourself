import { computed, onMounted, onUnmounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { boardColumns, toBoardQuest } from '@/fixtures/quest-board.fixture';
import { useDemoStore } from '@/stores/demo';

export function useQuestBoard() {
  const store = useDemoStore();
  const { state, loading, error, realtimeStatus, completingQuestId } = storeToRefs(store);
  const columns = ref(structuredClone(boardColumns));
  const search = ref('');
  const compact = ref(false);
  const searching = computed(() => search.value.trim().length > 0);
  const quests = computed(() => state.value?.quests.map(toBoardQuest) ?? []);
  const filteredQuests = computed(() => {
    const term = search.value.trim().toLocaleLowerCase();
    if (!term) {
      return quests.value;
    }
    return quests.value.filter((entry) =>
      `${entry.quest.title} ${entry.description}`.toLocaleLowerCase().includes(term),
    );
  });

  let stopRealtime: (() => void) | undefined;

  onMounted(() => {
    stopRealtime = store.startRealtime();
    if (state.value === null) {
      void store.loadState();
    }
  });

  onUnmounted(() => stopRealtime?.());

  function hideReason(): string {
    return 'Server columns cannot be hidden';
  }

  return {
    state,
    quests,
    columns,
    visibleColumns: columns,
    search,
    compact,
    searching,
    filteredQuests,
    loading,
    error,
    realtimeStatus,
    completingQuestId,
    hideReason,
    loadState: store.loadState,
    completeQuest: store.completeQuest,
  };
}
