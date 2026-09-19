<script setup lang="ts">
import { computed } from 'vue';
import type { Raid } from '@/api/demo.types';
import { formatNumber } from '@/fixtures/dashboard.fixture';
import { raidPercent } from './raid.types';

const open = defineModel<boolean>({ required: true });
const props = defineProps<{ raid: Raid; mutating?: boolean }>();
defineEmits<{ edit: []; activate: []; cancel: []; complete: []; delete: [] }>();
const defeated = computed(() => props.raid.status === 'COMPLETED');
const percent = computed(() => raidPercent(props.raid));
</script>

<template>
  <q-dialog
    v-model="open"
    position="right"
    full-height
    class="quest-dialog"
    :transition-duration="0"
    aria-labelledby="raid-details-title"
  >
    <q-card flat class="raid-drawer">
      <header class="drawer-header spread">
        <div>
          <div class="eyebrow blue">Raid details</div>
          <h2 id="raid-details-title">{{ raid.name }}</h2>
        </div>
        <q-btn
          flat
          square
          icon="close"
          class="close-icon"
          aria-label="Close Raid details"
          @click="open = false"
        />
      </header>
      <div class="drawer-body">
        <div class="spread">
          <span class="eyebrow muted">Status</span>
          <span class="raid-badge" :class="{ defeated }">● {{ raid.status }}</span>
        </div>
        <section>
          <div class="spread">
            <h3 class="eyebrow muted">Boss HP</h3>
            <strong :class="defeated ? 'green' : 'red'">{{ percent }}%</strong>
          </div>
          <div class="drawer-hp">
            {{ formatNumber(raid.currentHp) }} / {{ formatNumber(raid.maxHp) }}
          </div>
          <q-linear-progress
            class="progress-track hp-progress"
            :class="{ 'defeated-progress': defeated }"
            :value="raid.maxHp > 0 ? raid.currentHp / raid.maxHp : 0"
            aria-label="Boss HP"
            size="13px"
          />
        </section>
        <section>
          <h3 class="eyebrow muted">Description</h3>
          <p>{{ raid.description }}</p>
        </section>
        <section v-if="raid.externalReference">
          <h3 class="eyebrow muted">External reference</h3>
          <p>{{ raid.externalReference }}</p>
        </section>
        <section>
          <h3 class="eyebrow muted">How Raids work</h3>
          <p v-if="raid.status === 'ACTIVE'">Complete Quests to damage the Raid Boss.</p>
          <p v-else-if="raid.status === 'DRAFT'">Activate this Raid when the team is ready.</p>
          <p v-else>This Raid is preserved in team history.</p>
        </section>
      </div>
      <footer class="drawer-footer">
        <q-btn flat no-caps label="Edit" :disable="mutating" @click="$emit('edit')" />
        <q-btn
          v-if="raid.status === 'DRAFT'"
          unelevated
          no-caps
          color="primary"
          label="Activate"
          :loading="mutating"
          @click="$emit('activate')"
        />
        <q-btn
          v-if="raid.status === 'ACTIVE'"
          outline
          no-caps
          color="positive"
          label="Complete"
          :disable="mutating"
          @click="$emit('complete')"
        />
        <q-btn
          v-if="raid.status === 'ACTIVE'"
          outline
          no-caps
          color="negative"
          label="Cancel Raid"
          :disable="mutating"
          @click="$emit('cancel')"
        />
        <q-btn
          v-if="raid.status === 'DRAFT'"
          flat
          no-caps
          color="negative"
          label="Delete draft"
          :disable="mutating"
          @click="$emit('delete')"
        />
        <q-btn flat no-caps label="Close" :disable="mutating" @click="open = false" />
      </footer>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.raid-drawer {
  display: flex;
  flex-direction: column;
  width: 420px;
  max-width: 100vw;
  height: 100%;
  max-height: 100% !important;
  border-radius: 0;
  color: var(--ink);
  background: var(--card);
}
.drawer-header {
  align-items: flex-start;
  gap: 16px;
  padding: 22px 24px;
  border-bottom: 1px solid var(--border);
}
h2 {
  margin: 8px 0 0;
  font-size: 21px;
  font-weight: 750;
}
.close-icon {
  width: 40px;
  min-width: 40px;
  min-height: 40px;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: 5px;
  color: var(--muted);
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 25px 24px;
}
.raid-badge {
  padding: 5px 11px;
  border-radius: 18px;
  color: var(--red);
  background: #fae8e7;
  font-size: 11px;
  letter-spacing: 1px;
}
.raid-badge.defeated {
  color: var(--green);
  background: #dcefe8;
}
section {
  margin-top: 24px;
}
section h3 {
  margin: 0;
}
.drawer-hp {
  margin-top: 12px;
  font-size: 19px;
  font-weight: 750;
}
.hp-progress {
  margin-top: 15px;
}
.defeated-progress {
  color: var(--green);
  border-color: #abd8c7;
}
section p {
  margin: 12px 0 0;
  color: #43596b;
  line-height: 1.65;
}
.drawer-footer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 20px 24px;
  border-top: 1px solid var(--border);
}
.drawer-footer .q-btn {
  height: 46px;
  border: 1px solid var(--border);
  color: var(--muted);
  font-weight: 600;
}
</style>
