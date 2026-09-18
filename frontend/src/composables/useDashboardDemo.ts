import { computed, onUnmounted, reactive, ref } from 'vue';
import type { CharacterReaction, DemoState, ProgressionResult } from '@/api/demo.types';
import {
  appliedProgressionResult,
  dashboardPresentation,
  demoState,
  repeatedProgressionResult,
} from '@/fixtures/dashboard.fixture';

function clone<T>(value: T): T {
  return structuredClone(value);
}

export function useDashboardDemo() {
  const state = reactive<DemoState>(clone(demoState));
  const view = reactive(clone(dashboardPresentation));
  const reaction = ref<CharacterReaction | null>(null);
  const toast = ref<string | null>(null);
  const quest = computed(() => state.quests.find(({ id }) => id === 101));

  let feedbackTimer: ReturnType<typeof setTimeout> | undefined;

  function getMockCompletionResult(): ProgressionResult {
    return clone(
      quest.value?.status === 'DONE' ? repeatedProgressionResult : appliedProgressionResult,
    );
  }

  function applyProgressionResult(result: ProgressionResult): boolean {
    if (!result.applied) return false;

    const questIndex = state.quests.findIndex(({ id }) => id === result.quest.id);
    if (questIndex >= 0) state.quests[questIndex] = result.quest;
    state.player = result.player;
    state.raid = result.raid;

    const questPresentation = view.quests[result.quest.id];
    if (questPresentation) questPresentation.progressPercent = 100;
    view.player.xpProgress =
      result.player.nextLevelXp === null ? 1 : result.player.totalXp / result.player.nextLevelXp;
    view.player.xpToReward = 0;
    view.raid.playerDamage = result.raidDamage;
    view.activity.title = result.levelUp
      ? `Level ${result.player.level} reached`
      : 'Quest completed';
    view.activity.xpGained = result.xpGained;
    view.activity.timeLabel = 'Just now';

    reaction.value = result.reaction;
    toast.value = `Quest completed · +${result.xpGained} XP`;
    if (feedbackTimer) clearTimeout(feedbackTimer);
    feedbackTimer = setTimeout(() => {
      reaction.value = null;
      toast.value = null;
    }, 3200);
    return true;
  }

  function completeQuest(): ProgressionResult {
    const result = getMockCompletionResult();
    applyProgressionResult(result);
    return result;
  }

  onUnmounted(() => {
    if (feedbackTimer) clearTimeout(feedbackTimer);
  });

  return { state, view, quest, reaction, toast, completeQuest };
}
