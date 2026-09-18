<script setup lang="ts">
import { mdiSwordCross } from '@quasar/extras/mdi-v7';
import type { Raid } from '@/api/demo.types';
import { formatNumber } from '@/fixtures/dashboard.fixture';
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
      <div class="boss-placeholder" role="img" :aria-label="`${raid.name} placeholder`">
        <i /><i />
      </div>
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
.raid-card.defeated .boss-placeholder {
  filter: grayscale(0.6);
  opacity: 0.72;
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
.boss-placeholder {
  position: relative;
  width: 208px;
  height: 153px;
  border-radius: 45% 45% 35% 35%;
  background: linear-gradient(#b65848, #7f3933);
  box-shadow: 0 24px 24px #813f3b30;
}
.boss-placeholder::before,
.boss-placeholder::after {
  content: '';
  position: absolute;
  top: -29px;
  width: 60px;
  height: 85px;
  background: #894a3b;
}
.boss-placeholder::before {
  left: 10px;
  border-radius: 80% 15% 65% 45%;
  transform: rotate(-23deg);
}
.boss-placeholder::after {
  right: 10px;
  border-radius: 15% 80% 45% 65%;
  transform: rotate(23deg);
}
.boss-placeholder i {
  position: absolute;
  width: 13px;
  height: 8px;
  background: #eb6554;
  border-radius: 10px;
  top: 57px;
  left: 66px;
}
.boss-placeholder i + i {
  left: auto;
  right: 66px;
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
