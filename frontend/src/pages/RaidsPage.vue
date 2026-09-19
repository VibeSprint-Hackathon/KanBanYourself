<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { mdiShieldOutline } from '@quasar/extras/mdi-v7';
import ActiveRaidCard from '@/components/raids/ActiveRaidCard.vue';
import RaidDetailsDrawer from '@/components/raids/RaidDetailsDrawer.vue';
import type { RaidDamageFeedback } from '@/components/raids/raid.types';
import { raidPagePresentation as view } from '@/fixtures/raids.fixture';
import { useDemoStore } from '@/stores/demo';

const store = useDemoStore();
const { state, loading, error, realtimeStatus, lastProgression } = storeToRefs(store);
const detailsOpen = ref(false);
const raid = computed(() => state.value?.raid ?? null);
const damage = computed<RaidDamageFeedback | null>(() => {
  const progression = lastProgression.value;
  if (!progression?.applied || progression.raid.id !== raid.value?.id) {
    return null;
  }
  return {
    previousHp: Math.min(
      progression.raid.maxHp,
      progression.raid.currentHp + progression.raidDamage,
    ),
    damageReceived: progression.raidDamage,
  };
});

let stopRealtime: (() => void) | undefined;

onMounted(() => {
  stopRealtime = store.startRealtime();
  if (state.value === null) {
    void store.loadState();
  }
});

onUnmounted(() => stopRealtime?.());
</script>

<template>
  <q-page class="raids-page">
    <header class="page-header spread">
      <div>
        <div class="sprint-label blue">{{ view.sprintLabel }}</div>
        <h1>Raids</h1>
      </div>
      <div class="header-meta">
        <span class="github-sync muted">
          <q-icon
            name="sensors"
            size="17px"
            :class="realtimeStatus === 'connected' ? 'green' : 'orange'"
          />{{ realtimeStatus === 'connected' ? 'Live sync' : 'Sync offline' }}
        </span>
        <span class="header-time"
          ><span class="muted">{{ view.dateLabel }}</span
          >{{ view.timeLabel }}</span
        >
      </div>
    </header>

    <section class="team-heading">
      <h2>Team Raid</h2>
      <p>Complete Quests together to bring down the active Boss.</p>
    </section>

    <q-card v-if="loading && !state" flat bordered class="state-card">
      <q-spinner color="primary" size="34px" />
      <strong>Loading Raid…</strong>
    </q-card>
    <q-card v-else-if="!state" flat bordered class="state-card error-card">
      <q-icon name="cloud_off" size="34px" class="red" />
      <strong>Could not load Raid</strong>
      <span class="muted">{{ error }}</span>
      <q-btn unelevated no-caps label="Try again" color="primary" @click="store.loadState()" />
    </q-card>
    <template v-else>
      <div v-if="error" class="sync-warning" role="alert">
        <q-icon name="warning" size="19px" />
        <span>{{ error }}</span>
        <q-btn flat dense no-caps label="Retry" @click="store.loadState()" />
      </div>
      <ActiveRaidCard v-if="raid" :raid="raid" :damage="damage" @open="detailsOpen = true" />
      <q-card v-else flat bordered class="empty-raid-card">
        <div class="empty-icon"><q-icon :name="mdiShieldOutline" size="24px" /></div>
        <h2>No active Raid</h2>
        <p>The next Boss will appear when a new sprint begins.</p>
      </q-card>

      <section class="completed-section">
        <div class="completed-heading">
          <h2>Raid history</h2>
          <p>Completed Raid history is not available in the current backend API.</p>
        </div>
        <q-card flat bordered class="history-note muted">
          This demo shows the current team Raid only.
        </q-card>
      </section>

      <RaidDetailsDrawer v-if="raid" v-model="detailsOpen" :raid="raid" />
    </template>
  </q-page>
</template>

<style scoped>
.raids-page {
  padding: 28px 36px 48px;
}
.page-header {
  min-height: 54px;
}
.sprint-label {
  margin-bottom: 5px;
  font-size: 13px;
}
h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  font-weight: 750;
  letter-spacing: -0.5px;
}
.header-meta {
  display: flex;
  align-items: center;
  gap: 27px;
  font-size: 13px;
}
.github-sync {
  display: flex;
  align-items: center;
  gap: 9px;
}
.header-time {
  display: flex;
  gap: 18px;
  padding: 10px 0 10px 27px;
  border-left: 1px solid var(--border);
  font-family: monospace;
}
.team-heading {
  margin: 16px 0 20px;
  padding: 0 0 19px;
  border-bottom: 1px solid var(--border);
}
.team-heading h2,
.completed-heading h2,
.empty-raid-card h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 750;
  line-height: 1.3;
}
.team-heading p,
.completed-heading p,
.empty-raid-card p {
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 14px;
}
.empty-raid-card {
  display: grid;
  min-height: 430px;
  place-content: center;
  justify-items: center;
  border-radius: 7px;
  background: var(--card);
  box-shadow: 0 5px 14px #385b7410;
}
.empty-icon {
  display: grid;
  width: 48px;
  height: 48px;
  margin-bottom: 21px;
  place-items: center;
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--blue);
  background: #e5f3fb;
}
.state-card {
  display: grid;
  min-height: 430px;
  place-content: center;
  justify-items: center;
  gap: 16px;
  text-align: center;
}
.error-card {
  border-color: #ffaaa3;
}
.sync-warning {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 14px;
  color: #75442d;
  border: 1px solid #efca8c;
  border-radius: 6px;
  background: #fff0d4;
}
.sync-warning span {
  flex: 1;
}
.empty-raid-card h2 {
  font-size: 24px;
}
.completed-section {
  margin-top: 27px;
}
.completed-heading {
  margin-bottom: 14px;
}
.history-note {
  padding: 20px;
  border-style: dashed;
}
@media (max-width: 1200px) {
  .raids-page {
    padding: 24px;
  }
}
@media (max-width: 900px) {
  .header-time {
    display: none;
  }
}
@media (max-width: 700px) {
  .raids-page {
    padding: 18px;
  }
  .page-header {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
