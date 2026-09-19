<script setup lang="ts">
import { computed, ref } from 'vue';
import { mdiPlus, mdiMagnify, mdiTuneVariant, mdiClose } from '@quasar/extras/mdi-v7';
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
  quests,
  columns,
  search,
  compact,
  showCompleted,
  searching,
  visibleColumns,
  filteredQuests,
} = board;
const selectedId = ref<number | null>(null);
const selected = computed(() => quests.value.find((q) => q.quest.id === selectedId.value) ?? null);
const selectedColumn = computed(() => columns.value.find((c) => c.id === selected.value?.columnId));
const detailsPresentation = computed(() => ({
  description: selected.value?.description ?? '',
  progressPercent: selected.value ? (displayedProgress(selected.value) ?? 0) : 0,
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

function openQuest(id: number) {
  selectedId.value = id;
  detailsOpen.value = true;
}
function newQuest() {
  selectedId.value = null;
  formMode.value = 'create';
  formOpen.value = true;
}
function editQuest() {
  detailsOpen.value = false;
  formMode.value = 'edit';
  formOpen.value = true;
}
function saveQuest(value: QuestFormValue) {
  const id = board.saveQuest(
    value,
    formMode.value === 'edit' ? (selectedId.value ?? undefined) : undefined,
  );
  if (id === undefined) return;
  formOpen.value = false;
  announcement.value = formMode.value === 'create' ? 'Quest created' : 'Quest updated';
}
function confirmDelete() {
  if (selectedId.value === null) return;
  board.deleteQuest(selectedId.value);
  deleteOpen.value = false;
  detailsOpen.value = false;
  selectedId.value = null;
  announcement.value = 'Quest deleted';
}
function columnAction(id: BoardColumnId, action: ColumnAction) {
  if (action === 'rename') {
    renameId.value = id;
    renameLabel.value = columns.value.find((c) => c.id === id)?.label ?? '';
    renameOpen.value = true;
  } else if (action === 'hide') board.setVisible(id, false);
  else board.reorderColumn(id, action === 'left' ? -1 : 1);
}
function renameColumn() {
  if (!renameLabel.value.trim()) return;
  board.renameColumn(renameId.value, renameLabel.value);
  renameOpen.value = false;
}
function moveQuest(id: number, column: BoardColumnId, beforeId: number | null) {
  board.moveQuest(id, column, beforeId);
  announcement.value = `Quest moved to ${columns.value.find((c) => c.id === column)?.label ?? column}`;
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
        <span
          ><q-icon name="sensors" class="green" size="17px" /> {{ header.github.syncLabel }}</span
        ><span class="header-time"
          >{{ header.dateLabel }} <span>{{ header.timeLabel }}</span></span
        >
      </div>
    </header>
    <div class="board-heading spread">
      <div>
        <h2>Quest board</h2>
        <p>Choose what to tackle next and track each win.</p>
      </div>
      <div class="board-actions">
        <q-btn
          flat
          no-caps
          :icon="mdiTuneVariant"
          label="Board settings"
          class="settings-button"
          @click="settingsOpen = true"
        /><q-btn
          unelevated
          no-caps
          :icon="mdiPlus"
          label="New Quest"
          class="new-quest"
          @click="newQuest"
        />
      </div>
    </div>
    <div class="search-row">
      <q-input
        v-model="search"
        outlined
        dense
        placeholder="Search quests"
        aria-label="Search quests"
        class="quest-search"
        ><template #prepend><q-icon :name="mdiMagnify" size="19px" /></template
        ><template v-if="search" #append
          ><q-btn
            flat
            round
            dense
            :icon="mdiClose"
            size="sm"
            aria-label="Clear search"
            @click="search = ''" /></template></q-input
      ><span v-if="searching" class="search-hint"
        >{{ filteredQuests.length }} matches · Clear search to drag quests</span
      ><span v-else class="search-hint">Drag quests to plan your next win.</span
      ><span class="sr-only" role="status">{{ announcement }}</span>
    </div>
    <QuestBoard
      :columns="visibleColumns"
      :all-columns="columns"
      :quests="filteredQuests"
      :compact="compact"
      :searching="searching"
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
      @edit="editQuest"
      @delete="deleteOpen = true"
    />
    <QuestFormDialog
      v-model="formOpen"
      :mode="formMode"
      :entry="selected"
      :columns="columns"
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
    <q-dialog v-model="deleteOpen" aria-labelledby="delete-quest-title"
      ><q-card class="small-dialog"
        ><q-card-section
          ><h2 id="delete-quest-title">Delete Quest?</h2>
          <p>Delete “{{ selected?.quest.title }}”? This cannot be undone.</p></q-card-section
        ><q-card-actions align="right"
          ><q-btn flat no-caps label="Cancel" @click="deleteOpen = false" /><q-btn
            unelevated
            no-caps
            label="Delete"
            color="negative"
            @click="confirmDelete" /></q-card-actions></q-card
    ></q-dialog>
    <q-dialog v-model="renameOpen" aria-labelledby="rename-column-title"
      ><q-card class="small-dialog"
        ><q-form @submit="renameColumn"
          ><q-card-section
            ><h2 id="rename-column-title">Rename column</h2>
            <q-input
              v-model="renameLabel"
              outlined
              dense
              autofocus
              label="Column name"
              maxlength="32"
              :rules="[(v) => !!String(v).trim() || 'Name is required']" /></q-card-section
          ><q-card-actions align="right"
            ><q-btn flat no-caps label="Cancel" @click="renameOpen = false" /><q-btn
              unelevated
              no-caps
              type="submit"
              label="Save name"
              class="new-quest"
              :disable="!renameLabel.trim()" /></q-card-actions></q-form></q-card
    ></q-dialog>
  </q-page>
</template>

<style scoped>
.quests-page {
  padding: 28px 36px 28px;
  min-width: 0;
}
.page-header {
  min-height: 54px;
  margin-bottom: 22px;
}
.sprint-label {
  font-size: 13px;
  margin-bottom: 5px;
}
h1 {
  font-size: 28px;
  font-weight: 750;
  letter-spacing: -0.5px;
  line-height: 1.2;
  margin: 0;
}
.header-meta {
  display: flex;
  align-items: center;
  gap: 26px;
  font-size: 13px;
}
.header-time {
  font-family: monospace;
  padding: 10px 0 10px 26px;
  border-left: 1px solid var(--border);
}
.header-time span {
  margin-left: 18px;
  color: var(--ink);
}
.board-heading {
  gap: 20px;
  margin-bottom: 18px;
}
h2 {
  font-size: 19px;
  line-height: 1.4;
  font-weight: 750;
  margin: 0 0 5px;
}
.board-heading p {
  font-size: 14px;
  color: var(--muted);
  margin: 0;
}
.board-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}
.new-quest {
  background: var(--blue);
  color: white;
}
.settings-button {
  border: 1px solid var(--border);
  background: #f4fbff88;
  color: var(--muted);
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
.quest-search :deep(.q-field__control::before) {
  border-color: var(--border);
}
.search-hint {
  font-size: 12px;
  color: var(--muted);
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
}
.small-dialog {
  width: 420px;
  background: var(--card);
  color: var(--ink);
  border: 1px solid var(--border);
}
.small-dialog h2 {
  margin-bottom: 16px;
}
.small-dialog p {
  line-height: 1.6;
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
  .board-actions {
    flex-wrap: wrap;
  }
  .search-row {
    flex-wrap: wrap;
  }
  .quest-search {
    width: 100%;
  }
}
</style>
