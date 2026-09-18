<script setup lang="ts">
import type { Quest } from '@/api/demo.types';
import {
  formatNumber,
  questStatusLabels,
  type QuestPresentation,
} from '@/fixtures/dashboard.fixture';
defineProps<{ quest: Quest; presentation: QuestPresentation }>();
defineEmits<{ open: [] }>();
</script>

<template>
  <button
    class="dashboard-card active-quest"
    :class="{ completed: quest.status === 'DONE' }"
    type="button"
    aria-haspopup="dialog"
    :aria-label="`Open quest: ${quest.title}`"
    @click="$emit('open')"
  >
    <div class="quest-main">
      <div class="spread">
        <span class="eyebrow" :class="quest.status === 'DONE' ? 'green' : 'blue'"
          ><q-icon :name="quest.status === 'DONE' ? 'check' : 'bolt'" size="18px" />
          {{ quest.status === 'DONE' ? 'Quest completed' : 'Active quest' }}</span
        ><span class="quest-badge" :class="{ completed: quest.status === 'DONE' }"
          >● {{ questStatusLabels[quest.status] }}</span
        >
      </div>
      <h2>{{ quest.title }} <q-icon name="chevron_right" size="20px" class="muted" /></h2>
      <p :class="quest.status === 'DONE' ? 'green' : 'muted'">
        {{ quest.status === 'DONE' ? 'Quest completed — well done!' : presentation.description }}
      </p>
      <div class="quest-progress">
        <q-linear-progress
          class="progress-track"
          :class="{ 'completed-progress': quest.status === 'DONE' }"
          :value="presentation.progressPercent / 100"
          aria-label="Quest progress"
          size="13px"
        /><strong :class="quest.status === 'DONE' ? 'green' : 'blue'"
          >{{ presentation.progressPercent }}%</strong
        >
      </div>
    </div>
    <div class="quest-reward">
      <span class="eyebrow muted">{{ quest.status === 'DONE' ? 'XP earned' : 'Quest reward' }}</span
      ><strong class="orange">+{{ formatNumber(quest.xpReward) }} XP</strong
      ><q-icon
        :name="quest.status === 'DONE' ? 'check' : 'chevron_right'"
        size="19px"
        :class="quest.status === 'DONE' ? 'green' : 'muted'"
      />
    </div>
  </button>
</template>

<style scoped>
.active-quest {
  width: 100%;
  min-height: 224px;
  padding: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 23%;
  text-align: left;
  color: inherit;
  font: inherit;
  cursor: pointer;
  border-color: #97d9f2;
}
.active-quest:hover {
  border-color: var(--blue);
}
.active-quest.completed {
  border-color: #8dceb2;
}
.completed-progress {
  color: var(--green);
}
.quest-main {
  padding: 26px;
  min-width: 0;
}
h2 {
  font-size: 26px;
  line-height: 1.25;
  font-weight: 750;
  margin: 16px 0 12px;
  letter-spacing: -0.5px;
}
p {
  font-size: 15px;
  margin: 0;
  line-height: 1.6;
}
.quest-progress {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 27px;
}
.quest-progress .q-linear-progress {
  flex: 1;
}
.quest-reward {
  padding: 24px 22px;
  background: #dcecf65c;
  border-left: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.quest-reward strong {
  font-size: 27px;
  white-space: nowrap;
  line-height: 1.2;
}
.quest-reward .q-icon {
  align-self: flex-end;
  margin-top: auto;
}
@media (max-width: 1550px) {
  .active-quest {
    grid-template-columns: minmax(0, 1fr) 180px;
  }
  .quest-main {
    padding: 24px;
  }
}
@media (max-width: 700px) {
  .active-quest {
    grid-template-columns: 1fr;
  }
  .quest-reward {
    border-left: 0;
    border-top: 1px solid var(--border);
    flex-direction: row;
    align-items: center;
  }
  .quest-reward .q-icon {
    margin-left: auto;
  }
  .quest-main > .spread {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
