<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { mdiClose, mdiMagnify, mdiPlus, mdiTuneVariant } from '@quasar/extras/mdi-v7';
import { useQuestBoard } from '@/composables/useQuestBoard';
import { dashboardPresentation as header } from '@/fixtures/dashboard.fixture';
import {
  columnVisuals,
  displayedProgress,
  type BoardColumnId,
  type ColumnAction,
  type QuestFormValue,
} from '@/components/quests/board.types';
import QuestBoard from '@/components/quests/QuestBoard.vue';
import QuestFormDialog from '@/components/quests/QuestFormDialog.vue';
import BoardSettingsDialog from '@/components/quests/BoardSettingsDialog.vue';
import QuestDetailsDrawer from '@/components/dashboard/QuestDetailsDrawer.vue';

const board = useQuestBoard();
const {
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
  loadState,
} = board;
const selectedId = ref<number | null>(null);
const selected = computed(
  () => quests.value.find((entry) => entry.quest.id === selectedId.value) ?? null,
);
const selectedColumn = computed(() =>
  columns.value.find((column) => column.id === selected.value?.columnId),
);
const detailsPresentation = computed(() => ({
  description: selected.value?.quest.description ?? '',
  progressPercent: selected.value?.quest.progress ?? 0,
  pullRequestLabel: selected.value?.quest.externalReference ?? '',
}));
const detailsOpen = ref(false);
const formOpen = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const settingsOpen = ref(false);
const deleteOpen = ref(false);
const renameOpen = ref(false);
const renameId = ref<BoardColumnId>('BACKLOG');
const renameLabel = ref('');
const announcement = ref('');
const formSubmitting = computed(() =>
  formMode.value === 'create'
    ? creatingQuest.value
    : selectedId.value !== null && mutatingQuestId.value === selectedId.value,
);

watch(selected, (entry) => {
  if (selectedId.value !== null && entry === null) {
    detailsOpen.value = false;
    formOpen.value = false;
    deleteOpen.value = false;
    selectedId.value = null;
  }
});

function openQuest(id: number): void {
  selectedId.value = id;
  detailsOpen.value = true;
}

function newQuest(): void {
  selectedId.value = null;
  formMode.value = 'create';
  formOpen.value = true;
}

function editQuest(): void {
  if (!selected.value) return;
  detailsOpen.value = false;
  formMode.value = 'edit';
  formOpen.value = true;
}

async function saveQuest(value: QuestFormValue): Promise<void> {
  const success = await board.saveQuest(
    value,
    formMode.value === 'edit' ? (selectedId.value ?? undefined) : undefined,
  );
  if (!success) return;
  formOpen.value = false;
  announcement.value = formMode.value === 'create' ? 'Quest created' : 'Quest updated';
}

async function confirmDelete(): Promise<void> {
  if (selectedId.value === null || selected.value?.columnId === 'DONE') return;
  if (!(await board.deleteQuest(selectedId.value))) return;
  deleteOpen.value = false;
  detailsOpen.value = false;
  selectedId.value = null;
  announcement.value = 'Quest deleted';
}

async function completeSelectedQuest(): Promise<void> {
  if (!selected.value || selected.value.columnId === 'DONE') return;
  const progression = await board.completeQuest(selected.value.quest.id);
  if (progression?.applied) {
    announcement.value = `Quest completed. ${progression.xpGained} XP earned.`;
  } else if (progression) {
    announcement.value = 'Quest was already completed.';
  }
}

function columnAction(id: BoardColumnId, action: ColumnAction): void {
  if (action === 'rename') {
    renameId.value = id;
    renameLabel.value = columns.value.find((column) => column.id === id)?.label ?? '';
    renameOpen.value = true;
  } else if (action === 'hide') {
    board.setVisible(id, false);
  } else {
    board.reorderColumn(id, action === 'left' ? -1 : 1);
  }
}

function renameColumn(): void {
  if (!renameLabel.value.trim()) return;
  board.renameColumn(renameId.value, renameLabel.value);
  renameOpen.value = false;
}

