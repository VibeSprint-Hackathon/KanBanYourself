<script setup lang="ts">
import { computed } from 'vue';
import { formatNumber } from '@/fixtures/dashboard.fixture';
import { raidPercent, type RaidPresentation } from './raid.types';

const open = defineModel<boolean>({ required: true });
const props = defineProps<{ entry: RaidPresentation }>();
const defeated = computed(() => props.entry.raid.status === 'DEFEATED');
const percent = computed(() => raidPercent(props.entry.raid));
</script>

<template>
  <q-dialog
    v-model="open"
    position="right"
    full-height
    class="quest-dialog"
    :transition-duration="0"
    aria-labelledby="raid-details-title"
  >
    <q-card flat class="raid-drawer">
      <header class="drawer-header spread">
        <div>
          <div class="eyebrow blue">Raid details</div>
          <h2 id="raid-details-title">{{ entry.raid.name }}</h2>
        </div>
        <q-btn
          flat
          square
          icon="close"
          class="close-icon"
          aria-label="Close Raid details"
          @click="open = false"
        />
      </header>
      <div class="drawer-body">
        <div class="spread">
          <span class="eyebrow muted">Status</span>
          <span class="raid-badge" :class="{ defeated }">● {{ entry.raid.status }}</span>
        </div>
        <section>
          <div class="spread">
            <h3 class="eyebrow muted">Boss HP</h3>
            <strong :class="defeated ? 'green' : 'red'">{{ percent }}%</strong>
          </div>
          <div class="drawer-hp">
            {{ formatNumber(entry.raid.currentHp) }} / {{ formatNumber(entry.raid.maxHp) }}
          </div>
          <q-linear-progress
            class="progress-track hp-progress"
            :class="{ 'defeated-progress': defeated }"
            :value="entry.raid.maxHp > 0 ? entry.raid.currentHp / entry.raid.maxHp : 0"
            aria-label="Boss HP"
            size="13px"
          />
        </section>
        <div class="contribution-box">
          <span class="eyebrow muted">Your contribution</span>
          <strong>{{ formatNumber(entry.playerDamage) }} damage</strong>
        </div>
        <div class="reward-box spread">
          <span class="eyebrow orange">{{ defeated ? 'Reward earned' : 'Reward' }}</span>
          <strong>{{ formatNumber(entry.rewardXp) }} XP</strong>
        </div>
        <section>
          <h3 class="eyebrow muted">How Raids work</h3>
          <p>Complete Quests to damage the Raid Boss.</p>
        </section>
      </div>
      <footer class="drawer-footer">
        <q-btn flat no-caps label="Close" @click="open = false" />
      </footer>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.raid-drawer {
  display: flex;
  flex-direction: column;
  width: 420px;
  max-width: 100vw;
  height: 100%;
  max-height: 100% !important;
  border-radius: 0;
  color: var(--ink);
  background: var(--card);
}
.drawer-header {
  align-items: flex-start;
  gap: 16px;
  padding: 22px 24px;
  border-bottom: 1px solid var(--border);
}
h2 {
  margin: 8px 0 0;
  font-size: 21px;
  font-weight: 750;
}
.close-icon {
  width: 40px;
  min-width: 40px;
  min-height: 40px;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: 5px;
  color: var(--muted);
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 25px 24px;
}
.raid-badge {
  padding: 5px 11px;
  border-radius: 18px;
  color: var(--red);
  background: #fae8e7;
  font-size: 11px;
  letter-spacing: 1px;
}
.raid-badge.defeated {
  color: var(--green);
  background: #dcefe8;
}
section {
  margin-top: 24px;
}
section h3 {
  margin: 0;
}
.drawer-hp {
  margin-top: 12px;
  font-size: 19px;
  font-weight: 750;
}
.hp-progress {
  margin-top: 15px;
}
.defeated-progress {
  color: var(--green);
  border-color: #abd8c7;
}
.contribution-box,
.reward-box {
  margin-top: 20px;
  padding: 15px 16px;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: #eaf6fd;
}
.contribution-box strong {
  display: block;
  margin-top: 9px;
  font-size: 18px;
}
.reward-box {
  border-color: #efca8c;
  background: #fff8ea99;
}
.reward-box strong {
  color: var(--orange);
  font-size: 19px;
}
section p {
  margin: 12px 0 0;
  color: #43596b;
  line-height: 1.65;
}
.drawer-footer {
  padding: 20px 24px;
  border-top: 1px solid var(--border);
}
.drawer-footer .q-btn {
  width: 100%;
  height: 46px;
  border: 1px solid var(--border);
  color: var(--muted);
  font-weight: 600;
}
</style>
