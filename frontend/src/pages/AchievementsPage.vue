<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { mdiMedalOutline } from '@quasar/extras/mdi-v7';
import AchievementSummary from '@/components/achievements/AchievementSummary.vue';
import FeaturedAchievement from '@/components/achievements/FeaturedAchievement.vue';
import AchievementCard from '@/components/achievements/AchievementCard.vue';
import AchievementDetailsDrawer from '@/components/achievements/AchievementDetailsDrawer.vue';
import {
  achievementIcon,
  type Achievement,
  type AchievementFilter,
  type AchievementResponse,
} from '@/components/achievements/achievement.types';
import { getPlayerAchievements } from '@/api/achievements';
import { achievementPagePresentation as view } from '@/fixtures/achievements.fixture';
import { useDemoStore } from '@/stores/demo';

const demoStore = useDemoStore();
const { selectedPlayer, lastProgression } = storeToRefs(demoStore);
const response = ref<AchievementResponse | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const filter = ref<AchievementFilter>('ALL');
const selectedKey = ref<string | null>(null);
const detailsOpen = ref(false);
let requestNumber = 0;
let stopRealtime: (() => void) | undefined;

const achievements = computed<Achievement[]>(() =>
  (response.value?.achievements ?? []).map((achievement) => ({
    ...achievement,
    icon: achievementIcon(achievement),
  })),
);
const summary = computed(() => ({
  unlocked: response.value?.unlocked ?? 0,
  total: response.value?.total ?? 10,
}));
const featured = computed(() => achievements.value.find(({ featured }) => featured));
const visibleAchievements = computed(() => {
  if (filter.value === 'UNLOCKED') return achievements.value.filter(({ unlocked }) => unlocked);
  if (filter.value === 'LOCKED') return achievements.value.filter(({ unlocked }) => !unlocked);
  return achievements.value;
});
const selectedAchievement = computed(() =>
  achievements.value.find(({ key }) => key === selectedKey.value),
);

async function loadAchievements(playerId = selectedPlayer.value?.id): Promise<void> {
  if (!playerId) return;
  const currentRequest = ++requestNumber;
  loading.value = true;
  error.value = null;
  try {
    const next = await getPlayerAchievements(playerId);
    if (currentRequest === requestNumber && playerId === selectedPlayer.value?.id) {
      response.value = next;
    }
  } catch {
    if (currentRequest === requestNumber) {
      error.value = 'Achievements could not be loaded. Check the backend and try again.';
    }
  } finally {
    if (currentRequest === requestNumber) loading.value = false;
  }
}

function openDetails(key: string): void {
  selectedKey.value = key;
  detailsOpen.value = true;
}

watch(
  () => selectedPlayer.value?.id,
  (playerId) => {
    response.value = null;
    selectedKey.value = null;
    detailsOpen.value = false;
    if (playerId) void loadAchievements(playerId);
  },
  { immediate: true },
);

watch(lastProgression, (progression) => {
  const playerId = progression?.player.id;
  if (playerId && playerId === selectedPlayer.value?.id) void loadAchievements(playerId);
});

onMounted(() => {
  stopRealtime = demoStore.startRealtime();
  if (!demoStore.state) void demoStore.loadState();
});

onBeforeUnmount(() => stopRealtime?.());
</script>

