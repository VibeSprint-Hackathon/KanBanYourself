<script setup lang="ts">
import {
  mdiBookOpenPageVariantOutline,
  mdiCogOutline,
  mdiSwordCross,
  mdiTrophyOutline,
  mdiViewDashboardOutline,
} from '@quasar/extras/mdi-v7';
import { demoState, dashboardPresentation as view } from '@/fixtures/dashboard.fixture';
const navigation = [
  { label: 'Dashboard', icon: mdiViewDashboardOutline, active: true },
  { label: 'Quests', icon: mdiBookOpenPageVariantOutline, active: false },
  { label: 'Raids', icon: mdiSwordCross, active: false },
  { label: 'Achievements', icon: mdiTrophyOutline, active: false },
];
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
          <span class="brand-mark"
            ><q-icon name="hexagon" size="46px" /><span>&lt;/&gt;</span></span
          >
          <div><strong>VibeSprint</strong><span class="brand-subtitle">Developer RPG</span></div>
        </div>
        <nav aria-label="Main navigation">
          <q-list
            ><q-item
              v-for="item in navigation"
              :key="item.label"
              :to="item.active ? '/' : undefined"
              :active="item.active"
              :aria-disabled="!item.active || undefined"
              :aria-current="item.active ? 'page' : undefined"
              class="nav-item"
              ><q-item-section avatar><q-icon :name="item.icon" size="21px" /></q-item-section
              ><q-item-section>{{ item.label }}</q-item-section></q-item
            ></q-list
          >
        </nav>
        <div class="sidebar-player">
          <q-avatar rounded size="40px">{{ view.player.initials }}</q-avatar>
          <div>
            <strong>{{ demoState.player.name }}</strong
            ><span>{{ view.player.sidebarStatus }}</span>
          </div>
          <q-icon :name="mdiCogOutline" size="19px" class="muted" />
        </div>
      </aside>
    </q-drawer>
    <q-page-container><router-view /></q-page-container>
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
