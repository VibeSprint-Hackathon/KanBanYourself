<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { mdiShieldOutline } from '@quasar/extras/mdi-v7';
import ActiveRaidCard from '@/components/raids/ActiveRaidCard.vue';
import RaidDetailsDrawer from '@/components/raids/RaidDetailsDrawer.vue';
import RaidFormDialog from '@/components/raids/RaidFormDialog.vue';
import type { RaidDamageFeedback } from '@/components/raids/raid.types';
import type { CreateRaidRequest, Raid } from '@/api/demo.types';
import { raidPagePresentation as view } from '@/fixtures/raids.fixture';
import { useDemoStore } from '@/stores/demo';

type RaidCommand = 'activate' | 'cancel' | 'complete' | 'delete';

const store = useDemoStore();
const {
  state,
  raids,
  loading,
  raidsLoading,
  creatingRaid,
  mutatingRaidId,
  error,
  realtimeStatus,
  lastProgression,
} = storeToRefs(store);
const detailsOpen = ref(false);
const formOpen = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const selectedId = ref<number | null>(null);
const confirmCommand = ref<RaidCommand | null>(null);

const activeRaid = computed(() => raids.value.find((raid) => raid.status === 'ACTIVE') ?? null);
const drafts = computed(() => raids.value.filter((raid) => raid.status === 'DRAFT'));
const history = computed(() =>
  raids.value.filter((raid) => raid.status === 'COMPLETED' || raid.status === 'CANCELLED'),
);
const selected = computed(() => raids.value.find((raid) => raid.id === selectedId.value) ?? null);
const busy = computed(() => creatingRaid.value || mutatingRaidId.value !== null);
const damage = computed<RaidDamageFeedback | null>(() => {
  const progression = lastProgression.value;
  if (!progression?.applied || !progression.raid || progression.raid.id !== activeRaid.value?.id) {
    return null;
  }
  return {
    previousHp: Math.min(
      progression.raid.maxHp,
      progression.raid.currentHp + progression.raidDamage,
    ),
    damageReceived: progression.raidDamage,
  };
});
const confirmation = computed(() => {
  if (!selected.value || !confirmCommand.value) return null;
  const copy: Record<RaidCommand, { title: string; body: string; action: string; color: string }> =
    {
      activate: {
        title: 'Activate Raid?',
        body: `Make “${selected.value.name}” the active team Raid?`,
        action: 'Activate',
        color: 'primary',
      },
      cancel: {
        title: 'Cancel Raid?',
        body: `Cancel “${selected.value.name}” and keep it in history?`,
        action: 'Cancel Raid',
        color: 'negative',
      },
      complete: {
        title: 'Complete Raid?',
        body: `Mark “${selected.value.name}” as defeated without awarding XP?`,
        action: 'Complete Raid',
        color: 'positive',
      },
      delete: {
        title: 'Delete draft?',
        body: `Delete “${selected.value.name}”? This cannot be undone.`,
        action: 'Delete draft',
        color: 'negative',
      },
    };
  return copy[confirmCommand.value];
});

let stopRealtime: (() => void) | undefined;

onMounted(() => {
  stopRealtime = store.startRealtime();
  if (state.value === null) void store.loadState();
  void store.loadRaids();
});
onUnmounted(() => stopRealtime?.());

function openRaid(raid: Raid): void {
  selectedId.value = raid.id;
  detailsOpen.value = true;
}

function openCreate(): void {
  selectedId.value = null;
  formMode.value = 'create';
  formOpen.value = true;
}

function openEdit(): void {
  if (!selected.value) return;
  detailsOpen.value = false;
  formMode.value = 'edit';
  formOpen.value = true;
}

function ask(command: RaidCommand): void {
  detailsOpen.value = false;
  confirmCommand.value = command;
}

async function saveRaid(request: CreateRaidRequest): Promise<void> {
  const saved =
    formMode.value === 'create'
      ? await store.createRaid(request)
      : selectedId.value !== null && (await store.updateRaid(selectedId.value, request));
  if (saved) formOpen.value = false;
}