<template>
  <q-page class="achievements-page">
    <header class="page-header spread">
      <div>
        <div class="sprint-label blue">{{ view.sprintLabel }}</div>
        <h1>Achievements</h1>
      </div>
      <div class="header-meta">
        <span class="github-sync muted">
          <q-icon name="sensors" size="17px" class="green" />{{ view.syncLabel }}
        </span>
        <span class="header-time">
          <span class="muted">Profile</span>{{ selectedPlayer?.name ?? 'Loading…' }}
        </span>
      </div>
    </header>

    <AchievementSummary :summary="summary" />
    <FeaturedAchievement v-if="featured" :achievement="featured" @open="openDetails" />

    <section class="collection-section">
      <div class="collection-heading spread">
        <div>
          <h2>Achievement collection</h2>
          <p>Milestones earned through Quests, Raids, and Level progress.</p>
        </div>
        <div class="achievement-filters" role="group" aria-label="Filter achievements">
          <q-btn
            v-for="option in ['ALL', 'UNLOCKED', 'LOCKED'] as AchievementFilter[]"
            :key="option"
            flat
            no-caps
            :label="option.charAt(0) + option.slice(1).toLowerCase()"
            :class="{ active: filter === option }"
            :aria-pressed="filter === option"
            @click="filter = option"
          />
        </div>
      </div>

      <div v-if="loading && !response" class="empty-filter-state" role="status">
        <q-spinner color="primary" size="32px" />
        <strong>Loading Achievements…</strong>
      </div>
      <div v-else-if="error" class="empty-filter-state" role="alert">
        <q-icon name="warning_amber" size="27px" />
        <strong>{{ error }}</strong>
        <q-btn outline no-caps color="primary" label="Retry" @click="loadAchievements()" />
      </div>
      <div v-else-if="visibleAchievements.length" class="achievement-grid">
        <AchievementCard
          v-for="achievement in visibleAchievements"
          :key="achievement.key"
          :achievement="achievement"
          @open="openDetails"
        />
      </div>
      <div v-else class="empty-filter-state">
        <q-icon :name="mdiMedalOutline" size="27px" />
        <strong>No Achievements here yet.</strong>
      </div>
    </section>

    <AchievementDetailsDrawer
      v-if="selectedAchievement"
      v-model="detailsOpen"
      :achievement="selectedAchievement"
    />
  </q-page>
</template>

<style scoped>
.achievements-page { padding: 28px 36px 48px; }
.page-header { min-height: 54px; margin-bottom: 22px; }
.sprint-label { margin-bottom: 5px; font-size: 13px; }
h1 { margin: 0; font-size: 28px; line-height: 1.2; font-weight: 750; letter-spacing: -0.5px; }
.header-meta { display: flex; align-items: center; gap: 27px; font-size: 13px; }
.github-sync { display: flex; align-items: center; gap: 9px; }
.header-time { display: flex; gap: 18px; padding: 10px 0 10px 27px; border-left: 1px solid var(--border); font-family: monospace; }
.collection-section { margin-top: 23px; }
.collection-heading { align-items: flex-end; margin-bottom: 17px; }
.collection-heading h2 { margin: 0; font-size: 18px; line-height: 1.3; font-weight: 750; }
.collection-heading p { margin: 7px 0 0; color: var(--muted); font-size: 14px; }
.achievement-filters { display: grid; grid-template-columns: repeat(3, 84px); padding: 4px; border: 1px solid var(--border); border-radius: 6px; background: var(--card); }
.achievement-filters .q-btn { min-height: 30px; color: var(--ink); font-size: 12px; }
.achievement-filters .q-btn.active { color: var(--blue); background: #cce8f6; }
.achievement-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.empty-filter-state { display: grid; min-height: 360px; place-content: center; justify-items: center; gap: 16px; border: 1px dashed var(--border); border-radius: 6px; color: var(--blue); background: #f4fbff66; text-align: center; }
.empty-filter-state strong { max-width: 460px; color: var(--ink); font-size: 15px; }
@media (max-width: 1200px) { .achievements-page { padding: 24px; } }
@media (max-width: 950px) { .achievement-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .header-time { display: none; } }
@media (max-width: 700px) {
  .achievements-page { padding: 18px; }
  .page-header, .collection-heading { align-items: flex-start; flex-direction: column; gap: 16px; }
  .achievement-grid { grid-template-columns: 1fr; }
  .achievement-filters { width: 100%; grid-template-columns: repeat(3, 1fr); }
}
</style>
