<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { mdiCheckCircle, mdiMedalOutline } from '@quasar/extras/mdi-v7';
import AchievementSummary from '@/components/achievements/AchievementSummary.vue';
import FeaturedAchievement from '@/components/achievements/FeaturedAchievement.vue';
import AchievementCard from '@/components/achievements/AchievementCard.vue';
import AchievementDetailsDrawer from '@/components/achievements/AchievementDetailsDrawer.vue';
import type {
  AchievementFilter,
  AchievementUnlockResult,
} from '@/components/achievements/achievement.types';
import {
  achievementFixtures,
  achievementPagePresentation as view,
  achievementSummaryFixture,
  achievementUnlockFixture,
} from '@/fixtures/achievements.fixture';

const route = useRoute();
const achievements = ref(structuredClone(achievementFixtures));
const summary = ref(structuredClone(achievementSummaryFixture));
const filter = ref<AchievementFilter>('ALL');
const selectedId = ref<number | null>(null);
const detailsOpen = ref(false);
const toast = ref<string | null>(null);

const featured = computed(() => achievements.value.find(({ featured }) => featured));
const emptyPreview = computed(
  () => import.meta.env.DEV && route.query.achievementState === 'empty',
);
const visibleAchievements = computed(() => {
  if (emptyPreview.value) return [];
  if (filter.value === 'UNLOCKED') return achievements.value.filter(({ unlocked }) => unlocked);
  if (filter.value === 'LOCKED') return achievements.value.filter(({ unlocked }) => !unlocked);
  return achievements.value;
});
const selectedAchievement = computed(() =>
  achievements.value.find(({ id }) => id === selectedId.value),
);

function openDetails(id: number) {
  selectedId.value = id;
  detailsOpen.value = true;
}

function applyUnlock(result: AchievementUnlockResult): boolean {
  const achievement = achievements.value.find(({ id }) => id === result.achievementId);
  if (!achievement || achievement.unlocked) return false;
  achievement.unlocked = true;
  achievement.currentProgress = achievement.targetProgress;
  achievement.unlockedAt = result.unlockedAt;
  achievement.newlyUnlocked = true;
  summary.value.unlocked += 1;
  return true;
}

watch(
  () => route.query.achievementState,
  (state) => {
    toast.value = null;
    if (import.meta.env.DEV && (state === 'newly-unlocked' || state === 'empty')) {
      filter.value = 'ALL';
    }
    if (
      import.meta.env.DEV &&
      state === 'newly-unlocked' &&
      applyUnlock(achievementUnlockFixture)
    ) {
      toast.value = achievementUnlockFixture.announcement;
    }
  },
  { immediate: true },
);
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
          <span class="muted">{{ view.dateLabel }}</span
          >{{ view.timeLabel }}
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

      <div v-if="visibleAchievements.length" class="achievement-grid">
        <AchievementCard
          v-for="achievement in visibleAchievements"
          :key="achievement.id"
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

    <div v-if="toast" class="achievement-toast" role="status" aria-live="polite">
      <q-icon :name="mdiCheckCircle" size="28px" />
      <strong>{{ toast }}</strong>
    </div>
  </q-page>
</template>

<style scoped>
.achievements-page {
  padding: 28px 36px 48px;
}
.page-header {
  min-height: 54px;
  margin-bottom: 22px;
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
.collection-section {
  margin-top: 23px;
}
.collection-heading {
  align-items: flex-end;
  margin-bottom: 17px;
}
.collection-heading h2 {
  margin: 0;
  font-size: 18px;
  line-height: 1.3;
  font-weight: 750;
}
.collection-heading p {
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 14px;
}
.achievement-filters {
  display: grid;
  grid-template-columns: repeat(3, 84px);
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--card);
}
.achievement-filters .q-btn {
  min-height: 30px;
  color: var(--ink);
  font-size: 12px;
}
.achievement-filters .q-btn.active {
  color: var(--blue);
  background: #cce8f6;
}
.achievement-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.empty-filter-state {
  display: grid;
  min-height: 360px;
  place-content: center;
  justify-items: center;
  gap: 16px;
  border: 1px dashed var(--border);
  border-radius: 6px;
  color: var(--blue);
  background: #f4fbff66;
}
.empty-filter-state strong {
  color: var(--ink);
  font-size: 15px;
}
.achievement-toast {
  position: fixed;
  z-index: 7000;
  right: 32px;
  top: 464px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 16px;
  border: 1px solid #8dceb2;
  border-radius: 6px;
  color: var(--green);
  background: var(--card);
  box-shadow: 0 6px 18px #28495c2d;
}
.achievement-toast strong {
  color: var(--ink);
  font-size: 14px;
}
@media (max-width: 1200px) {
  .achievements-page {
    padding: 24px;
  }
}
@media (max-width: 950px) {
  .achievement-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .header-time {
    display: none;
  }
}
@media (max-width: 700px) {
  .achievements-page {
    padding: 18px;
  }
  .page-header,
  .collection-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }
  .achievement-grid {
    grid-template-columns: 1fr;
  }
  .achievement-filters {
    width: 100%;
    grid-template-columns: repeat(3, 1fr);
  }
  .achievement-toast {
    right: 18px;
    left: 18px;
    top: auto;
    bottom: 18px;
  }
}
</style>
