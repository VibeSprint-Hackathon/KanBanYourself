<script setup lang="ts">
import { computed } from 'vue';
import { achievementPercent, type AchievementSummaryState } from './achievement.types';

const props = defineProps<{ summary: AchievementSummaryState }>();
const percent = computed(() => achievementPercent(props.summary));
</script>

<template>
  <q-card flat bordered class="achievement-summary dashboard-card">
    <div class="summary-count">
      <div class="eyebrow blue">Achievements</div>
      <strong>{{ summary.unlocked }} unlocked</strong>
      <span>{{ summary.total }} total</span>
    </div>
    <div class="summary-progress">
      <div class="spread">
        <strong>Overall progress</strong>
        <span class="blue">{{ summary.unlocked }} / {{ summary.total }}</span>
      </div>
      <q-linear-progress
        class="progress-track"
        :value="summary.total ? summary.unlocked / summary.total : 0"
        size="14px"
        aria-label="Overall achievement progress"
      />
    </div>
    <div class="summary-percent">
      <strong>{{ percent }}%</strong>
      <span>complete</span>
    </div>
  </q-card>
</template>

<style scoped>
.achievement-summary {
  display: grid;
  grid-template-columns: 220px 1fr 120px;
  min-height: 112px;
  align-items: center;
  padding: 20px 24px;
}
.summary-count {
  padding-right: 24px;
  border-right: 1px solid var(--border);
}
.summary-count strong,
.summary-count span {
  display: block;
}
.summary-count strong {
  margin-top: 10px;
  font-size: 23px;
  line-height: 1.1;
}
.summary-count span {
  margin-top: 7px;
  color: var(--muted);
  font-size: 12px;
}
.summary-progress {
  padding: 0 24px;
  font-size: 12px;
}
.summary-progress .spread > span {
  font-weight: 600;
}
.summary-progress .progress-track {
  margin-top: 12px;
}
.summary-percent {
  text-align: right;
}
.summary-percent strong,
.summary-percent span {
  display: block;
}
.summary-percent strong {
  color: var(--blue);
  font-size: 31px;
  line-height: 1;
}
.summary-percent span {
  margin-top: 7px;
  color: var(--muted);
  font: 12px monospace;
}
@media (max-width: 900px) {
  .achievement-summary {
    grid-template-columns: 180px 1fr;
  }
  .summary-percent {
    display: none;
  }
}
</style>
