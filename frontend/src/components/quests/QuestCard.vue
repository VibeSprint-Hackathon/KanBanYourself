<script setup lang="ts">
import { computed } from 'vue';
import { mdiCheck, mdiChevronRight, mdiDragVertical, mdiGithub } from '@quasar/extras/mdi-v7';
import { formatNumber } from '@/fixtures/dashboard.fixture';
import { columnVisuals, displayedProgress, type BoardQuest } from './board.types';
const props = defineProps<{
  entry: BoardQuest;
  compact: boolean;
  dragDisabled: boolean;
  dragging: boolean;
}>();
defineEmits<{ open: [id: number]; start: [id: number, event: DragEvent]; end: [] }>();
const visual = computed(() => columnVisuals[props.entry.columnId]);
const progress = computed(() => displayedProgress(props.entry));
</script>

<template>
  <article
    class="quest-card"
    :class="{ compact, dragging, completed: entry.columnId === 'DONE' }"
    :data-quest-id="entry.quest.id"
    :draggable="!dragDisabled"
    :style="{ '--column-accent': visual.color }"
    @dragstart="$emit('start', entry.quest.id, $event)"
    @dragend="$emit('end')"
  >
    <button
      type="button"
      class="card-open"
      :draggable="!dragDisabled"
      :aria-label="`Open quest: ${entry.quest.title}`"
      @click="$emit('open', entry.quest.id)"
    >
      <div class="card-title">
        <h3>{{ entry.quest.title }}</h3>
        <q-icon :name="mdiChevronRight" size="18px" class="muted" />
      </div>
      <p v-if="!compact" class="description">{{ entry.description }}</p>
      <div v-if="entry.quest.externalReference && !compact" class="reference muted">
        <q-icon :name="mdiGithub" size="15px" /><span>{{ entry.quest.externalReference }}</span>
      </div>
      <div v-if="progress !== null" class="card-progress">
        <q-linear-progress
          :value="progress / 100"
          class="progress-track"
          size="9px"
          aria-label="Quest progress"
        /><strong>{{ progress }}%</strong>
      </div>
      <div class="card-footer">
        <strong class="orange">+{{ formatNumber(entry.quest.xpReward) }} XP</strong
        ><span class="status-label"
          ><q-icon v-if="entry.columnId === 'DONE'" :name="mdiCheck" size="16px" />{{
            visual.status
          }}</span
        >
      </div>
    </button>
    <span
      class="drag-handle"
      :title="dragDisabled ? 'Clear search to drag quests' : 'Drag to move Quest'"
      aria-hidden="true"
      ><q-icon :name="mdiDragVertical" size="17px"
    /></span>
  </article>
</template>

<style scoped>
.quest-card {
  position: relative;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--card);
  box-shadow: 0 3px 7px #385b740a;
  cursor: grab;
}
.quest-card:hover {
  border-color: var(--column-accent);
  box-shadow: 0 4px 12px #385b741a;
}
.quest-card.completed {
  border-color: #aedacb;
}
.quest-card.dragging {
  opacity: 0.4;
}
.card-open {
  display: block;
  width: 100%;
  padding: 18px 16px 20px;
  color: var(--ink);
  font: inherit;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
  border-radius: inherit;
}
.card-title {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}
h3 {
  flex: 1;
  margin: 0;
  font-size: 16px;
  font-weight: 750;
  line-height: 1.4;
}
.description {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
  margin: 8px 0 17px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.reference {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 11px;
  margin: -5px 0 14px;
}
.reference span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-progress {
  display: flex;
  gap: 10px;
  align-items: center;
  color: var(--column-accent);
  font-size: 11px;
  margin: 18px 0;
}
.card-progress .progress-track {
  flex: 1;
  color: var(--column-accent);
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding-top: 13px;
  border-top: 1px solid #c5dbe8;
  font-size: 13px;
}
.status-label {
  color: var(--column-accent);
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}
.drag-handle {
  position: absolute;
  left: 1px;
  top: 20px;
  color: var(--muted);
  cursor: grab;
  opacity: 0.6;
}
.compact .card-open {
  padding: 12px 16px;
}
.compact .card-footer {
  margin-top: 10px;
  padding-top: 9px;
}
.compact .card-progress {
  margin: 12px 0;
}
.compact .drag-handle {
  top: 14px;
}
</style>
