import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import type {
  CharacterReaction,
  CharacterState,
  Player,
  ProgressionResult,
} from '@/api/demo.types';
import { dashboardPresentation } from '@/fixtures/dashboard.fixture';
import { useDemoStore } from '@/stores/demo';

function clone<T>(value: T): T {
  return structuredClone(value);
}

export function useDashboardDemo() {
  const store = useDemoStore();
  const {
    state,
    selectedPlayer,
    loading,
    completingQuestId,
    resetting,
    error,
    realtimeStatus,
    lastProgression,
  } = storeToRefs(store);
  const view = reactive(clone(dashboardPresentation));
  const reaction = ref<CharacterReaction | null>(null);
  const toast = ref<string | null>(null);
  const characterState = computed<CharacterState>(
    () => selectedPlayer.value?.characterState ?? 'idle',
  );
  const quest = computed(
    () =>
      state.value?.quests.find(
        ({ status, assigneeId }) =>
          status === 'IN_PROGRESS' && assigneeId === selectedPlayer.value?.id,
      ) ?? null,
  );

  let reactionTimer: ReturnType<typeof setTimeout> | undefined;
  let toastTimer: ReturnType<typeof setTimeout> | undefined;
  let stopRealtime: (() => void) | undefined;

  watch(
    selectedPlayer,
    (player, previousPlayer) => {
      if (player === null) {
        return;
      }
      if (previousPlayer && previousPlayer.id !== player.id) {
        clearReactionTimer();
        reaction.value = null;
      }
      syncPresentation(player);
    },
    { immediate: true },
  );

  watch(lastProgression, (progression) => {
    if (progression?.applied && progression.player.id === selectedPlayer.value?.id) {
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

  function syncPresentation(player: Player): void {
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
    if (progression.player.id === selectedPlayer.value?.id) {
      reaction.value = progression.reaction ?? 'happy';
      clearReactionTimer();
      reactionTimer = setTimeout(() => {
        reaction.value = null;
        reactionTimer = undefined;
      }, 1_500);
    }
    showToast(`${progression.player.name} completed a Quest · +${progression.xpGained} XP`);
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
    selectedPlayer,
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
