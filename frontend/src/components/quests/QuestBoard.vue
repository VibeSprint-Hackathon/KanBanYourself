<script setup lang="ts">
import { ref, watch } from 'vue';
import QuestColumn from './QuestColumn.vue';
import type { BoardColumn, BoardColumnId, BoardQuest, ColumnAction } from './board.types';
import type { Player } from '@/api/demo.types';
const props = defineProps<{
  columns: BoardColumn[];
  allColumns: BoardColumn[];
  quests: BoardQuest[];
  players: Player[];
  compact: boolean;
  searching: boolean;
  readOnly: boolean;
  hideReason: (id: BoardColumnId) => string;
}>();
const emit = defineEmits<{
  open: [id: number];
  move: [id: number, columnId: BoardColumnId, beforeId: number | null];
  action: [id: BoardColumnId, action: ColumnAction];
}>();
const dragId = ref<number | null>(null);
const overColumn = ref<BoardColumnId | null>(null);
const beforeId = ref<number | null>(null);
function endDrag() {
  dragId.value = null;
  overColumn.value = null;
  beforeId.value = null;
}
watch(() => props.searching, endDrag);
function startDrag(id: number, event: DragEvent) {
  if (props.readOnly || props.searching || !event.dataTransfer) {
    event.preventDefault();
    return;
  }
  dragId.value = id;
  event.dataTransfer.setData('text/plain', String(id));
  event.dataTransfer.setData('application/x-vibesprint-quest', String(id));
  event.dataTransfer.effectAllowed = 'move';
}
function hover(column: BoardColumnId, before: number | null) {
  overColumn.value = column;
  beforeId.value = before;
}
function drop(column: BoardColumnId, before: number | null) {
  if (dragId.value !== null) emit('move', dragId.value, column, before);
  endDrag();
}
function scrollBoard(event: DragEvent) {
  if (dragId.value === null) return;
  const board = event.currentTarget as HTMLElement;
  const rect = board.getBoundingClientRect();
  if (event.clientX < rect.left + 55) board.scrollLeft -= 22;
  if (event.clientX > rect.right - 55) board.scrollLeft += 22;
}
function leaveBoard(event: DragEvent) {
  const board = event.currentTarget as HTMLElement;
  if (!(event.relatedTarget instanceof Node) || !board.contains(event.relatedTarget))
    overColumn.value = null;
}
</script>

<template>
  <div
    class="quest-board"
    role="region"
    aria-label="Quest board"
    tabindex="0"
    @dragover="scrollBoard"
    @dragleave="leaveBoard"
    @keydown.esc="endDrag"
  >
    <QuestColumn
      v-for="column in columns"
      :key="column.id"
      :column="column"
      :quests="quests.filter((q) => q.columnId === column.id)"
      :players="players"
      :compact="compact"
      :searching="searching"
      :read-only="readOnly"
      :drag-id="dragId"
      :over="overColumn === column.id"
      :before-id="beforeId"
      :first="allColumns[0]?.id === column.id"
      :last="allColumns[allColumns.length - 1]?.id === column.id"
      :hide-reason="hideReason(column.id)"
      @open="emit('open', $event)"
      @start="startDrag"
      @end="endDrag"
      @hover="hover(column.id, $event)"
      @drop="drop(column.id, $event)"
      @action="emit('action', column.id, $event)"
    />
  </div>
</template>

<style scoped>
.quest-board {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  width: 100%;
  max-width: 100%;
  height: max(520px, calc(100vh - 295px));
  padding: 0 0 12px;
  scrollbar-width: thin;
  align-items: stretch;
}
</style>
