<script setup lang="ts">
import { nextTick, ref } from 'vue';
import { mdiGithub, mdiPulse } from '@quasar/extras/mdi-v7';
import { formatNumber } from '@/fixtures/dashboard.fixture';
import { useDashboardDemo } from '@/composables/useDashboardDemo';
import PlayerCard from '@/components/dashboard/PlayerCard.vue';
import RaidBossCard from '@/components/dashboard/RaidBossCard.vue';
import ActiveQuestCard from '@/components/dashboard/ActiveQuestCard.vue';
import QuestDetailsDrawer from '@/components/dashboard/QuestDetailsDrawer.vue';
const questOpen = ref(false);
const { state, view, quest: activeQuest, reaction, toast, completeQuest } = useDashboardDemo();
const questView = activeQuest.value ? view.quests[activeQuest.value.id] : undefined;

async function handleCompleteQuest() {
  questOpen.value = false;
  await nextTick();
  completeQuest();
}
</script>

<template>
  <q-page class="dashboard-page">
    <header class="dashboard-header spread">
      <div>
        <div class="sprint-label blue">{{ view.sprintLabel }}</div>
        <h2>Your adventure</h2>
      </div>
      <div class="header-meta">
        <span class="github-sync muted"
          ><q-icon name="sensors" size="17px" class="green" />{{ view.github.syncLabel }}</span
        ><span class="header-time"
          ><span class="muted">{{ view.dateLabel }}</span
          >{{ view.timeLabel }}</span
        >
      </div>
    </header>
    <div class="dashboard-top">
      <PlayerCard
        :player="state.player"
        :next-unlock="state.nextUnlock"
        :presentation="view.player"
        :reaction="reaction"
      /><RaidBossCard :raid="state.raid" :presentation="view.raid" />
    </div>
    <div class="dashboard-bottom">
      <ActiveQuestCard
        v-if="activeQuest && questView"
        :quest="activeQuest"
        :presentation="questView"
        @open="questOpen = true"
      />
      <q-card flat bordered class="dashboard-card small-card"
        ><div class="spread small-card-top">
          <q-icon :name="mdiGithub" size="24px" /><span class="status-dot green-dot" />
        </div>
        <div class="eyebrow muted">GitHub</div>
        <h3>{{ view.github.status }}</h3>
        <p class="muted">{{ view.github.commitsToday }} commits today</p></q-card
      >
      <q-card flat bordered class="dashboard-card small-card"
        ><div class="spread small-card-top">
          <q-icon :name="mdiPulse" size="24px" class="blue" /><span class="muted">{{
            view.activity.timeLabel
          }}</span>
        </div>
        <div class="eyebrow muted">Recent activity</div>
        <h3>{{ view.activity.title }}</h3>
        <p v-if="view.activity.xpGained" class="activity-xp orange">
          +{{ formatNumber(view.activity.xpGained) }} XP
        </p></q-card
      >
    </div>
    <QuestDetailsDrawer
      v-if="activeQuest && questView"
      v-model="questOpen"
      :quest="activeQuest"
      :presentation="questView"
      @complete="handleCompleteQuest"
    />
    <div v-if="toast" class="completion-toast" role="status" aria-live="polite">
      <q-icon name="check_circle" size="23px" class="green" />
      <strong>{{ toast }}</strong>
    </div>
  </q-page>
</template>

<style scoped>
.dashboard-page {
  padding: 28px 36px 40px;
}
.dashboard-header {
  min-height: 54px;
  margin-bottom: 25px;
}
.sprint-label {
  font-size: 13px;
  margin-bottom: 5px;
}
.dashboard-header h2 {
  font-size: 28px;
  line-height: 1.2;
  font-weight: 750;
  letter-spacing: -0.5px;
  margin: 0;
}
.header-meta {
  display: flex;
  align-items: center;
  font-size: 13px;
  gap: 27px;
}
.github-sync {
  display: flex;
  gap: 9px;
  align-items: center;
}
.header-time {
  display: flex;
  gap: 18px;
  padding: 10px 0 10px 27px;
  border-left: 1px solid var(--border);
  font-family: monospace;
}
.dashboard-top {
  display: grid;
  grid-template-columns: minmax(0, 1.75fr) minmax(0, 1fr);
  gap: 22px;
}
.dashboard-bottom {
  display: grid;
  grid-template-columns: minmax(0, 2.8fr) repeat(2, minmax(0, 1fr));
  gap: 22px;
  margin-top: 22px;
}
.small-card {
  padding: 24px 22px;
  min-height: 224px;
}
.small-card-top {
  margin-bottom: 31px;
  font-size: 13px;
}
.small-card h3 {
  margin-top: 11px;
}
.small-card p {
  margin: 15px 0 0;
  font-size: 13px;
}
.small-card .activity-xp {
  font-size: 15px;
  font-weight: 600;
}
.completion-toast {
  position: fixed;
  z-index: 7000;
  left: 50%;
  bottom: 28px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border: 1px solid #8dceb2;
  border-radius: 7px;
  background: #f4fbff;
  box-shadow: 0 8px 24px #28495c35;
  transform: translateX(-50%);
}
@media (max-width: 1200px) {
  .dashboard-page {
    padding: 24px;
  }
  .dashboard-bottom {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .active-quest {
    grid-column: 1 / -1;
  }
  .header-meta {
    gap: 12px;
  }
}
@media (max-width: 950px) {
  .dashboard-top {
    grid-template-columns: 1fr;
  }
  .header-time {
    display: none;
  }
}
@media (max-width: 700px) {
  .dashboard-page {
    padding: 18px;
  }
  .dashboard-header {
    gap: 16px;
    flex-wrap: wrap;
  }
  .dashboard-bottom {
    gap: 14px;
  }
  .small-card {
    padding: 20px 16px;
  }
}
</style>
