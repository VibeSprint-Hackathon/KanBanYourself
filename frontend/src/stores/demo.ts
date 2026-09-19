import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { isAxiosError } from 'axios';
import { completeDemoQuest, getDemoState, resetDemoState } from '@/api/demo';
import type { ApiError, DemoState, ProgressionResult } from '@/api/demo.types';
import { progressionRealtime, type RealtimeStatus } from '@/realtime/progression';

const DEFAULT_ERROR = 'Backend is unavailable. Check the server and try again.';

export const useDemoStore = defineStore('demo', () => {
  const state = ref<DemoState | null>(null);
  const loading = ref(false);
  const completingQuestId = ref<number | null>(null);
  const resetting = ref(false);
  const error = ref<string | null>(null);
  const lastProgression = ref<ProgressionResult | null>(null);
  const realtimeStatus = ref<RealtimeStatus>('disconnected');
  const handledEventIds = new Set<string>();

  let realtimeUsers = 0;
  let stopProgression: (() => void) | undefined;
  let stopStatus: (() => void) | undefined;

  const hasState = computed(() => state.value !== null);

  async function loadState(silent = false): Promise<boolean> {
    if (!silent) {
      loading.value = true;
    }
    error.value = null;

    try {
      state.value = await getDemoState();
      return true;
    } catch (cause) {
      error.value = errorMessage(cause);
      return false;
    } finally {
      if (!silent) {
        loading.value = false;
      }
    }
  }

  async function completeQuest(questId: number): Promise<ProgressionResult | null> {
    if (completingQuestId.value !== null) {
      return null;
    }

    completingQuestId.value = questId;
    error.value = null;

    try {
      const progression = await completeDemoQuest(questId, {
        eventId: createEventId(),
        source: 'DEMO',
      });
      applyProgression(progression);
      await loadState(true);
      return progression;
    } catch (cause) {
      error.value = errorMessage(cause);
      return null;
    } finally {
      completingQuestId.value = null;
    }
  }

  async function resetDemo(): Promise<boolean> {
    if (resetting.value) {
      return false;
    }

    resetting.value = true;
    error.value = null;

    try {
      state.value = await resetDemoState();
      handledEventIds.clear();
      lastProgression.value = null;
      return true;
    } catch (cause) {
      error.value = errorMessage(cause);
      return false;
    } finally {
      resetting.value = false;
    }
  }

  function applyProgression(progression: ProgressionResult): void {
    if (state.value !== null) {
      const quests = state.value.quests.map((quest) =>
        quest.id === progression.quest.id ? progression.quest : quest,
      );
      if (!quests.some((quest) => quest.id === progression.quest.id)) {
        quests.push(progression.quest);
      }
      state.value = {
        ...state.value,
        player: progression.player,
        quests,
        raid: progression.raid,
      };
    }

    if (!progression.applied || handledEventIds.has(progression.eventId)) {
      return;
    }

    handledEventIds.add(progression.eventId);
    lastProgression.value = progression;
  }

  function startRealtime(): () => void {
    realtimeUsers += 1;
    if (realtimeUsers === 1) {
      let previousStatus: RealtimeStatus | null = null;
      let hasConnected = false;
      stopProgression = progressionRealtime.subscribe(applyProgression);
      stopStatus = progressionRealtime.subscribeStatus((status) => {
        const reconnected =
          hasConnected && previousStatus === 'disconnected' && status === 'connected';
        previousStatus = status;
        hasConnected ||= status === 'connected';
        realtimeStatus.value = status;
        if (reconnected && state.value !== null) {
          void loadState(true);
        }
      });
    }

    let stopped = false;
    return () => {
      if (stopped) {
        return;
      }
      stopped = true;
      realtimeUsers = Math.max(0, realtimeUsers - 1);
      if (realtimeUsers === 0) {
        stopProgression?.();
        stopStatus?.();
        stopProgression = undefined;
        stopStatus = undefined;
      }
    };
  }

  return {
    state,
    loading,
    completingQuestId,
    resetting,
    error,
    lastProgression,
    realtimeStatus,
    hasState,
    loadState,
    completeQuest,
    resetDemo,
    startRealtime,
  };
});

function createEventId(): string {
  return typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `demo-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function errorMessage(cause: unknown): string {
  if (isAxiosError<ApiError>(cause)) {
    return cause.response?.data?.message || DEFAULT_ERROR;
  }
  return DEFAULT_ERROR;
}
