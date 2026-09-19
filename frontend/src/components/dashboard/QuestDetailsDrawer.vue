<script setup lang="ts">
import { computed } from 'vue';
import type { Player, Quest } from '@/api/demo.types';
import { safeReferenceUrl } from '@/components/quests/board.types';
import {
  formatNumber,
  questStatusLabels,
  type QuestPresentation,
} from '@/fixtures/dashboard.fixture';
const open = defineModel<boolean>({ required: true });
const props = withDefaults(
  defineProps<{
    quest: Quest;
    assignee: Player | undefined;
    presentation: QuestPresentation;
    completing?: boolean;
    mutating?: boolean;
    mode?: 'dashboard' | 'board';
    statusLabel?: string;
    completed?: boolean;
    showProgress?: boolean;
    statusColor?: string;
  }>(),
  { completing: false, mutating: false, mode: 'dashboard', showProgress: true },
);
defineEmits<{ complete: []; edit: []; delete: [] }>();
const isCompleted = computed(() =>
  props.mode === 'board' ? props.completed : props.quest.status === 'DONE',
);
const referenceUrl = computed(() => safeReferenceUrl(props.quest.externalReference));
</script>

<template>
  <q-dialog
    v-model="open"
    position="right"
    full-height
    class="quest-dialog"
    :transition-duration="0"
    aria-labelledby="quest-details-title"
  >
    <q-card flat class="quest-drawer">
      <header class="drawer-header spread">
        <div>
          <div class="eyebrow blue">Quest details</div>
          <h2 id="quest-details-title">{{ quest.title }}</h2>
        </div>
        <q-btn
          flat
          square
          icon="close"
          class="close-icon"
          aria-label="Close quest details"
          @click="open = false"
        />
      </header>
      <div class="drawer-body">
        <div class="spread">
          <span class="eyebrow muted">Status</span
          ><span
            class="quest-badge"
            :class="{ completed: isCompleted }"
            :style="statusColor ? { color: statusColor } : undefined"
            >● {{ statusLabel ?? questStatusLabels[quest.status] }}</span
          >
        </div>
        <section>
          <h3 class="eyebrow muted">Description</h3>
          <p>{{ presentation.description }}</p>
        </section>
        <section>
          <h3 class="eyebrow muted">Assignee</h3>
          <p>{{ assignee?.name ?? 'Unknown player' }}</p>
        </section>
        <section v-if="showProgress">
          <div class="spread">
            <h3 class="eyebrow muted">Progress</h3>
            <strong class="blue">{{ presentation.progressPercent }}%</strong>
          </div>
          <q-linear-progress
            class="progress-track"
            :value="presentation.progressPercent / 100"
            aria-label="Quest progress"
            size="13px"
          />
        </section>
        <div class="drawer-reward spread orange">
          <span class="eyebrow">Reward</span><strong>+{{ formatNumber(quest.xpReward) }} XP</strong>
        </div>
        <section v-if="quest.externalReference || presentation.pullRequestLabel">
          <h3 class="eyebrow muted">GitHub</h3>
          <p class="linked-label">Linked pull request</p>
          <div class="pull-request">
            <q-icon name="merge" size="17px" class="green" />
            <a v-if="referenceUrl" :href="referenceUrl" target="_blank" rel="noopener noreferrer">{{
              quest.externalReference
            }}</a>
            <span v-else>{{ presentation.pullRequestLabel || quest.externalReference }}</span>
          </div>
        </section>
      </div>
      <footer class="drawer-footer">
        <q-btn
          v-if="mode === 'dashboard'"
          unelevated
          no-caps
          class="complete-button"
          :label="quest.status === 'DONE' ? 'Quest completed' : 'Complete Quest'"
          aria-label="Complete Quest"
          :loading="completing"
          :disable="completing || quest.status === 'DONE'"
          @click="$emit('complete')"
        />
        <template v-else>
          <q-btn
            v-if="!isCompleted"
            unelevated
            no-caps
            class="complete-button"
            label="Complete Quest"
            :loading="completing"
            :disable="completing || mutating"
            @click="$emit('complete')"
          />
          <q-btn
            flat
            no-caps
            label="Edit Quest"
            :disable="completing || mutating"
            @click="$emit('edit')"
          />
          <q-btn
            v-if="!isCompleted"
            flat
            no-caps
            class="red"
            label="Delete Quest"
            :disable="completing || mutating"
            @click="$emit('delete')"
          />
        </template>
        <q-btn flat no-caps class="drawer-close" label="Close" @click="open = false" />
      </footer>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.quest-drawer {
  width: 470px;
  max-width: 100vw;
  height: 100%;
  max-height: 100% !important;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  color: var(--ink);
  background: var(--card);
}
.drawer-header {
  padding: 25px 27px;
  gap: 16px;
  border-bottom: 1px solid var(--border);
  align-items: flex-start;
}
h2 {
  margin: 9px 0 0;
  font-size: 22px;
  font-weight: 750;
  line-height: 1.35;
}
.close-icon {
  padding: 0;
  width: 40px;
  flex-shrink: 0;
  border: 1px solid var(--border);
  color: var(--muted);
  border-radius: 5px;
  min-width: 40px;
  min-height: 40px;
}
.drawer-body {
  padding: 24px 27px;
  overflow-y: auto;
  flex: 1;
}
section {
  margin-top: 26px;
}
section h3 {
  margin: 0;
}
section p {
  font-size: 15px;
  line-height: 1.75;
  margin: 12px 0 0;
  color: #43596b;
}
section .progress-track {
  margin-top: 16px;
}
.drawer-reward {
  margin-top: 22px;
  padding: 16px 18px;
  border: 1px solid #efca8c;
  background: #fff0d466;
  border-radius: 5px;
}
.drawer-reward strong {
  font-size: 20px;
}
section .linked-label {
  color: var(--ink);
  font-weight: 500;
  margin-bottom: 6px;
}
.pull-request {
  display: flex;
  gap: 8px;
  align-items: center;
  font: 13px monospace;
  color: var(--muted);
}
.pull-request a,
.pull-request span {
  overflow-wrap: anywhere;
  color: inherit;
}
.drawer-footer {
  padding: 22px 27px;
  border-top: 1px solid var(--border);
  display: grid;
  gap: 12px;
}
.drawer-footer .q-btn {
  height: 50px;
  font-weight: 600;
}
.complete-button {
  color: white;
  background: var(--blue);
}
.complete-button:not(.disabled) {
  cursor: pointer;
}
.drawer-close {
  color: var(--muted);
  border: 1px solid var(--border);
}
</style>