async function moveQuest(
  id: number,
  column: BoardColumnId,
  beforeId: number | null,
): Promise<void> {
  const result = await board.moveQuest(id, column, beforeId);
  if (!result) return;
  if (typeof result !== 'boolean') {
    announcement.value = result.applied
      ? `Quest completed. ${result.xpGained} XP earned.`
      : 'Quest was already completed.';
    return;
  }
  announcement.value = `Quest moved to ${columns.value.find((item) => item.id === column)?.label ?? column}`;
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

    <div class="board-heading spread">
      <div>
        <h2>Quest board</h2>
        <p>Choose what to tackle next and track each server-backed win.</p>
      </div>
      <div class="board-actions">
        <q-btn
          flat
          no-caps
          :icon="mdiTuneVariant"
          label="Board settings"
          class="settings-button"
          @click="settingsOpen = true"
        />
        <q-btn
          unelevated
          no-caps
          :icon="mdiPlus"
          label="New Quest"
          class="new-quest"
          :loading="creatingQuest"
          :disable="mutationPending"
          @click="newQuest"
        />
      </div>
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
          {{
            searching
              ? `${filteredQuests.length} matches · Clear search to drag quests`
              : 'Drag quests to plan your next win.'
          }}
        </span>
        <span class="sr-only" role="status" aria-live="polite">{{ announcement }}</span>
      </div>
      <QuestBoard
        :columns="visibleColumns"
        :all-columns="columns"
        :quests="filteredQuests"
        :compact="compact"
        :searching="searching"
        :read-only="mutationPending"
        :hide-reason="board.hideReason"
        @open="openQuest"
        @move="moveQuest"
        @action="columnAction"
      />
      <QuestDetailsDrawer
        v-if="selected"
        v-model="detailsOpen"
        :quest="selected.quest"
        :presentation="detailsPresentation"
        mode="board"
        :status-label="selectedColumn?.label ?? columnVisuals[selected.columnId].label"
        :completed="selected.columnId === 'DONE'"
        :show-progress="displayedProgress(selected) !== null"
        :status-color="columnVisuals[selected.columnId].color"
        :completing="completingQuestId === selected.quest.id"
        :mutating="mutatingQuestId === selected.quest.id"
        @complete="completeSelectedQuest"
        @edit="editQuest"
        @delete="deleteOpen = true"
      />
      <QuestFormDialog
        v-model="formOpen"
        :mode="formMode"
        :entry="selected"
        :columns="columns"
        :submitting="formSubmitting"
        :error="error"
        @save="saveQuest"
      />
      <BoardSettingsDialog
        v-model="settingsOpen"
        :columns="columns"
        :compact="compact"
        :show-completed="showCompleted"
        :hide-reason="board.hideReason"
        @compact="compact = $event"
        @completed="showCompleted = $event"
        @visible="board.setVisible"
        @action="columnAction"
        @reset="board.resetLayout"
      />
      <q-dialog v-model="deleteOpen" aria-labelledby="delete-quest-title">
        <q-card class="small-dialog">
          <q-card-section>
            <h2 id="delete-quest-title">Delete Quest?</h2>
            <p>Delete “{{ selected?.quest.title }}”? This cannot be undone.</p>
            <p v-if="error" class="red" role="alert">{{ error }}</p>
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat no-caps label="Cancel" @click="deleteOpen = false" />
            <q-btn
              unelevated
              no-caps
              label="Delete"
              color="negative"
              :loading="selectedId !== null && mutatingQuestId === selectedId"
              :disable="selected?.columnId === 'DONE'"
              @click="confirmDelete"
            />
          </q-card-actions>
        </q-card>
      </q-dialog>
      <q-dialog v-model="renameOpen" aria-labelledby="rename-column-title">
        <q-card class="small-dialog">
          <q-form @submit="renameColumn">
            <q-card-section>
              <h2 id="rename-column-title">Rename column</h2>
              <q-input
                v-model="renameLabel"
                outlined
                dense
                autofocus
                label="Column name"
                maxlength="32"
                :rules="[(value) => !!String(value).trim() || 'Name is required']"
              />
            </q-card-section>
            <q-card-actions align="right">
              <q-btn flat no-caps label="Cancel" @click="renameOpen = false" />
              <q-btn
                unelevated
                no-caps
                type="submit"
                label="Save name"
                class="new-quest"
                :disable="!renameLabel.trim()"
              />
            </q-card-actions>
          </q-form>
        </q-card>
      </q-dialog>
    </template>
  </q-page>
</template>

<style scoped>
.quests-page {
  min-width: 0;
  max-width: 100%;
  padding: 28px 36px;
  overflow-x: hidden;
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
  gap: 20px;
  padding-bottom: 18px;
}
.board-heading h2,
.small-dialog h2 {
  margin: 0 0 5px;
  font-size: 19px;
  font-weight: 750;
}
.board-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
}
.board-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
}
.new-quest {
  color: white;
  background: var(--blue);
}
.settings-button {
  color: var(--muted);
  border: 1px solid var(--border);
  background: #f4fbff88;
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
.small-dialog {
  width: 420px;
  color: var(--ink);
  border: 1px solid var(--border);
  background: var(--card);
}
.small-dialog h2 {
  margin-bottom: 16px;
}
.small-dialog p {
  line-height: 1.6;
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
  .board-heading {
    flex-wrap: wrap;
  }
}
@media (max-width: 700px) {
  .quests-page {
    padding: 18px;
  }
  .header-meta {
    display: none;
  }
  .board-actions,
  .search-row {
    flex-wrap: wrap;
  }
  .quest-search {
    width: 100%;
  }
}
</style>