async function runCommand(): Promise<void> {
  const raidId = selectedId.value;
  const command = confirmCommand.value;
  if (raidId === null || command === null) return;
  const actions: Record<RaidCommand, (id: number) => Promise<boolean>> = {
    activate: store.activateRaid,
    cancel: store.cancelRaid,
    complete: store.completeRaid,
    delete: store.deleteRaid,
  };
  if (await actions[command](raidId)) {
    confirmCommand.value = null;
    selectedId.value = null;
  }
}
</script>

<template>
  <q-page class="raids-page">
    <header class="page-header spread">
      <div>
        <div class="sprint-label blue">{{ view.sprintLabel }}</div>
        <h1>Raids</h1>
      </div>
      <div class="header-actions">
        <span class="sync muted">
          <q-icon name="sensors" :class="realtimeStatus === 'connected' ? 'green' : 'orange'" />
          {{ realtimeStatus === 'connected' ? 'Live sync' : 'Sync offline' }}
        </span>
        <q-btn unelevated no-caps color="primary" icon="add" label="New Raid" @click="openCreate" />
      </div>
    </header>

    <q-card v-if="(loading || raidsLoading) && raids.length === 0" flat bordered class="state-card">
      <q-spinner color="primary" size="34px" />
      <strong>Loading Raids…</strong>
    </q-card>
    <q-card v-else-if="raids.length === 0 && error" flat bordered class="state-card error-card">
      <q-icon name="cloud_off" size="34px" class="red" />
      <strong>Could not load Raids</strong>
      <span class="muted">{{ error }}</span>
      <q-btn unelevated no-caps label="Try again" color="primary" @click="store.loadRaids()" />
    </q-card>
    <template v-else>
      <div v-if="error" class="sync-warning" role="alert">
        <q-icon name="warning" /><span>{{ error }}</span>
      </div>

      <section>
        <div class="section-heading">
          <div>
            <span class="eyebrow red">Current challenge</span>
            <h2>Active Raid</h2>
          </div>
          <p>Quest completions damage only this Boss.</p>
        </div>
        <ActiveRaidCard
          v-if="activeRaid"
          :raid="activeRaid"
          :damage="damage"
          @open="openRaid(activeRaid)"
        />
        <q-card v-else flat bordered class="empty-card">
          <q-icon :name="mdiShieldOutline" size="28px" class="blue" />
          <h3>No active Raid</h3>
          <p class="muted">Activate one of the prepared Raids when the team is ready.</p>
        </q-card>
      </section>

      <section>
        <div class="section-heading">
          <div>
            <span class="eyebrow blue">Ready next</span>
            <h2>Draft Raids</h2>
          </div>
          <p>Prepare future bosses without affecting progression.</p>
        </div>
        <div v-if="drafts.length" class="raid-grid">
          <q-card v-for="raid in drafts" :key="raid.id" flat bordered class="raid-list-card">
            <div class="spread">
              <span class="status-badge draft">Draft</span
              ><strong>{{ raid.maxHp.toLocaleString() }} HP</strong>
            </div>
            <h3>{{ raid.name }}</h3>
            <p>{{ raid.description }}</p>
            <q-btn outline no-caps color="primary" label="View Raid" @click="openRaid(raid)" />
          </q-card>
        </div>
        <q-card v-else flat bordered class="empty-row muted">No draft Raids prepared.</q-card>
      </section>

      <section>
        <div class="section-heading">
          <div>
            <span class="eyebrow green">Archive</span>
            <h2>History</h2>
          </div>
          <p>Completed and cancelled team challenges.</p>
        </div>
        <div v-if="history.length" class="raid-grid">
          <q-card v-for="raid in history" :key="raid.id" flat bordered class="raid-list-card">
            <div class="spread">
              <span class="status-badge" :class="raid.status.toLowerCase()">{{ raid.status }}</span>
              <strong
                >{{ raid.currentHp.toLocaleString() }} /
                {{ raid.maxHp.toLocaleString() }} HP</strong
              >
            </div>
            <h3>{{ raid.name }}</h3>
            <p>{{ raid.description }}</p>
            <q-btn flat no-caps label="View details" @click="openRaid(raid)" />
          </q-card>
        </div>
        <q-card v-else flat bordered class="empty-row muted">Raid history is empty.</q-card>
      </section>

      <RaidDetailsDrawer
        v-if="selected"
        v-model="detailsOpen"
        :raid="selected"
        :mutating="mutatingRaidId === selected.id"
        @edit="openEdit"
        @activate="ask('activate')"
        @cancel="ask('cancel')"
        @complete="ask('complete')"
        @delete="ask('delete')"
      />
      <RaidFormDialog
        v-model="formOpen"
        :mode="formMode"
        :raid="selected"
        :submitting="creatingRaid || mutatingRaidId !== null"
        :error="error"
        @save="saveRaid"
      />
      <q-dialog :model-value="confirmCommand !== null" @update:model-value="confirmCommand = null">
        <q-card v-if="confirmation" class="confirm-card">
          <h2>{{ confirmation.title }}</h2>
          <p>{{ confirmation.body }}</p>
          <div class="confirm-actions">
            <q-btn flat no-caps label="Back" :disable="busy" @click="confirmCommand = null" />
            <q-btn
              unelevated
              no-caps
              :color="confirmation.color"
              :label="confirmation.action"
              :loading="busy"
              @click="runCommand"
            />
          </div>
        </q-card>
      </q-dialog>
    </template>
  </q-page>
