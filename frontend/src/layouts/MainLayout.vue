<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute } from 'vue-router';
import {
  mdiBookOpenPageVariantOutline,
  mdiCogOutline,
  mdiSwordCross,
  mdiTrophyOutline,
  mdiViewDashboardOutline,
} from '@quasar/extras/mdi-v7';
import { dashboardPresentation as view } from '@/fixtures/dashboard.fixture';
import type { CharacterReaction, CharacterState } from '@/api/demo.types';
import CharacterAvatar from '@/components/dashboard/CharacterAvatar.vue';
import PictureInPictureCompanion from '@/components/companion/PictureInPictureCompanion.vue';
import RaidDamageNotification from '@/components/raids/RaidDamageNotification.vue';
import AchievementUnlockNotification from '@/components/achievements/AchievementUnlockNotification.vue';
import { useDemoStore } from '@/stores/demo';

const route = useRoute();
const demoStore = useDemoStore();
const { state, selectedPlayer, lastProgression } = storeToRefs(demoStore);
const reaction = ref<CharacterReaction | null>(null);
const characterState = computed<CharacterState>(
  () => selectedPlayer.value?.characterState ?? 'idle',
);
const playerName = computed(() => selectedPlayer.value?.name ?? 'Player');
const playerLevel = computed(() => selectedPlayer.value?.level ?? 0);
const playerCosmeticKey = computed(() => selectedPlayer.value?.cosmeticKey ?? 'base');
const playerInitials = computed(() => {
  const initials = playerName.value
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
  return initials || view.player.initials;
});
const navigation = [
  { label: 'Dashboard', icon: mdiViewDashboardOutline, to: '/' },
  { label: 'Quests', icon: mdiBookOpenPageVariantOutline, to: '/quests' },
  { label: 'Raids', icon: mdiSwordCross, to: '/raids' },
  { label: 'Achievements', icon: mdiTrophyOutline, to: '/achievements' },
];

let reactionTimer: ReturnType<typeof setTimeout> | undefined;
let stopRealtime: (() => void) | undefined;

watch(selectedPlayer, (player, previousPlayer) => {
  if (previousPlayer && player?.id !== previousPlayer.id) {
    clearReactionTimer();
    reaction.value = null;
  }
});

watch(lastProgression, (progression) => {
  if (!progression?.applied || progression.player.id !== selectedPlayer.value?.id) {
    return;
  }
  reaction.value = progression.reaction ?? 'happy';
  clearReactionTimer();
  reactionTimer = setTimeout(() => {
    reaction.value = null;
    reactionTimer = undefined;
  }, 1_500);
});

onMounted(() => {
  stopRealtime = demoStore.startRealtime();
  if (state.value === null) {
    void demoStore.loadState();
  }
});

onUnmounted(() => {
  stopRealtime?.();
  clearReactionTimer();
});

function clearReactionTimer(): void {
  if (reactionTimer) {
    clearTimeout(reactionTimer);
    reactionTimer = undefined;
  }
}
</script>

