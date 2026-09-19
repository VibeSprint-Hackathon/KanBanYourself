<script setup lang="ts">
import { mdiSwordCross } from '@quasar/extras/mdi-v7';
import type { Raid } from '@/api/demo.types';
import { formatNumber } from '@/fixtures/dashboard.fixture';
import BossVisual from '@/components/raids/BossVisual.vue';
defineProps<{ raid: Raid; presentation: { playerDamage: number; xpReward: number } }>();
</script>

<template>
  <q-card
    flat
    bordered
    class="dashboard-card raid-card"
    :class="{ defeated: raid.status === 'DEFEATED' }"
  >
    <div class="raid-header spread">
      <div>
        <div class="eyebrow" :class="raid.status === 'DEFEATED' ? 'green' : 'red'">
          Raid boss · {{ raid.status }}
        </div>
        <h2>{{ raid.name }}</h2>
      </div>
      <q-icon :name="mdiSwordCross" size="28px" class="red" />
    </div>
    <div class="boss-stage">
      <BossVisual :name="raid.name" :defeated="raid.status === 'DEFEATED'" />
    </div>
    <div class="raid-stats">
      <div class="spread">
        <span>Boss HP</span
        ><span class="red"
          >{{ formatNumber(raid.currentHp) }} / {{ formatNumber(raid.maxHp) }}</span
        >
      </div>
      <q-linear-progress
        class="progress-track hp-progress"
        :value="raid.maxHp > 0 ? raid.currentHp / raid.maxHp : 0"
        aria-label="Boss HP"
        size="20px"
      />
      <div class="spread raid-footer">
        <span class="muted"
          >Your damage: <strong>{{ formatNumber(presentation.playerDamage) }}</strong></span
        ><span class="orange">Reward: {{ formatNumber(presentation.xpReward) }} XP</span>
      </div>
    </div>
  </q-card>
</template>

<style scoped>
.raid-card {
  height: 486px;
  border-color: #ffaaa3;
  display: flex;
  flex-direction: column;
}
.raid-card.defeated {
  border-color: #8dceb2;
}
.raid-header {
  padding: 23px 27px;
  border-bottom: 1px solid var(--border);
}
h2 {
  margin: 10px 0 0;
  font-size: 22px;
  font-weight: 750;
  line-height: 1.25;
}
.boss-stage {
  display: grid;
  place-items: center;
  flex: 1;
  min-height: 220px;
  padding-bottom: 10px;
}
.raid-stats {
  padding: 0 27px 28px;
  font-size: 13px;
}
.hp-progress {
  margin: 15px 0 16px;
}
.raid-footer {
  gap: 12px;
  flex-wrap: wrap;
}
.raid-footer strong {
  color: var(--ink);
}
</style>
