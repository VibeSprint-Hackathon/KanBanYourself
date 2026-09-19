<script setup lang="ts">
import { mdiDotsHorizontal } from '@quasar/extras/mdi-v7';
import QuestCard from './QuestCard.vue';
import type { Player } from '@/api/demo.types';
import { columnVisuals, type BoardColumn, type BoardQuest, type ColumnAction } from './board.types';
const props = defineProps<{
  column: BoardColumn;
  quests: BoardQuest[];
  players: Player[];
  compact: boolean;
  searching: boolean;
  dragId: number | null;
  over: boolean;
  beforeId: number | null;
  first: boolean;
  last: boolean;
  hideReason: string;
  readOnly: boolean;
}>();
const emit = defineEmits<{
  open: [id: number];
  start: [id: number, event: DragEvent];
  end: [];
  hover: [beforeId: number | null];
  drop: [beforeId: number | null];
  action: [action: ColumnAction];
}>();
function insertionPoint(event: DragEvent): number | null {
  const region = event.currentTarget as HTMLElement;
  const cards = [...region.querySelectorAll<HTMLElement>('[data-quest-id]')];
  const card = cards.find((card) => {
    const rect = card.getBoundingClientRect();
    return event.clientY < rect.top + rect.height / 2;
  });
  return card ? Number(card.dataset.questId) : null;
}
function dragOver(event: DragEvent) {
  if (
    props.readOnly ||
    props.searching ||
    !event.dataTransfer?.types.includes('application/x-vibesprint-quest')
  )
    return;
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
  const region = event.currentTarget as HTMLElement;
  const rect = region.getBoundingClientRect();
  if (event.clientY < rect.top + 45) region.scrollTop -= 14;
  if (event.clientY > rect.bottom - 45) region.scrollTop += 14;
  emit('hover', insertionPoint(event));
}
function drop(event: DragEvent) {
  if (
    props.readOnly ||
    props.searching ||
    !event.dataTransfer?.types.includes('application/x-vibesprint-quest')
  )
    return;
  event.preventDefault();
  emit('drop', insertionPoint(event));
}
</script>

<template>
  <section
    class="quest-column"
    :class="{ 'drag-over': over }"
    :data-column-id="column.id"
    :style="{ '--column-accent': columnVisuals[column.id].color }"
    :aria-label="`${column.label} column`"
  >
    <header class="column-header">
      <span class="column-dot" />
      <h2>{{ column.label }}</h2>
      <span class="column-count" :aria-label="`${quests.length} quests`">{{ quests.length }}</span>
      <q-btn
        v-if="!readOnly"
        flat
        dense
        round
        :icon="mdiDotsHorizontal"
        size="sm"
        :aria-label="`${column.label} column menu`"
        ><q-menu class="column-menu"
          ><q-list dense>
            <q-item clickable v-close-popup @click="emit('action', 'rename')"
              ><q-item-section>Rename</q-item-section></q-item
            >
            <q-item clickable v-close-popup :disable="first" @click="emit('action', 'left')"
              ><q-item-section>Move left</q-item-section></q-item
            >
            <q-item clickable v-close-popup :disable="last" @click="emit('action', 'right')"
              ><q-item-section>Move right</q-item-section></q-item
            >
            <q-item clickable v-close-popup :disable="!!hideReason" @click="emit('action', 'hide')"
              ><q-item-section
                >Hide column<q-item-label v-if="hideReason" caption>{{
                  hideReason
                }}</q-item-label></q-item-section
              ></q-item
            >
          </q-list></q-menu
        ></q-btn
      >
    </header>
    <div
      class="column-content"
      tabindex="0"
      :aria-label="readOnly ? `${column.label} quests` : `Drop quests in ${column.label}`"
      @dragover="dragOver"
      @drop="drop"
    >
      <div
        v-for="entry in quests"
        :key="entry.quest.id"
        class="card-slot"
        :class="{ 'insert-before': over && beforeId === entry.quest.id }"
      >
        <QuestCard
          :entry="entry"
          :assignee="players.find((player) => player.id === entry.quest.assigneeId)"
          :compact="compact"
          :drag-disabled="readOnly || searching || entry.columnId === 'DONE'"
          :dragging="dragId === entry.quest.id"
          @open="emit('open', $event)"
          @start="(id, event) => emit('start', id, event)"
          @end="emit('end')"
        />
      </div>
      <div
        class="column-tail"
        :class="{ 'insert-before': over && beforeId === null, empty: !quests.length }"
      >
        <div v-if="!quests.length || over" class="drop-hint">
          <strong>{{
            searching ? 'No matching quests' : readOnly ? 'No quests yet' : 'Move Quest here'
          }}</strong
          ><span>{{
            searching
              ? 'Try a different search.'
              : readOnly
                ? 'No server Quests in this status.'
                : columnVisuals[column.id].empty
          }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.quest-column {
  flex: 1 0 280px;
  min-width: 280px;
  background: #eff9ffb8;
  border: 1px solid var(--border);
  border-radius: 7px;
  box-shadow: 0 5px 12px #385b740e;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.quest-column.drag-over {
  background: #d8effbcc;
  border-color: var(--blue);
}
.column-header {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 16px;
  height: 58px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border);
}
.column-dot {
  height: 8px;
  width: 8px;
  border-radius: 50%;
  background: var(--column-accent);
  flex-shrink: 0;
}
h2 {
  font-size: 11px;
  letter-spacing: 1.3px;
  text-transform: uppercase;
  font-weight: 700;
  margin: 0;
  line-height: 1.4;
  overflow-wrap: anywhere;
}
.column-count {
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
}
.column-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  scrollbar-width: thin;
}
.card-slot {
  position: relative;
  margin-bottom: 12px;
}
.insert-before::before {
  content: '';
  display: block;
  position: absolute;
  top: -8px;
  height: 3px;
  left: 0;
  right: 0;
  background: var(--blue);
  border-radius: 3px;
  pointer-events: none;
}
.column-tail {
  position: relative;
  min-height: 80px;
}
.column-tail.empty {
  height: 100%;
}
.drop-hint {
  border: 1px dashed #95cde5;
  border-radius: 5px;
  min-height: 110px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-items: center;
  justify-content: center;
  background: #cbe7f54a;
  text-align: center;
  padding: 14px;
}
.drop-hint strong {
  color: var(--blue);
  font-size: 12px;
  font-weight: 500;
}
.drop-hint span {
  color: var(--muted);
  font-size: 11px;
}
</style>
