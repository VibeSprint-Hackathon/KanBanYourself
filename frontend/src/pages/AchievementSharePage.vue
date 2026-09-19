<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { mdiAlertCircleOutline, mdiLockOutline, mdiTrophyOutline } from '@quasar/extras/mdi-v7';
import { getPlayerAchievements } from '@/api/achievements';
import { getPlayers } from '@/api/demo';
import type { Player } from '@/api/demo.types';
import type { AchievementState } from '@/components/achievements/achievement.types';

type PageState = 'loading' | 'unlocked' | 'locked' | 'not-found' | 'error';

const route = useRoute();
const router = useRouter();
const state = ref<PageState>('loading');
const player = ref<Player | null>(null);
const achievement = ref<AchievementState | null>(null);
const originalTitle = document.title;

const unlockedDate = computed(() => {
  if (!achievement.value?.unlockedAt) return null;
  return new Intl.DateTimeFormat('en', { dateStyle: 'long' }).format(
    new Date(achievement.value.unlockedAt),
  );
});

async function loadShare(): Promise<void> {
  state.value = 'loading';
  const playerId = Number(route.params.playerId);
  const achievementKey = String(route.params.achievementKey ?? '');
  if (!Number.isInteger(playerId) || playerId <= 0 || !achievementKey) {
    state.value = 'not-found';
    return;
  }

  try {
    const [players, response] = await Promise.all([getPlayers(), getPlayerAchievements(playerId)]);
    player.value = players.find(({ id }) => id === playerId) ?? null;
    achievement.value = response.achievements.find(({ key }) => key === achievementKey) ?? null;

    if (!player.value || !achievement.value) {
      state.value = 'not-found';
      return;
    }
    state.value = achievement.value.unlocked ? 'unlocked' : 'locked';
    document.title = achievement.value.unlocked
      ? `${achievement.value.name} · VibeSprint Achievement`
      : 'Achievement locked · VibeSprint';
  } catch {
    state.value = 'error';
  }
}

onMounted(() => void loadShare());
onBeforeUnmount(() => {
  document.title = originalTitle;
});
</script>

<template>
  <q-layout view="lHh Lpr lFf" class="share-layout">
    <q-page-container>
      <q-page class="share-page">
        <main class="share-shell">
          <router-link to="/" class="brand" aria-label="Open VibeSprint home">
            <span class="brand-mark"
              ><q-icon name="hexagon" size="45px" /><span>&lt;/&gt;</span></span
            >
            <span><strong>VibeSprint</strong><small>Developer RPG</small></span>
          </router-link>

          <q-card v-if="state === 'loading'" flat bordered class="state-card" role="status">
            <q-spinner color="primary" size="38px" />
            <strong>Checking Achievement…</strong>
          </q-card>

          <q-card
            v-else-if="state === 'unlocked' && achievement && player"
            flat
            bordered
            class="achievement-share-card"
          >
            <div class="trophy"><q-icon :name="mdiTrophyOutline" size="38px" /></div>
            <div class="eyebrow green">Achievement unlocked</div>
            <h1>{{ achievement.name }}</h1>
            <p class="owner">
              <strong>{{ player.name }}</strong> unlocked this VibeSprint Achievement.
            </p>
            <blockquote>“{{ achievement.description }}”</blockquote>
            <p v-if="unlockedDate" class="unlock-date">Unlocked {{ unlockedDate }}</p>
            <q-btn unelevated no-caps color="primary" label="Open VibeSprint" to="/achievements" />
          </q-card>

          <q-card v-else-if="state === 'locked'" flat bordered class="state-card locked-card">
            <q-icon :name="mdiLockOutline" size="38px" />
            <h1>Achievement not unlocked</h1>
            <p>This Achievement has not been unlocked by this Player.</p>
            <q-btn outline no-caps color="primary" label="Open VibeSprint" to="/achievements" />
          </q-card>

          <q-card v-else flat bordered class="state-card error-card" role="alert">
            <q-icon :name="mdiAlertCircleOutline" size="38px" />
            <h1>
              {{ state === 'not-found' ? 'Achievement not found' : 'Could not load Achievement' }}
            </h1>
            <p>
              {{
                state === 'not-found'
                  ? 'This share link is invalid.'
                  : 'Check the backend and try again.'
              }}
            </p>
            <div class="state-actions">
              <q-btn v-if="state === 'error'" flat no-caps label="Retry" @click="loadShare" />
              <q-btn
                outline
                no-caps
                color="primary"
                label="Open VibeSprint"
                @click="router.push('/')"
              />
            </div>
          </q-card>
        </main>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<style scoped>
.share-layout,
.share-page {
  min-height: 100vh;
  color: var(--ink);
  background-color: #eef8fd;
  background-image:
    linear-gradient(#8dc3dc20 1px, transparent 1px),
    linear-gradient(90deg, #8dc3dc20 1px, transparent 1px);
  background-size: 28px 28px;
}
.share-page {
  display: grid;
  place-items: center;
  padding: 28px 18px;
}
.share-shell {
  width: min(620px, 100%);
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  color: var(--ink);
  text-decoration: none;
}
.brand > span:last-child {
  display: grid;
}
.brand strong {
  font-size: 22px;
  line-height: 1.1;
}
.brand small {
  margin-top: 3px;
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
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
  font: bold 17px monospace;
}
.achievement-share-card,
.state-card {
  padding: 38px;
  border-radius: 10px;
  background: var(--card);
  box-shadow: 0 14px 40px #28495c20;
}
.achievement-share-card {
  text-align: center;
  border-color: #9fd4bf;
}
.trophy {
  display: grid;
  width: 74px;
  height: 74px;
  place-items: center;
  margin: 0 auto 20px;
  border: 1px solid #efca8c;
  border-radius: 9px;
  color: var(--orange);
  background: #fff8ea;
}
h1 {
  margin: 10px 0 0;
  font-size: clamp(28px, 6vw, 40px);
  line-height: 1.15;
}
.owner {
  margin: 16px 0 0;
  color: var(--muted);
}
.owner strong {
  color: var(--ink);
}
blockquote {
  margin: 25px 0;
  padding: 18px;
  border-left: 4px solid var(--blue);
  color: #43596b;
  background: #edf8fd;
  font-size: 17px;
  line-height: 1.55;
  text-align: left;
}
.unlock-date {
  color: var(--muted);
  font: 12px monospace;
}
.achievement-share-card .q-btn {
  min-width: 180px;
  min-height: 44px;
  margin-top: 18px;
}
.state-card {
  display: grid;
  min-height: 330px;
  place-content: center;
  justify-items: center;
  gap: 14px;
  text-align: center;
}
.state-card h1 {
  font-size: 27px;
}
.state-card p {
  margin: 0;
  color: var(--muted);
}
.locked-card .q-icon {
  color: var(--muted);
}
.error-card .q-icon {
  color: var(--orange);
}
.state-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
@media (max-width: 600px) {
  .achievement-share-card,
  .state-card {
    padding: 28px 20px;
  }
  .state-actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>
