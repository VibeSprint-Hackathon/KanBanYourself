<script setup lang="ts">
import { computed } from 'vue';
import { mdiCheck, mdiFlashOutline, mdiSwordCross } from '@quasar/extras/mdi-v7';
import type { Raid } from '@/api/demo.types';
import { formatNumber } from '@/fixtures/dashboard.fixture';
import BossVisual from './BossVisual.vue';
import { raidPercent, type RaidDamageFeedback } from './raid.types';

const props = defineProps<{ raid: Raid; damage?: RaidDamageFeedback | null }>();
defineEmits<{ open: [] }>();
const defeated = computed(() => props.raid.status === 'DEFEATED');
const percent = computed(() => raidPercent(props.raid));
</script>

<template>
  <q-card
    flat
    bordered
    class="active-raid-card"
    :class="{ defeated }"
    role="button"
    tabindex="0"
    aria-label="Open Raid details"
    @click="$emit('open')"
    @keydown.enter.prevent="$emit('open')"
    @keydown.space.prevent="$emit('open')"
  >
    <div class="raid-visual-panel">
      <header class="raid-card-header spread">
        <div>
          <div class="eyebrow" :class="defeated ? 'green' : 'red'">
            {{ defeated ? 'Boss defeated' : 'Active Raid' }}
          </div>
          <h2>{{ raid.name }}</h2>
        </div>
        <q-icon :name="mdiSwordCross" size="24px" :class="defeated ? 'green' : 'red'" />
      </header>
      <div class="boss-stage">
        <div v-if="damage" class="damage-feedback" role="status">
          <q-icon :name="mdiFlashOutline" size="20px" /> Boss -{{
            formatNumber(damage.damageReceived)
          }}
          HP
        </div>
        <div v-else-if="defeated" class="damage-feedback victory">
          <q-icon :name="mdiCheck" size="20px" /> Raid complete
        </div>
        <BossVisual :name="raid.name" :defeated="defeated" />
      </div>
    </div>
    <div class="raid-data-panel">
      <div>
        <div class="eyebrow muted">Boss HP</div>
        <div class="hp-value" :class="defeated ? 'green' : 'red'">
          {{ formatNumber(raid.currentHp) }}
          <span>/ {{ formatNumber(raid.maxHp) }}</span>
        </div>
        <strong class="remaining">{{ percent }}% remaining</strong>
        <q-linear-progress
          class="progress-track hp-progress"
          :class="{ 'defeated-progress': defeated }"
          :value="raid.maxHp > 0 ? raid.currentHp / raid.maxHp : 0"
          aria-label="Boss HP"
          size="23px"
        />
        <div v-if="damage" class="damage-comparison spread">
          <span>Previous HP: {{ formatNumber(damage.previousHp) }}</span>
          <strong>-{{ formatNumber(damage.damageReceived) }} HP</strong>
        </div>
      </div>
      <p :class="defeated ? 'green defeated-copy' : 'muted'">
        {{
          defeated ? `${raid.name} has been defeated.` : 'Complete Quests to damage the Raid Boss.'
        }}
      </p>
    </div>
  </q-card>
</template>

<style scoped>
.active-raid-card {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(365px, 1fr);
  min-height: 432px;
  overflow: hidden;
  border-color: #f19a93;
  cursor: pointer;
}
.active-raid-card.defeated {
  border-color: #78c5a3;
}
.active-raid-card:focus-visible {
  outline: 3px solid var(--blue);
  outline-offset: 3px;
}
.raid-visual-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  border-right: 1px solid var(--border);
}
.raid-card-header {
  min-height: 100px;
  padding: 21px 28px;
  border-bottom: 1px solid var(--border);
}
h2 {
  margin: 8px 0 0;
  font-size: 29px;
  line-height: 1.15;
  font-weight: 780;
  letter-spacing: -0.6px;
}
.boss-stage {
  position: relative;
  display: grid;
  flex: 1;
  min-height: 300px;
  place-items: center;
}
.boss-stage :deep(.boss-placeholder) {
  transform: scale(1.12);
}
.damage-feedback {
  position: absolute;
  z-index: 1;
  top: 21px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 12px;
  border: 1px solid #f2a09a;
  border-radius: 5px;
  color: var(--red);
  background: var(--card);
  box-shadow: 0 3px 8px #385b7420;
  font-size: 14px;
  font-weight: 700;
}
.damage-feedback.victory {
  color: var(--green);
  border-color: #8dceb2;
}
.raid-data-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 29px 28px 31px;
}
.hp-value {
  margin-top: 10px;
  font-size: 31px;
  font-weight: 780;
  line-height: 1.05;
}
.hp-value span {
  color: var(--muted);
  font-size: 18px;
}
.remaining {
  display: block;
  margin-top: 8px;
  color: var(--muted);
  font-size: 14px;
}
.hp-progress {
  margin-top: 21px;
}
.defeated-progress {
  color: var(--green);
  border-color: #abd8c7;
}
.damage-comparison {
  margin-top: 11px;
  color: var(--muted);
  font-size: 12px;
}
.damage-comparison strong {
  color: var(--red);
}
.raid-data-panel p {
  margin: 22px 0 0;
  font-size: 14px;
}
.defeated-copy {
  font-weight: 600;
}
@media (max-width: 1050px) {
  .active-raid-card {
    grid-template-columns: minmax(0, 1.45fr) 350px;
  }
}
</style>