</template>

<style scoped>
.raids-page {
  padding: 28px 36px 48px;
}
.page-header {
  align-items: center;
  margin-bottom: 26px;
}
.sprint-label {
  margin-bottom: 5px;
  font-size: 13px;
}
h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
}
.header-actions,
.sync {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-actions {
  gap: 22px;
}
section + section {
  margin-top: 34px;
}
.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
}
.section-heading h2 {
  margin: 4px 0 0;
  font-size: 20px;
}
.section-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}
.raid-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.raid-list-card {
  display: flex;
  min-height: 210px;
  flex-direction: column;
  padding: 20px;
}
.raid-list-card h3 {
  margin: 19px 0 8px;
  font-size: 18px;
}
.raid-list-card p {
  flex: 1;
  margin: 0 0 18px;
  color: var(--muted);
  line-height: 1.5;
}
.status-badge {
  padding: 5px 9px;
  border-radius: 16px;
  color: var(--green);
  background: #dcefe8;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}
.status-badge.draft {
  color: var(--blue);
  background: #e5f3fb;
}
.status-badge.cancelled {
  color: var(--muted);
  background: #e8edf0;
}
.empty-card,
.state-card {
  display: grid;
  min-height: 260px;
  place-content: center;
  justify-items: center;
  gap: 10px;
  text-align: center;
}
.empty-card h3 {
  margin: 5px 0 0;
  font-size: 20px;
}
.empty-card p {
  margin: 0;
}
.empty-row {
  padding: 22px;
  border-style: dashed;
}
.error-card {
  border-color: #ffaaa3;
}
.sync-warning {
  display: flex;
  gap: 9px;
  padding: 10px 14px;
  margin-bottom: 16px;
  color: #75442d;
  border: 1px solid #efca8c;
  border-radius: 6px;
  background: #fff0d4;
}
.confirm-card {
  width: 430px;
  max-width: calc(100vw - 32px);
  padding: 24px;
}
.confirm-card h2 {
  margin: 0;
  font-size: 21px;
}
.confirm-card p {
  color: var(--muted);
}
.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 22px;
}
@media (max-width: 1000px) {
  .raid-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 700px) {
  .raids-page {
    padding: 18px;
  }
  .page-header,
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
  .raid-grid {
    grid-template-columns: 1fr;
  }
}
</style>
