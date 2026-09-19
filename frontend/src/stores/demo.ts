import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { isAxiosError } from 'axios';
import {
  completeDemoQuest,
  createDemoQuest,
  deleteDemoQuest,
  getDemoState,
  moveDemoQuest,
  resetDemoState,
  updateDemoQuest,
} from '@/api/demo';
import {
  activateRaid as activateRaidRequest,
  cancelRaid as cancelRaidRequest,
  completeRaid as completeRaidRequest,
  createRaid as createRaidRequest,
  deleteRaid as deleteRaidRequest,
  getRaids,
  updateRaid as updateRaidRequest,
} from '@/api/raids';
import type {
  ApiError,
  CreateQuestRequest,
  CreateRaidRequest,
  DemoState,
  MoveQuestRequest,
  ProgressionResult,
  Quest,
  QuestStatus,
  Raid,
  UpdateRaidRequest,
  UpdateQuestRequest,
} from '@/api/demo.types';
import { progressionRealtime, type RealtimeStatus } from '@/realtime/progression';

const DEFAULT_ERROR = 'Backend is unavailable. Check the server and try again.';
const SELECTED_PLAYER_KEY = 'vibesprint.selected-player-id';

export const useDemoStore = defineStore('demo', () => {
  const state = ref<DemoState | null>(null);
  const selectedPlayerId = ref<number | null>(loadSelectedPlayerId());
  const loading = ref(false);
  const completingQuestId = ref<number | null>(null);
  const creatingQuest = ref(false);
  const mutatingQuestId = ref<number | null>(null);
  const resetting = ref(false);
  const raids = ref<Raid[]>([]);
  const raidsLoading = ref(false);
  const creatingRaid = ref(false);
  const mutatingRaidId = ref<number | null>(null);
  const error = ref<string | null>(null);
  const lastProgression = ref<ProgressionResult | null>(null);
  const progressionEvents = ref<ProgressionResult[]>([]);
  const realtimeStatus = ref<RealtimeStatus>('disconnected');
  const handledEventIds = new Set<string>();

  let realtimeUsers = 0;
  let stopProgression: (() => void) | undefined;
  let stopStatus: (() => void) | undefined;

  const hasState = computed(() => state.value !== null);
  const selectedPlayer = computed(
    () =>
      state.value?.players.find((player) => player.id === selectedPlayerId.value) ??
      state.value?.players[0] ??
      null,
  );

  async function loadState(silent = false): Promise<boolean> {
    if (!silent) {
      loading.value = true;
    }
    error.value = null;

    try {
      setState(await getDemoState());
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
    if (completingQuestId.value !== null || mutatingQuestId.value !== null || creatingQuest.value) {
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

  async function loadRaids(silent = false): Promise<boolean> {
    if (!silent) raidsLoading.value = true;
    error.value = null;
    try {
      raids.value = await getRaids();
      return true;
    } catch (cause) {
      error.value = errorMessage(cause);
      return false;
    } finally {
      if (!silent) raidsLoading.value = false;
    }
  }

  async function createRaid(request: CreateRaidRequest): Promise<boolean> {
    if (creatingRaid.value || mutatingRaidId.value !== null) return false;
    creatingRaid.value = true;
    error.value = null;
    try {
      await createRaidRequest(request);
      return await refreshRaidsAndState();
    } catch (cause) {
      error.value = errorMessage(cause);
      return false;
    } finally {
      creatingRaid.value = false;
    }
  }

  async function updateRaid(raidId: number, request: UpdateRaidRequest): Promise<boolean> {
    return mutateRaid(raidId, () => updateRaidRequest(raidId, request));
  }

  async function activateRaid(raidId: number): Promise<boolean> {
    return mutateRaid(raidId, () => activateRaidRequest(raidId));
  }

  async function cancelRaid(raidId: number): Promise<boolean> {
    return mutateRaid(raidId, () => cancelRaidRequest(raidId));
  }

  async function completeRaid(raidId: number): Promise<boolean> {
    return mutateRaid(raidId, () => completeRaidRequest(raidId));
  }

  async function deleteRaid(raidId: number): Promise<boolean> {
    return mutateRaid(raidId, () => deleteRaidRequest(raidId));
  }

  async function mutateRaid(raidId: number, request: () => Promise<unknown>): Promise<boolean> {
    if (creatingRaid.value || mutatingRaidId.value !== null) return false;
    mutatingRaidId.value = raidId;
    error.value = null;
    try {
      await request();
      return await refreshRaidsAndState();
    } catch (cause) {
      error.value = errorMessage(cause);
      return false;
    } finally {
      mutatingRaidId.value = null;
    }
  }

  async function refreshRaidsAndState(): Promise<boolean> {
    const [nextRaids, nextState] = await Promise.all([getRaids(), getDemoState()]);
    raids.value = nextRaids;
    setState(nextState);
    return true;
  }

  async function createQuest(request: CreateQuestRequest): Promise<boolean> {
    if (creatingQuest.value || mutatingQuestId.value !== null || completingQuestId.value !== null) {
      return false;
    }
    creatingQuest.value = true;
    error.value = null;
    try {
      setState(await createDemoQuest(request));
      return true;
    } catch (cause) {
      error.value = errorMessage(cause);
      return false;
    } finally {
      creatingQuest.value = false;
    }
  }

  async function updateQuest(questId: number, request: UpdateQuestRequest): Promise<boolean> {
    return mutateQuest(questId, () => updateDemoQuest(questId, request));
  }

  async function deleteQuest(questId: number): Promise<boolean> {
    return mutateQuest(questId, () => deleteDemoQuest(questId));
  }

  async function moveQuest(questId: number, request: MoveQuestRequest): Promise<boolean> {
    return mutateQuest(questId, () => moveDemoQuest(questId, request));
  }

  async function mutateQuest(questId: number, request: () => Promise<DemoState>): Promise<boolean> {
    if (creatingQuest.value || mutatingQuestId.value !== null || completingQuestId.value !== null) {
      return false;
    }
    mutatingQuestId.value = questId;
    error.value = null;
    try {
      setState(await request());
      return true;
    } catch (cause) {
      error.value = errorMessage(cause);
      return false;
    } finally {
      mutatingQuestId.value = null;
    }
  }

  async function resetDemo(): Promise<boolean> {
    if (resetting.value) {
      return false;
    }

    resetting.value = true;
    error.value = null;

    try {
      setState(await resetDemoState());
      raids.value = await getRaids();
      handledEventIds.clear();
      lastProgression.value = null;
      progressionEvents.value = [];
      return true;
    } catch (cause) {
      error.value = errorMessage(cause);
      return false;
    } finally {
      resetting.value = false;
    }
  }

  function applyProgression(progression: ProgressionResult): void {
    if (progression.raid !== null) {
      raids.value = raids.value.map((raid) =>
        raid.id === progression.raid?.id ? progression.raid : raid,
      );
    }
    if (state.value !== null) {
      const quests = state.value.quests.map((quest) =>
        quest.id === progression.quest.id ? progression.quest : quest,
      );
      if (!quests.some((quest) => quest.id === progression.quest.id)) {
        quests.push(progression.quest);
      }
      state.value = {
        ...state.value,
        players: state.value.players.map((player) =>
          player.id === progression.player.id ? progression.player : player,
        ),
        quests: sortQuests(quests),
        raid: progression.raid?.status === 'ACTIVE' ? progression.raid : null,
      };
    }

    if (!progression.applied || handledEventIds.has(progression.eventId)) {
      return;
    }

    handledEventIds.add(progression.eventId);
    lastProgression.value = progression;
    progressionEvents.value = [...progressionEvents.value.slice(-19), progression];
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

  function selectPlayer(playerId: number): void {
    if (!state.value?.players.some((player) => player.id === playerId)) return;
    selectedPlayerId.value = playerId;
    try {
      localStorage.setItem(SELECTED_PLAYER_KEY, String(playerId));
    } catch {
      // Profile switching remains usable when storage is unavailable.
    }
  }

  function setState(nextState: DemoState): void {
    state.value = nextState;
    const requested = selectedPlayerId.value;
    const fallback = nextState.players[0]?.id ?? null;
    selectedPlayerId.value =
      requested !== null && nextState.players.some((player) => player.id === requested)
        ? requested
        : fallback;
    if (selectedPlayerId.value !== null) {
      try {
        localStorage.setItem(SELECTED_PLAYER_KEY, String(selectedPlayerId.value));
      } catch {
        // Persisting the mock profile is optional.
      }
    }
  }

  return {
    state,
    selectedPlayerId,
    selectedPlayer,
    loading,
    completingQuestId,
    creatingQuest,
    mutatingQuestId,
    resetting,
    raids,
    raidsLoading,
    creatingRaid,
    mutatingRaidId,
    error,
    lastProgression,
    progressionEvents,
    realtimeStatus,
    hasState,
    loadState,
    loadRaids,
    completeQuest,
    createQuest,
    updateQuest,
    deleteQuest,
    moveQuest,
    createRaid,
    updateRaid,
    activateRaid,
    cancelRaid,
    completeRaid,
    deleteRaid,
    resetDemo,
    startRealtime,
    selectPlayer,
  };
});

function loadSelectedPlayerId(): number | null {
  try {
    const value = Number(localStorage.getItem(SELECTED_PLAYER_KEY));
    return Number.isInteger(value) && value > 0 ? value : null;
  } catch {
    return null;
  }
}

const STATUS_ORDER: Record<QuestStatus, number> = {
  BACKLOG: 0,
  TODO: 1,
  IN_PROGRESS: 2,
  TESTING: 3,
  DONE: 4,
};

function sortQuests(quests: Quest[]): Quest[] {
  return [...quests].sort(
    (left, right) =>
      STATUS_ORDER[left.status] - STATUS_ORDER[right.status] ||
      left.sortOrder - right.sortOrder ||
      left.id - right.id,
  );
}

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
