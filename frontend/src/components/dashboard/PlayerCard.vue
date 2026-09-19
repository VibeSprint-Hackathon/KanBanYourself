<script setup lang="ts">
import type { CharacterReaction, CharacterState, Player } from '@/api/demo.types';
import { formatNumber } from '@/fixtures/dashboard.fixture';
import CharacterAvatar from './CharacterAvatar.vue';
defineProps<{
  player: Player;
  presentation: {
    subtitle: string;
    status: string;
    characterSrc: string | null;
    xpProgress: number;
    xpToReward: number;
  };
  characterState: CharacterState;
  reaction: CharacterReaction | null;
}>();
</script>

<template>
  <q-card flat bordered class="dashboard-card player-card">
    <div class="player-identity">
      <div class="player-status"><span class="status-dot" />{{ presentation.status }}</div>
      <div class="identity-bottom">
        <div class="muted player-subtitle">{{ presentation.subtitle }}</div>
        <h1>{{ player.name }}</h1>
        <div class="level-row">
          <strong class="level-number">{{ player.level }}</strong>
          <div>
            <h3>Level</h3>
            <div class="muted">{{ player.title }}</div>
          </div>
        </div>
      </div>
    </div>
    <CharacterAvatar
      class="player-character"
      :name="player.name"
      :persistent-state="characterState"
      :reaction="reaction"
      :cosmetic-key="player.cosmeticKey"
      :level="player.level"
    />
    <div class="player-progress">
      <div class="spread progress-heading">
        <h3>Level progress</h3>
        <strong v-if="player.nextLevelXp !== null" class="orange">
          {{ formatNumber(player.totalXp) }} / {{ formatNumber(player.nextLevelXp) }} XP
        </strong>
        <strong v-else class="orange">{{ formatNumber(player.totalXp) }} XP · MAX</strong>
      </div>
      <q-linear-progress
        class="progress-track xp-progress"
        :value="presentation.xpProgress"
        aria-label="Level progress"
        size="14px"
      />
      <div v-if="player.nextUnlock" class="next-reward">
        <div class="eyebrow orange">
          <q-icon name="auto_awesome" size="17px" />
          {{
            player.cosmeticKey === player.nextUnlock.cosmeticKey ? 'Reward unlocked' : 'Next reward'
          }}
        </div>
        <div class="spread reward-detail">
          <h3>{{ player.nextUnlock.displayName }}</h3>
          <strong v-if="player.cosmeticKey !== player.nextUnlock.cosmeticKey" class="orange"
            >{{ formatNumber(presentation.xpToReward) }} XP</strong
          >
          <q-icon v-else name="check_circle" size="20px" class="green" />
        </div>
      </div>
      <div v-else class="next-reward">
        <div class="eyebrow orange">
          <q-icon name="verified" size="17px" />
          All rewards unlocked
        </div>
      </div>
    </div>
  </q-card>
</template>

<style scoped>
.player-card {
  height: 486px;
  padding: 32px;
  display: grid;
  grid-template-columns: 30% 24% 46%;
  background: radial-gradient(ellipse at 56% 60%, #d8f0fc, transparent 64%), var(--card);
}
.player-identity {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  z-index: 1;
}
.player-status {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--blue);
  white-space: nowrap;
}
.player-subtitle {
  margin-bottom: 5px;
}
h1 {
  font-size: 46px;
  line-height: 1.04;
  font-weight: 750;
  letter-spacing: -1.4px;
  margin: 0 0 24px;
}
.level-row {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}
.level-number {
  font-size: 54px;
  line-height: 1;
  color: #009ec6;
}
.level-row h3 {
  margin-bottom: 5px;
}
.player-character {
  min-height: 0;
  transform: translateX(-34px);
}
.player-progress {
  padding-left: 4px;
}
.progress-heading {
  margin: 3px 0 15px;
  gap: 10px;
  flex-wrap: wrap;
}
.progress-heading strong {
  font-size: 13px;
}
.next-reward {
  margin-top: 30px;
  padding: 18px;
  border: 1px solid #efca8c;
  background: #e9970610;
  border-radius: 5px;
}
.reward-detail {
  margin-top: 12px;
  gap: 10px;
  flex-wrap: wrap;
}
.reward-detail strong {
  font-size: 13px;
}
@media (max-width: 1500px) {
  .player-card {
    grid-template-columns: 30% 23% 47%;
  }
  .player-character {
    transform: translateX(-46px);
  }
  h1 {
    font-size: 44px;
  }
  .reward-detail h3 {
    font-size: 16px;
  }
}
@media (max-width: 1100px) {
  .player-card {
    padding: 24px;
    grid-template-columns: 1fr 1fr;
    height: auto;
    min-height: 450px;
  }
  .player-progress {
    grid-column: 1 / -1;
    padding: 20px 0 0;
  }
  .next-reward {
    margin-top: 16px;
  }
  .player-character {
    height: 260px;
    transform: translateX(-16px);
  }
  h1 {
    margin-top: 25px;
  }
}
</style>
