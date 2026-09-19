<script setup lang="ts">
import { mdiCheck } from '@quasar/extras/mdi-v7';
import type { RaidPresentation } from './raid.types';
import { formatNumber } from '@/fixtures/dashboard.fixture';

defineProps<{ entry: RaidPresentation }>();
</script>

<template>
  <q-card flat bordered class="completed-raid-card">
    <div class="spread">
      <h3>{{ entry.raid.name }}</h3>
      <span class="muted">{{ entry.completedAt }}</span>
    </div>
    <div class="completed-status"><q-icon :name="mdiCheck" size="17px" /> Defeated</div>
    <div class="completed-reward">+{{ formatNumber(entry.rewardXp) }} XP</div>
  </q-card>
</template>

<style scoped>
.completed-raid-card {
  min-height: 149px;
  padding: 22px 20px 18px;
  border-color: #abd8c7;
  border-radius: 7px;
  background: var(--card);
  box-shadow: 0 4px 12px #385b7410;
}
h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 750;
}
.spread > span {
  font-size: 12px;
}
.completed-status {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 11px;
  color: var(--green);
  font-size: 13px;
  font-weight: 600;
}
.completed-reward {
  margin-top: 26px;
  padding-top: 15px;
  border-top: 1px solid var(--border);
  color: var(--orange);
  font-size: 14px;
  font-weight: 700;
}
</style>
