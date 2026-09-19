<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { mdiSwordCross, mdiTrophyOutline } from '@quasar/extras/mdi-v7';
import type { ProgressionResult } from '@/api/demo.types';
import { useSoundEffect } from '@/composables/useSoundEffect';
import { useDemoStore } from '@/stores/demo';

const RAID_HIT_SOUND_URL = '/sounds/raid-hit.mp3';
const MAX_PENDING_NOTIFICATIONS = 12;

interface DamageNotification {
  eventId: string;
  damage: number;
  raidName: string;
  currentHp: number;
  maxHp: number;
  defeated: boolean;
}

const store = useDemoStore();
const { progressionEvents } = storeToRefs(store);
const current = ref<DamageNotification | null>(null);
const pending: DamageNotification[] = [];
const sound = useSoundEffect(RAID_HIT_SOUND_URL);
let dismissTimer: ReturnType<typeof setTimeout> | undefined;

watch(progressionEvents, (events, previousEvents) => {
  const previousIds = new Set(previousEvents.map(({ eventId }) => eventId));
  events.filter(({ eventId }) => !previousIds.has(eventId)).forEach(enqueueProgression);
});

onUnmounted(() => {
  if (dismissTimer) clearTimeout(dismissTimer);
});

function enqueueProgression(progression: ProgressionResult): void {
  if (!progression.applied || progression.raidDamage <= 0 || progression.raid === null) return;

  const notification: DamageNotification = {
    eventId: progression.eventId,
    damage: progression.raidDamage,
    raidName: progression.raid.name,
    currentHp: progression.raid.currentHp,
    maxHp: progression.raid.maxHp,
    defeated: progression.bossDefeated || progression.raid.status === 'COMPLETED',
  };

  if (pending.length >= MAX_PENDING_NOTIFICATIONS) pending.shift();
  pending.push(notification);
  if (current.value === null) showNext();
}

function showNext(): void {
  current.value = pending.shift() ?? null;
  if (current.value === null) return;

  sound.play();
  dismissTimer = setTimeout(
    () => {
      current.value = null;
      dismissTimer = setTimeout(showNext, 180);
    },
    current.value.defeated ? 4_200 : 3_200,
  );
}

function formatNumber(value: number): string {
  return value.toLocaleString('en-US');
}
</script>

<template>
  <div class="raid-notification-host" aria-live="polite" aria-atomic="true">
    <Transition name="raid-impact">
      <q-card
        v-if="current"
        :key="current.eventId"
        flat
        bordered
        class="raid-damage-notification"
        :class="{ defeated: current.defeated }"
        role="status"
      >
        <div class="impact-icon" aria-hidden="true">
          <q-icon :name="current.defeated ? mdiTrophyOutline : mdiSwordCross" size="28px" />
        </div>
        <div class="notification-copy">
          <span class="impact-label">
            {{ current.defeated ? 'Boss defeated' : 'Boss hit' }}
          </span>
          <strong class="damage-value">-{{ formatNumber(current.damage) }} HP</strong>
          <span class="raid-name">
            {{ current.defeated ? `${current.raidName} has fallen!` : current.raidName }}
          </span>
          <span v-if="!current.defeated" class="hp-remaining">
            {{ formatNumber(current.currentHp) }} / {{ formatNumber(current.maxHp) }} HP remaining
          </span>
        </div>
      </q-card>
    </Transition>
  </div>
</template>

<style scoped>
.raid-notification-host {
  position: fixed;
  z-index: 7000;
  top: 24px;
  right: 24px;
  width: min(360px, calc(100vw - 32px));
  pointer-events: none;
}
.raid-damage-notification {
  position: relative;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 15px;
  overflow: hidden;
  padding: 18px;
  border-color: #ee8e86;
  border-radius: 9px;
  color: var(--ink);
  background: linear-gradient(135deg, #fff8f5, var(--card));
  box-shadow: 0 13px 34px #2b455b38, 0 0 22px #ed5a4e24;
}
.raid-damage-notification::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 5px;
  background: var(--red);
  content: '';
}
.raid-damage-notification.defeated {
  border-color: #6fbe99;
  background: linear-gradient(135deg, #effcf6, var(--card));
  box-shadow: 0 13px 34px #2b455b38, 0 0 26px #329c7040;
}
.raid-damage-notification.defeated::before {
  background: var(--green);
}
.impact-icon {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--red);
  box-shadow: 0 5px 12px #ed5a4e45;
}
.defeated .impact-icon {
  background: var(--green);
  box-shadow: 0 5px 12px #329c7045;
}
.notification-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
}
.impact-label {
  color: var(--red);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}
.defeated .impact-label {
  color: var(--green);
}
.damage-value {
  margin-top: 2px;
  color: var(--red);
  font-size: 31px;
  font-weight: 850;
  line-height: 1.08;
  letter-spacing: -0.7px;
  animation: damage-punch 360ms ease-out both;
}
.defeated .damage-value {
  color: var(--green);
}
.raid-name {
  margin-top: 5px;
  overflow: hidden;
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hp-remaining {
  margin-top: 2px;
  color: var(--muted);
  font-size: 12px;
}
.raid-impact-enter-active,
.raid-impact-leave-active {
  transition: opacity 180ms ease, transform 240ms ease;
}
.raid-impact-enter-from,
.raid-impact-leave-to {
  opacity: 0;
  transform: translateX(24px) scale(0.96);
}
@keyframes damage-punch {
  0% {
    transform: scale(0.82);
  }
  55% {
    transform: scale(1.08);
  }
  100% {
    transform: scale(1);
  }
}
@media (max-width: 700px) {
  .raid-notification-host {
    top: 16px;
    right: 16px;
    left: 16px;
    width: auto;
  }
}
@media (prefers-reduced-motion: reduce) {
  .raid-impact-enter-active,
  .raid-impact-leave-active {
    transition: opacity 120ms linear;
  }
  .raid-impact-enter-from,
  .raid-impact-leave-to {
    transform: none;
  }
  .damage-value {
    animation: none;
  }
}
</style>

<style>
@media (min-width: 1100px) {
  body:has(.quest-dialog) .raid-notification-host {
    right: 444px;
  }
}
</style>
