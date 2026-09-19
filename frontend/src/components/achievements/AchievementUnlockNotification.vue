<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { mdiTrophyOutline } from '@quasar/extras/mdi-v7';
import type { AchievementUnlock } from '@/api/demo.types';
import { useDemoStore } from '@/stores/demo';

const DISPLAY_MS = 4200;
const demoStore = useDemoStore();
const { progressionEvents, selectedPlayer } = storeToRefs(demoStore);
const handled = new Set<string>();
const queue = ref<AchievementUnlock[]>([]);
const current = ref<AchievementUnlock | null>(null);
let timer: ReturnType<typeof setTimeout> | undefined;
let stopRealtime: (() => void) | undefined;

for (const event of progressionEvents.value) {
  for (const unlock of event.unlockedAchievements ?? []) handled.add(`${event.eventId}:${unlock.key}`);
}

watch(progressionEvents, (events) => {
  for (const event of events) {
    for (const unlock of event.unlockedAchievements ?? []) {
      const id = `${event.eventId}:${unlock.key}`;
      if (handled.has(id)) continue;
      handled.add(id);
      if (event.player.id === selectedPlayer.value?.id) queue.value.push(unlock);
    }
  }
  showNext();
});

watch(
  () => selectedPlayer.value?.id,
  () => {
    queue.value = [];
    current.value = null;
    if (timer) clearTimeout(timer);
    timer = undefined;
  },
);

function showNext(): void {
  if (current.value || queue.value.length === 0) return;
  current.value = queue.value.shift() ?? null;
  timer = setTimeout(() => {
    current.value = null;
    timer = undefined;
    showNext();
  }, DISPLAY_MS);
}

onMounted(() => {
  stopRealtime = demoStore.startRealtime();
});

onBeforeUnmount(() => {
  stopRealtime?.();
  if (timer) clearTimeout(timer);
});
</script>

<template>
  <div class="achievement-notification-region" aria-live="polite" aria-atomic="true">
    <Transition name="achievement-unlock">
      <article v-if="current" :key="current.key" class="achievement-notification" role="status">
        <span class="notification-icon"><q-icon :name="mdiTrophyOutline" size="25px" /></span>
        <div>
          <span>Achievement unlocked</span>
          <strong>{{ current.name }}</strong>
          <p>{{ current.description }}</p>
          <small>{{ current.rewardLabel }}</small>
        </div>
      </article>
    </Transition>
  </div>
</template>

<style scoped>
.achievement-notification-region {
  position: fixed;
  z-index: 7100;
  right: 28px;
  top: 28px;
  width: min(380px, calc(100vw - 36px));
  pointer-events: none;
}
.achievement-notification {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 14px;
  padding: 17px;
  border: 1px solid #e0bb73;
  border-radius: 8px;
  color: var(--ink);
  background: #fffdf7;
  box-shadow: 0 10px 30px #233d5238;
}
.notification-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 7px;
  color: var(--orange);
  background: #fff1cf;
}
.achievement-notification span {
  color: var(--orange);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 1.3px;
  text-transform: uppercase;
}
.achievement-notification strong { display: block; margin-top: 4px; font-size: 17px; }
.achievement-notification p { margin: 4px 0 0; color: var(--muted); font-size: 12px; line-height: 1.45; }
.achievement-notification small { display: block; margin-top: 8px; color: var(--green); font-weight: 700; }
.achievement-unlock-enter-active, .achievement-unlock-leave-active { transition: opacity 180ms ease, transform 180ms ease; }
.achievement-unlock-enter-from, .achievement-unlock-leave-to { opacity: 0; transform: translateY(-10px); }
@media (max-width: 700px) { .achievement-notification-region { top: auto; right: 18px; bottom: 18px; left: 18px; width: auto; } }
@media (prefers-reduced-motion: reduce) {
  .achievement-unlock-enter-active, .achievement-unlock-leave-active { transition: none; }
}
</style>
