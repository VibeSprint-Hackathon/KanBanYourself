<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import { mdiShieldOutline } from '@quasar/extras/mdi-v7';
import ActiveRaidCard from '@/components/raids/ActiveRaidCard.vue';
import CompletedRaidCard from '@/components/raids/CompletedRaidCard.vue';
import RaidDetailsDrawer from '@/components/raids/RaidDetailsDrawer.vue';
import type { RaidPreviewState } from '@/components/raids/raid.types';
import {
  completedRaidFixtures,
  initialRaidPreviewState,
  raidDamageFixture,
  raidFixtures,
  raidPagePresentation as view,
} from '@/fixtures/raids.fixture';

const route = useRoute();
const detailsOpen = ref(false);
const previewStates: Record<string, RaidPreviewState> = {
  active: 'ACTIVE',
  damaged: 'DAMAGED',
  defeated: 'DEFEATED',
  empty: 'EMPTY',
};
const previewState = computed<RaidPreviewState>(() => {
  if (!import.meta.env.DEV) return initialRaidPreviewState;
  const requested = String(route.query.raidState ?? '').toLowerCase();
  return previewStates[requested] ?? initialRaidPreviewState;
});
const activeRaid = computed(() =>
  previewState.value === 'EMPTY' ? null : raidFixtures[previewState.value],
);
const damage = computed(() => (previewState.value === 'DAMAGED' ? raidDamageFixture : null));
const completedRaids = computed(() => {
  const current = activeRaid.value?.raid.status === 'DEFEATED' ? [activeRaid.value] : [];
  const ids = new Set(current.map(({ raid }) => raid.id));
  return [...current, ...completedRaidFixtures.filter(({ raid }) => !ids.has(raid.id))];
});
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
          <q-icon name="sensors" size="17px" class="green" />{{ view.syncLabel }}
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

    <ActiveRaidCard
      v-if="activeRaid"
      :entry="activeRaid"
      :damage="damage"
      @open="detailsOpen = true"
    />
    <q-card v-else flat bordered class="empty-raid-card">
      <div class="empty-icon"><q-icon :name="mdiShieldOutline" size="24px" /></div>
      <h2>No active Raid</h2>
      <p>The next Boss will appear when a new sprint begins.</p>
    </q-card>

    <section class="completed-section">
      <div class="completed-heading spread">
        <div>
          <h2>Completed Raids</h2>
          <p>Bosses defeated through team Quest progress.</p>
        </div>
        <span class="muted">{{ completedRaids.length }} defeated</span>
      </div>
      <div class="completed-grid">
        <CompletedRaidCard v-for="entry in completedRaids" :key="entry.raid.id" :entry="entry" />
      </div>
    </section>

    <RaidDetailsDrawer v-if="activeRaid" v-model="detailsOpen" :entry="activeRaid" />
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
.empty-raid-card h2 {
  font-size: 24px;
}
.completed-section {
  margin-top: 27px;
}
.completed-heading {
  align-items: flex-end;
  margin-bottom: 18px;
}
.completed-heading > span {
  font: 13px monospace;
}
.completed-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
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
  .completed-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
  .completed-grid {
    grid-template-columns: 1fr;
  }
}
</style>
