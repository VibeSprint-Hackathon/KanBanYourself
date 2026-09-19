<script setup lang="ts">
import { computed, ref } from 'vue';
import { mdiClose, mdiMagnify } from '@quasar/extras/mdi-v7';
import { useQuestBoard } from '@/composables/useQuestBoard';
import { dashboardPresentation as header } from '@/fixtures/dashboard.fixture';
import { columnVisuals, displayedProgress } from '@/components/quests/board.types';
import QuestBoard from '@/components/quests/QuestBoard.vue';
import QuestDetailsDrawer from '@/components/dashboard/QuestDetailsDrawer.vue';

const board = useQuestBoard();
const {
  state,
  quests,
  columns,
  search,
  compact,
  searching,
  visibleColumns,
  filteredQuests,
  loading,
  error,
  realtimeStatus,
  completingQuestId,
  loadState,
  completeQuest,
} = board;
const selectedId = ref<number | null>(null);
const selected = computed(() => quests.value.find((entry) => entry.quest.id === selectedId.value));
const selectedColumn = computed(() =>
  columns.value.find((column) => column.id === selected.value?.columnId),
);
const detailsPresentation = computed(() => ({
  description: selected.value?.description ?? '',
  progressPercent: selected.value ? (displayedProgress(selected.value) ?? 0) : 0,
  pullRequestLabel: selected.value?.quest.externalReference ?? '',
}));
const detailsOpen = ref(false);
const announcement = ref('');

function openQuest(id: number) {
  selectedId.value = id;
  detailsOpen.value = true;
}

async function completeSelectedQuest() {
  if (!selected.value) {
    return;
  }
  const progression = await completeQuest(selected.value.quest.id);
  if (progression?.applied) {
    announcement.value = `Quest completed. ${progression.xpGained} XP earned.`;
  } else if (progression) {
    announcement.value = 'Quest was already completed.';
  }
}
</script>

<template>
  <q-page class="quests-page">
    <header class="page-header spread">
      <div>
        <div class="blue sprint-label">{{ header.sprintLabel }}</div>
        <h1>Quests</h1>
      </div>
      <div class="header-meta muted">
        <span>
          <q-icon
            name="sensors"
            :class="realtimeStatus === 'connected' ? 'green' : 'orange'"
            size="17px"
          />
          {{ realtimeStatus === 'connected' ? 'Live sync' : 'Sync offline' }}
        </span>
        <span class="header-time"
          >{{ header.dateLabel }} <span>{{ header.timeLabel }}</span></span
        >
      </div>
    </header>

    <div class="board-heading">
      <h2>Quest board</h2>
      <p>Server-backed sprint state. Complete a Quest to update XP and the Raid.</p>
    </div>

    <q-card v-if="loading && !state" flat bordered class="state-card">
      <q-spinner color="primary" size="34px" />
      <strong>Loading Quests…</strong>
    </q-card>
    <q-card v-else-if="!state" flat bordered class="state-card error-card">
      <q-icon name="cloud_off" size="34px" class="red" />
      <strong>Could not load Quests</strong>
      <span class="muted">{{ error }}</span>
      <q-btn unelevated no-caps label="Try again" color="primary" @click="loadState()" />
    </q-card>

    <template v-else>
      <div v-if="error" class="sync-warning" role="alert">
        <q-icon name="warning" size="19px" />
        <span>{{ error }}</span>
        <q-btn flat dense no-caps label="Retry" @click="loadState()" />
      </div>
      <div class="search-row">
        <q-input
          v-model="search"
          outlined
          dense
          placeholder="Search quests"
          aria-label="Search quests"
          class="quest-search"
        >
          <template #prepend><q-icon :name="mdiMagnify" size="19px" /></template>
          <template v-if="search" #append>
            <q-btn
              flat
              round
              dense
              :icon="mdiClose"
              size="sm"
              aria-label="Clear search"
              @click="search = ''"
            />
          </template>
        </q-input>
        <span class="search-hint">
          {{ searching ? `${filteredQuests.length} matches` : 'Quest statuses come from backend.' }}
        </span>
        <span class="sr-only" role="status">{{ announcement }}</span>
      </div>
      <QuestBoard
        :columns="visibleColumns"
        :all-columns="columns"
        :quests="filteredQuests"
        :compact="compact"
        :searching="searching"
        :hide-reason="board.hideReason"
        read-only
        @open="openQuest"
      />
      <QuestDetailsDrawer
        v-if="selected"
        v-model="detailsOpen"
        :quest="selected.quest"
        :presentation="detailsPresentation"
        :status-label="selectedColumn?.label ?? columnVisuals[selected.columnId].label"
        :show-progress="displayedProgress(selected) !== null"
        :status-color="columnVisuals[selected.columnId].color"
        :completing="completingQuestId === selected.quest.id"
        @complete="completeSelectedQuest"
      />
    </template>
  </q-page>
</template>

<style scoped>
.quests-page {
  min-width: 0;
  padding: 28px 36px;
}
.page-header {
  min-height: 54px;
  margin-bottom: 22px;
}
.sprint-label {
  margin-bottom: 5px;
  font-size: 13px;
}
h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 750;
  line-height: 1.2;
  letter-spacing: -0.5px;
}
.header-meta,
.header-meta > span {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
}
.header-time {
  padding: 10px 0 10px 26px;
  margin-left: 17px;
  border-left: 1px solid var(--border);
  font-family: monospace;
}
.header-time span {
  margin-left: 9px;
  color: var(--ink);
}
.board-heading {
  padding-bottom: 18px;
}
.board-heading h2 {
  margin: 0 0 5px;
  font-size: 19px;
  font-weight: 750;
}
.board-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0 18px;
  border-top: 1px solid var(--border);
}
.quest-search {
  width: 300px;
  background: #f4fbffa0;
}
.search-hint {
  color: var(--muted);
  font-size: 12px;
}
.state-card {
  display: grid;
  min-height: 420px;
  place-content: center;
  justify-items: center;
  gap: 16px;
  text-align: center;
}
.error-card {
  border-color: #ffaaa3;
}
.sync-warning {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 14px;
  color: #75442d;
  border: 1px solid #efca8c;
  border-radius: 6px;
  background: #fff0d4;
}
.sync-warning span {
  flex: 1;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
}
@media (max-width: 1100px) {
  .quests-page {
    padding: 24px;
  }
  .header-time {
    display: none;
  }
}
@media (max-width: 700px) {
  .quests-page {
    padding: 18px;
  }
  .header-meta {
    display: none;
  }
  .search-row {
    flex-wrap: wrap;
  }
  .quest-search {
    width: 100%;
  }
}
</style>
