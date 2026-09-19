<script setup lang="ts">
import { mdiCheck } from '@quasar/extras/mdi-v7';
import { achievementRewardLabel, type Achievement } from './achievement.types';

defineProps<{ achievement: Achievement }>();
defineEmits<{ open: [id: number] }>();
</script>

<template>
  <q-card
    flat
    bordered
    class="featured-achievement"
    role="button"
    tabindex="0"
    :aria-label="`Open achievement: ${achievement.name}`"
    @click="$emit('open', achievement.id)"
    @keydown.enter.prevent="$emit('open', achievement.id)"
    @keydown.space.prevent="$emit('open', achievement.id)"
  >
    <div class="featured-icon">
      <span class="featured-icon-box"><q-icon :name="achievement.icon" size="24px" /></span>
    </div>
    <div class="featured-copy">
      <div class="eyebrow green">Featured achievement</div>
      <h2>{{ achievement.name }}</h2>
      <p>{{ achievement.description }}</p>
    </div>
    <div class="featured-reward">
      <div class="unlocked-label"><q-icon :name="mdiCheck" size="18px" /> Unlocked</div>
      <span class="eyebrow orange">Reward</span>
      <strong>{{ achievementRewardLabel(achievement.reward) }}</strong>
    </div>
  </q-card>
</template>

<style scoped>
.featured-achievement {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) 218px;
  min-height: 150px;
  margin-top: 20px;
  overflow: hidden;
  border-color: #9fd4bf;
  border-radius: 7px;
  background: var(--card);
  cursor: pointer;
  box-shadow: 0 5px 14px #385b7410;
}
.featured-achievement:hover {
  border-color: var(--green);
}
.featured-achievement:focus-visible {
  outline: 3px solid var(--blue);
  outline-offset: 3px;
}
.featured-icon {
  display: grid;
  place-items: center;
  border-right: 1px solid var(--border);
  color: var(--green);
  background: #e9f7f1;
}
.featured-icon-box {
  display: grid;
  width: 64px;
  height: 64px;
  place-items: center;
  border: 1px solid #9fd4bf;
  border-radius: 6px;
  background: var(--card);
}
.featured-copy {
  align-self: center;
  padding: 22px 24px;
}
h2 {
  margin: 9px 0 0;
  font-size: 24px;
  line-height: 1.2;
  font-weight: 750;
}
p {
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 14px;
}
.featured-reward {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 20px 23px;
  border-left: 1px solid var(--border);
  background: #edf8fd;
}
.unlocked-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 17px;
  color: var(--green);
  font-size: 13px;
  font-weight: 600;
}
.featured-reward strong {
  margin-top: 7px;
  color: var(--orange);
  font-size: 20px;
}
@media (max-width: 900px) {
  .featured-achievement {
    grid-template-columns: 110px minmax(0, 1fr) 190px;
  }
}
</style>
