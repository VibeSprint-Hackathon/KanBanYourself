<script setup lang="ts">
import { mdiArrowLeft, mdiArrowRight, mdiClose, mdiPencilOutline } from '@quasar/extras/mdi-v7';
import type { BoardColumn, BoardColumnId, ColumnAction } from './board.types';
const open = defineModel<boolean>({ required: true });
defineProps<{
  columns: BoardColumn[];
  compact: boolean;
  showCompleted: boolean;
  hideReason: (id: BoardColumnId) => string;
}>();
defineEmits<{
  compact: [value: boolean];
  completed: [value: boolean];
  visible: [id: BoardColumnId, value: boolean];
  action: [id: BoardColumnId, action: ColumnAction];
  reset: [];
}>();
</script>

<template>
  <q-dialog v-model="open" aria-labelledby="board-settings-title"
    ><q-card class="settings-dialog">
      <header class="spread">
        <h2 id="board-settings-title">Board settings</h2>
        <q-btn
          flat
          dense
          round
          :icon="mdiClose"
          aria-label="Close board settings"
          @click="open = false"
        />
      </header>
      <div class="settings-body">
        <q-toggle
          :model-value="compact"
          label="Compact cards"
          @update:model-value="$emit('compact', !!$event)"
        />
        <q-toggle
          :model-value="showCompleted"
          label="Show completed"
          @update:model-value="$emit('completed', !!$event)"
        />
        <p class="muted">
          Arrange your workflow. Hide empty optional columns without losing quests.
        </p>
        <div v-for="(column, index) in columns" :key="column.id" class="settings-column">
          <div class="spread">
            <strong>{{ column.label }}</strong>
            <div class="column-controls">
              <q-btn
                flat
                dense
                :icon="mdiPencilOutline"
                :aria-label="`Rename ${column.label}`"
                @click="$emit('action', column.id, 'rename')"
              />
              <q-btn
                flat
                dense
                :icon="mdiArrowLeft"
                :disable="index === 0"
                :aria-label="`Move ${column.label} left`"
                @click="$emit('action', column.id, 'left')"
              />
              <q-btn
                flat
                dense
                :icon="mdiArrowRight"
                :disable="index === columns.length - 1"
                :aria-label="`Move ${column.label} right`"
                @click="$emit('action', column.id, 'right')"
              />
              <q-toggle
                :model-value="column.visible"
                :disable="column.visible && !!hideReason(column.id)"
                :aria-label="`Show ${column.label} column`"
                @update:model-value="$emit('visible', column.id, !!$event)"
              />
            </div>
          </div>
          <small v-if="column.visible && hideReason(column.id)" class="muted">{{
            hideReason(column.id)
          }}</small>
        </div>
      </div>
      <footer class="spread">
        <q-btn flat no-caps label="Reset board layout" class="blue" @click="$emit('reset')" /><q-btn
          flat
          no-caps
          label="Close"
          @click="open = false"
        />
      </footer> </q-card
  ></q-dialog>
</template>

<style scoped>
.settings-dialog {
  width: 520px;
  max-width: calc(100vw - 32px);
  background: var(--card);
  color: var(--ink);
  border: 1px solid var(--border);
  border-radius: 7px;
}
header,
footer {
  padding: 16px 24px;
}
header {
  border-bottom: 1px solid var(--border);
}
h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}
.settings-body {
  padding: 12px 24px;
}
.settings-body > .q-toggle {
  display: flex;
}
.settings-body p {
  font-size: 13px;
  margin-top: 18px;
}
.settings-column {
  padding: 9px 0;
  border-top: 1px solid #d2e4ee;
}
.settings-column strong {
  font-size: 14px;
  overflow-wrap: anywhere;
}
.column-controls {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
footer {
  border-top: 1px solid var(--border);
}
</style>
