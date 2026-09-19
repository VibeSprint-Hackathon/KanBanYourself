<script setup lang="ts">
import { mdiCheck, mdiLockOutline } from '@quasar/extras/mdi-v7';
import { achievementRewardLabel, type Achievement } from './achievement.types';

defineProps<{ achievement: Achievement }>();
defineEmits<{ open: [id: number] }>();
</script>

<template>
  <article
    class="achievement-card"
    :class="{ unlocked: achievement.unlocked, newly: achievement.newlyUnlocked }"
  >
    <button
      type="button"
      :aria-label="`Open achievement: ${achievement.name}`"
      @click="$emit('open', achievement.id)"
    >
      <div class="card-top spread">
        <span class="achievement-icon"><q-icon :name="achievement.icon" size="20px" /></span>
        <span class="card-status">
          <q-icon :name="achievement.unlocked ? mdiCheck : mdiLockOutline" size="17px" />
          {{ achievement.unlocked ? 'Unlocked' : 'Locked' }}
        </span>
      </div>
      <h3>{{ achievement.name }}</h3>
      <p>{{ achievement.description }}</p>
      <div class="card-reward">{{ achievementRewardLabel(achievement.reward) }}</div>
    </button>
  </article>
</template>

<style scoped>
.achievement-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--card);
  box-shadow: 0 4px 10px #385b7410;
}
.achievement-card.unlocked {
  border-color: #abd8c7;
}
.achievement-card.newly {
  border-color: var(--green);
  background: #effaf6cc;
  box-shadow: 0 5px 14px #329c7026;
}
button {
  display: block;
  width: 100%;
  min-height: 168px;
  padding: 16px;
  border: 0;
  border-radius: inherit;
  color: var(--ink);
  font: inherit;
  text-align: left;
  background: transparent;
  cursor: pointer;
}
button:hover {
  background: #eaf7fd66;
}
.achievement-icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid var(--border);
  border-radius: 5px;
  color: var(--muted);
  background: #e6f2fa;
}
.unlocked .achievement-icon,
.newly .achievement-icon {
  color: var(--green);
  border-color: #abd8c7;
  background: #e3f4ed;
}
.card-status {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
}
.unlocked .card-status,
.newly .card-status {
  color: var(--green);
}
h3 {
  margin: 13px 0 0;
  font-size: 16px;
  line-height: 1.3;
  font-weight: 750;
}
p {
  min-height: 20px;
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}
.card-reward {
  margin-top: 13px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
  color: var(--orange);
  font-size: 14px;
  font-weight: 700;
}
</style>
