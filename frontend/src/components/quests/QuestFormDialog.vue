<script setup lang="ts">
import { computed, reactive, watch } from 'vue';
import { mdiClose } from '@quasar/extras/mdi-v7';
import type { Player } from '@/api/demo.types';
import {
  columnVisuals,
  validQuestForm,
  type BoardColumn,
  type BoardQuest,
  type QuestFormValue,
} from './board.types';
const open = defineModel<boolean>({ required: true });
const props = defineProps<{
  mode: 'create' | 'edit';
  entry: BoardQuest | null;
  columns: BoardColumn[];
  players: Player[];
  defaultAssigneeId: number | null;
  submitting: boolean;
  error: string | null;
}>();
const emit = defineEmits<{ save: [value: QuestFormValue] }>();
const form = reactive<QuestFormValue>({
  title: '',
  description: '',
  xpReward: 250,
  columnId: 'TODO',
  progress: 0,
  assigneeId: 0,
  externalReference: '',
});
const options = computed(() =>
  props.columns
    .filter((column) =>
      props.entry?.columnId === 'DONE' ? column.id === 'DONE' : column.id !== 'DONE',
    )
    .map((column) => ({ label: column.label, value: column.id })),
);
const completed = computed(() => props.mode === 'edit' && props.entry?.columnId === 'DONE');
const hasProgress = computed(() => form.columnId === 'IN_PROGRESS' || form.columnId === 'TESTING');
const payload = computed<QuestFormValue>(() => ({
  ...form,
  progress: hasProgress.value ? Number(form.progress) : form.columnId === 'DONE' ? 100 : null,
}));
const valid = computed(() => validQuestForm(payload.value));
watch(open, (value) => {
  if (!value) return;
  const entry = props.entry;
  Object.assign(
    form,
    entry && props.mode === 'edit'
      ? {
          title: entry.quest.title,
          description: entry.description,
          xpReward: entry.quest.xpReward,
          columnId: entry.columnId,
          progress: entry.progress ?? 0,
          assigneeId: entry.quest.assigneeId,
          externalReference: entry.quest.externalReference ?? '',
        }
      : {
          title: '',
          description: '',
          xpReward: 250,
          columnId: 'TODO',
          progress: 0,
          assigneeId: props.defaultAssigneeId ?? props.players[0]?.id ?? 0,
          externalReference: '',
        },
  );
});
function submit() {
  if (valid.value) emit('save', payload.value);
}
</script>

<template>
  <q-dialog v-model="open" aria-labelledby="quest-form-title">
    <q-card class="quest-form-dialog">
      <header>
        <div class="spread">
          <span class="eyebrow blue">{{ mode === 'create' ? 'New Quest' : 'Edit Quest' }}</span
          ><q-btn
            flat
            dense
            round
            :icon="mdiClose"
            aria-label="Close Quest form"
            :disable="submitting"
            @click="open = false"
          />
        </div>
        <h2 id="quest-form-title">{{ mode === 'create' ? 'Create a Quest' : 'Edit Quest' }}</h2>
        <p class="muted">Set one clear objective and its XP reward.</p>
      </header>
      <q-form @submit="submit">
        <div class="form-fields">
          <q-input
            v-model="form.title"
            outlined
            dense
            autofocus
            label="Quest title"
            placeholder="Name this Quest"
            maxlength="255"
            :rules="[(v) => !!String(v).trim() || 'Title is required']"
            lazy-rules
          />
          <q-input
            v-model="form.description"
            outlined
            label="Description"
            type="textarea"
            rows="3"
            placeholder="What needs to be accomplished?"
            maxlength="2000"
            :rules="[(v) => !!String(v).trim() || 'Description is required']"
            lazy-rules
          />
          <div class="form-row">
            <q-input
              v-model.number="form.xpReward"
              outlined
              dense
              label="XP reward"
              type="number"
              min="1"
              step="1"
              suffix="XP"
              :disable="completed"
              :rules="[
                (v) => (Number.isInteger(Number(v)) && Number(v) > 0) || 'Use a positive integer',
              ]"
              lazy-rules
            /><q-select
              v-model="form.columnId"
              outlined
              dense
              :label="mode === 'create' ? 'Initial column' : 'Column'"
              :options="options"
              emit-value
              map-options
              :disable="completed"
            />
          </div>
          <q-select
            v-model="form.assigneeId"
            outlined
            dense
            label="Assignee"
            :options="players.map((player) => ({ label: player.name, value: player.id }))"
            emit-value
            map-options
            :disable="completed"
          />
          <q-input
            v-if="hasProgress"
            v-model.number="form.progress"
            outlined
            dense
            label="Progress"
            type="number"
            min="0"
            max="100"
            suffix="%"
            :rules="[(v) => (Number(v) >= 0 && Number(v) <= 100) || 'Use 0–100']"
            lazy-rules
          />
          <q-input
            v-model="form.externalReference"
            outlined
            dense
            label="GitHub reference (optional)"
            placeholder="Pull request URL or reference"
            maxlength="500"
          />
          <p v-if="form.columnId === 'DONE'" class="muted form-note">
            {{ columnVisuals.DONE.status }} · status, progress and XP reward are locked.
          </p>
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        </div>
        <footer>
          <q-btn
            flat
            no-caps
            label="Cancel"
            class="secondary-action"
            :disable="submitting"
            @click="open = false"
          /><q-btn
            unelevated
            no-caps
            type="submit"
            :label="mode === 'create' ? 'Create Quest' : 'Save changes'"
            class="primary-action"
            :loading="submitting"
            :disable="!valid || submitting"
          />
        </footer>
      </q-form>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.quest-form-dialog {
  width: 500px;
  max-width: calc(100vw - 32px);
  border: 1px solid var(--border);
  border-radius: 7px;
  color: var(--ink);
  background: var(--card);
}
header {
  padding: 18px 24px;
  border-bottom: 1px solid var(--border);
}
h2 {
  font-size: 22px;
  line-height: 1.3;
  margin: 3px 0 7px;
  font-weight: 750;
}
header p {
  font-size: 13px;
  margin: 0;
}
.form-fields {
  padding: 24px;
  display: grid;
  gap: 18px;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.form-note {
  margin: 0;
  font-size: 12px;
}
.form-error {
  margin: 0;
  color: var(--red);
  font-size: 13px;
}
footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}
.primary-action {
  color: white;
  background: var(--blue);
}
.secondary-action {
  border: 1px solid var(--border);
  color: var(--muted);
}
:deep(.q-field__control) {
  background: #e7f3fb66;
  color: var(--blue);
}
:deep(.q-field--outlined .q-field__control::before) {
  border-color: var(--border);
}
:deep(.q-field__label) {
  color: var(--muted);
}
@media (max-width: 420px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
