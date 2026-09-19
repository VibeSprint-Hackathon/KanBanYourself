<script setup lang="ts">
import { computed, reactive, watch } from 'vue';
import type { CreateRaidRequest, Raid } from '@/api/demo.types';

const open = defineModel<boolean>({ required: true });
const props = defineProps<{
  mode: 'create' | 'edit';
  raid: Raid | null;
  submitting: boolean;
  error: string | null;
}>();
const emit = defineEmits<{ save: [request: CreateRaidRequest] }>();
const form = reactive<CreateRaidRequest>({
  name: '',
  description: '',
  maxHp: 1000,
  externalReference: null,
});
const finished = computed(
  () => props.raid?.status === 'COMPLETED' || props.raid?.status === 'CANCELLED',
);
const valid = computed(
  () =>
    form.name.trim().length > 0 &&
    form.name.trim().length <= 255 &&
    form.description.trim().length > 0 &&
    form.description.trim().length <= 2000 &&
    Number.isInteger(Number(form.maxHp)) &&
    Number(form.maxHp) > 0 &&
    (form.externalReference?.trim().length ?? 0) <= 500,
);

watch(open, (value) => {
  if (!value) return;
  Object.assign(
    form,
    props.mode === 'edit' && props.raid
      ? {
          name: props.raid.name,
          description: props.raid.description,
          maxHp: props.raid.maxHp,
          externalReference: props.raid.externalReference,
        }
      : { name: '', description: '', maxHp: 1000, externalReference: null },
  );
});

function submit(): void {
  if (!valid.value) return;
  emit('save', {
    name: form.name.trim(),
    description: form.description.trim(),
    maxHp: Number(form.maxHp),
    externalReference: form.externalReference?.trim() || null,
  });
}
</script>

<template>
  <q-dialog v-model="open" aria-labelledby="raid-form-title">
    <q-card class="raid-form-dialog">
      <header class="spread">
        <div>
          <div class="eyebrow blue">{{ mode === 'create' ? 'New Raid' : 'Edit Raid' }}</div>
          <h2 id="raid-form-title">{{ mode === 'create' ? 'Prepare a Raid' : raid?.name }}</h2>
        </div>
        <q-btn flat round dense icon="close" :disable="submitting" @click="open = false" />
      </header>
      <q-form @submit="submit">
        <div class="fields">
          <q-input
            v-model="form.name"
            outlined
            dense
            autofocus
            label="Raid name"
            maxlength="255"
            :rules="[(value) => !!String(value).trim() || 'Name is required']"
          />
          <q-input
            v-model="form.description"
            outlined
            type="textarea"
            rows="4"
            label="Description"
            maxlength="2000"
            :rules="[(value) => !!String(value).trim() || 'Description is required']"
          />
          <q-input
            v-model.number="form.maxHp"
            outlined
            dense
            type="number"
            min="1"
            step="1"
            label="Maximum HP"
            suffix="HP"
            :disable="finished"
            :rules="[(value) => Number(value) > 0 || 'HP must be positive']"
          />
          <q-input
            v-model="form.externalReference"
            outlined
            dense
            label="External reference (optional)"
            maxlength="500"
            placeholder="Milestone URL or reference"
          />
          <p v-if="mode === 'create'" class="muted note">New Raids start as drafts with full HP.</p>
          <p v-else-if="raid?.status === 'ACTIVE'" class="muted note">
            Changing maximum HP preserves damage already taken.
          </p>
          <p v-else-if="finished" class="muted note">
            Historical HP is locked; only metadata can be edited.
          </p>
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        </div>
        <footer>
          <q-btn flat no-caps label="Cancel" :disable="submitting" @click="open = false" />
          <q-btn
            unelevated
            no-caps
            color="primary"
            type="submit"
            :label="mode === 'create' ? 'Create draft' : 'Save changes'"
            :loading="submitting"
            :disable="!valid || submitting"
          />
        </footer>
      </q-form>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.raid-form-dialog {
  width: 520px;
  max-width: calc(100vw - 32px);
  color: var(--ink);
}
header {
  align-items: flex-start;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}
h2 {
  margin: 7px 0 0;
  font-size: 22px;
}
.fields {
  display: grid;
  gap: 17px;
  padding: 24px;
}
.note,
.form-error {
  margin: 0;
  font-size: 13px;
}
.form-error {
  color: var(--red);
}
footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}
</style>
