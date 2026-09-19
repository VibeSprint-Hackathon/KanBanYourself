import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import type {
  CharacterReaction,
  CharacterState,
  DemoState,
  ProgressionResult,
} from '@/api/demo.types';
import { dashboardPresentation } from '@/fixtures/dashboard.fixture';
import { useDemoStore } from '@/stores/demo';

function clone<T>(value: T): T {
  return structuredClone(value);
}

export function useDashboardDemo() {
  const store = useDemoStore();
  const { state, loading, completingQuestId, resetting, error, realtimeStatus, lastProgression } =
    storeToRefs(store);
  const view = reactive(clone(dashboardPresentation));
  const reaction = ref<CharacterReaction | null>(null);
  const toast = ref<string | null>(null);
  const characterState = computed<CharacterState>(() =>
    state.value?.quests.some(({ status }) => status === 'IN_PROGRESS') ? 'coding' : 'idle',
  );
  const quest = computed(
    () =>
      state.value?.quests.find(({ id }) => id === 101) ??
      state.value?.quests.find(({ status }) => status === 'IN_PROGRESS') ??
      state.value?.quests[0],
  );

  let reactionTimer: ReturnType<typeof setTimeout> | undefined;
  let toastTimer: ReturnType<typeof setTimeout> | undefined;
  let stopRealtime: (() => void) | undefined;

  watch(
    state,
    (currentState) => {
      if (currentState === null) {
        return;
      }
      syncPresentation(currentState);
    },
    { immediate: true },
  );

  watch(lastProgression, (progression) => {
    if (progression?.applied) {
      showProgression(progression);
    }
  });

  onMounted(() => {
    stopRealtime = store.startRealtime();
    if (state.value === null) {
      void store.loadState();
    }
  });

  onUnmounted(() => {
    stopRealtime?.();
    clearReactionTimer();
    clearToastTimer();
  });

  async function completeQuest(): Promise<void> {
    if (!quest.value) {
      return;
    }
    const progression = await store.completeQuest(quest.value.id);
    if (progression && !progression.applied) {
      showToast('Quest was already completed');
    }
  }

  async function resetDemo(): Promise<void> {
    if (!(await store.resetDemo())) {
      return;
    }
    Object.assign(view, clone(dashboardPresentation));
    clearReactionTimer();
    reaction.value = null;
    showToast('Demo state restored');
  }

  function syncPresentation(currentState: DemoState): void {
    const { player } = currentState;
    view.player.xpProgress =
      player.nextLevelXp === null ? 1 : Math.min(1, player.totalXp / player.nextLevelXp);
    view.player.xpToReward =
      player.nextLevelXp === null ? 0 : Math.max(0, player.nextLevelXp - player.totalXp);
  }

  function showProgression(progression: ProgressionResult): void {
    view.raid.playerDamage = progression.raidDamage;
    view.activity.title = progression.levelUp
      ? `Level ${progression.player.level} reached`
      : 'Quest completed';
    view.activity.xpGained = progression.xpGained;
    view.activity.timeLabel = 'Just now';
    reaction.value = progression.reaction ?? 'happy';
    clearReactionTimer();
    reactionTimer = setTimeout(() => {
      reaction.value = null;
      reactionTimer = undefined;
    }, 1_500);
    showToast(`Quest completed · +${progression.xpGained} XP`);
  }

  function showToast(message: string): void {
    toast.value = message;
    clearToastTimer();
    toastTimer = setTimeout(() => {
      toast.value = null;
      toastTimer = undefined;
    }, 3_200);
  }

  function clearReactionTimer(): void {
    if (reactionTimer) {
      clearTimeout(reactionTimer);
      reactionTimer = undefined;
    }
  }

  function clearToastTimer(): void {
    if (toastTimer) {
      clearTimeout(toastTimer);
      toastTimer = undefined;
    }
  }

  return {
    state,
    view,
    quest,
    characterState,
    reaction,
    toast,
    loading,
    completingQuestId,
    resetting,
    error,
    realtimeStatus,
    loadState: store.loadState,
    completeQuest,
    resetDemo,
  };
}