<template>
  <q-layout view="lHh Lpr lFf" class="dashboard-layout">
    <q-drawer
      :model-value="$q.screen.width > 700"
      show-if-above
      :breakpoint="700"
      :width="248"
      bordered
      class="dashboard-sidebar"
    >
      <aside class="sidebar-content">
        <div class="brand">
          <div><strong>KanBanYourself</strong><span class="brand-subtitle">Developer RPG</span></div>
        </div>
        <nav aria-label="Main navigation">
          <q-list
            ><q-item
              v-for="item in navigation"
              :key="item.label"
              :to="item.to"
              exact
              :active="item.to === route.path"
              :aria-disabled="!item.to || undefined"
              :aria-current="item.to === route.path ? 'page' : undefined"
              class="nav-item"
              ><q-item-section avatar><q-icon :name="item.icon" size="21px" /></q-item-section
              ><q-item-section>{{ item.label }}</q-item-section></q-item
            ></q-list
          >
        </nav>
        <div v-if="route.path !== '/'" class="sidebar-companion">
          <CharacterAvatar
            :name="playerName"
            :persistent-state="characterState"
            :reaction="reaction"
            :cosmetic-key="playerCosmeticKey"
            :level="playerLevel"
          />
        </div>
        <div class="sidebar-player" role="button" tabindex="0" aria-label="Switch profile">
          <q-avatar rounded size="40px">{{ playerInitials }}</q-avatar>
          <div>
            <strong>{{ playerName }}</strong
            ><span>{{ view.player.sidebarStatus }}</span>
          </div>
          <PictureInPictureCompanion
            :name="playerName"
            :persistent-state="characterState"
            :reaction="reaction"
            :cosmetic-key="playerCosmeticKey"
            :level="playerLevel"
          />
          <q-icon :name="mdiCogOutline" size="19px" class="muted" />
          <q-menu anchor="top right" self="bottom right">
            <q-list style="min-width: 210px">
              <q-item-label header>Demo profile</q-item-label>
              <q-item
                v-for="player in state?.players ?? []"
                :key="player.id"
                v-close-popup
                clickable
                :active="player.id === selectedPlayer?.id"
                @click="demoStore.selectPlayer(player.id)"
              >
                <q-item-section avatar
                  ><q-avatar size="32px">{{ player.name[0] }}</q-avatar></q-item-section
                >
                <q-item-section>
                  <q-item-label>{{ player.name }}</q-item-label>
                  <q-item-label caption>@{{ player.githubLogin }}</q-item-label>
                </q-item-section>
                <q-item-section v-if="player.id === selectedPlayer?.id" side>
                  <q-icon name="check" color="primary" />
                </q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </div>
      </aside>
    </q-drawer>
    <q-page-container><router-view /></q-page-container>
    <RaidDamageNotification />
    <AchievementUnlockNotification />
  </q-layout>
</template>

<style scoped>
.sidebar-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 28px 24px 24px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
}
.brand strong {
  display: block;
  font-size: 23px;
  font-weight: 750;
  line-height: 1.15;
}
.brand-mark {
  position: relative;
  color: var(--blue);
}
.brand-mark > span {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--card);
  font: bold 18px monospace;
}
.brand-subtitle {
  display: block;
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 1.7px;
  text-transform: uppercase;
  margin-top: 3px;
}
nav {
  margin-top: 55px;
}
.sidebar-companion {
  display: grid;
  flex: 1;
  min-height: 0;
  place-items: center;
  overflow: hidden;
  margin-inline: -18px;
  padding: 14px 0 8px;
}
.sidebar-companion :deep(.character-avatar) {
  width: min(224px, 100%);
  height: clamp(185px, 29vh, 224px);
}
.nav-item {
  height: 50px;
  min-height: 50px;
  margin-bottom: 8px;
  border-radius: 5px;
  color: var(--muted);
  font-size: 15px;
  font-weight: 500;
  padding: 0 12px;
}
.nav-item .q-item__section--avatar {
  min-width: 32px;
  padding-right: 10px;
}
.nav-item.q-router-link--active {
  color: var(--ink);
  background: #c1e5f5;
}
.nav-item.q-router-link--active::before {
  content: '';
  position: absolute;
  left: 0;
  width: 4px;
  top: 11px;
  bottom: 11px;
  background: var(--blue);
  border-radius: 4px;
}
.nav-item.q-router-link--active .q-icon {
  color: var(--blue);
}
.sidebar-player {
  display: flex;
  gap: 13px;
  align-items: center;
  border-top: 1px solid var(--border);
  padding-top: 23px;
  margin-top: auto;
  cursor: pointer;
}
.sidebar-player .q-avatar {
  color: var(--blue);
  background: #cfe8f4;
  font-size: 13px;
  font-weight: 700;
}
.sidebar-player > div:nth-child(2) {
  flex: 1;
  min-width: 0;
}
.sidebar-player strong {
  font-size: 15px;
  font-weight: 500;
}
.sidebar-player span {
  display: block;
  font-size: 13px;
  color: var(--muted);
}
</style>
