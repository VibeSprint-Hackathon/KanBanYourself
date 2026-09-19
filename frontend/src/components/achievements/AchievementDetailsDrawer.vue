<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { mdiCheck, mdiLinkedin, mdiLockOutline } from '@quasar/extras/mdi-v7';
import type { Player } from '@/api/demo.types';
import { copyText, shareAchievementOnLinkedIn } from '@/utils/achievementShare';
import { achievementProgress, type Achievement } from './achievement.types';

const open = defineModel<boolean>({ required: true });
const props = defineProps<{
  achievement: Achievement;
  player: Pick<Player, 'id' | 'name'>;
}>();
const router = useRouter();
const shareText = ref('');
const shareFeedback = ref('');

watch(
  () => props.achievement.key,
  () => {
    shareText.value = '';
    shareFeedback.value = '';
  },
);

async function shareOnLinkedIn(): Promise<void> {
  const attempt = shareAchievementOnLinkedIn(router, props.player, props.achievement);
  shareText.value = attempt.text;
  const copied = await attempt.copied;
  if (!attempt.linkedinOpened) {
    shareFeedback.value = 'LinkedIn could not be opened. Your post text is ready below.';
  } else if (copied) {
    shareFeedback.value = 'Post text copied · paste it into LinkedIn.';
  } else {
    shareFeedback.value = 'LinkedIn opened. Copy the suggested post below.';
  }
}

async function copySuggestedText(): Promise<void> {
  shareFeedback.value = (await copyText(shareText.value))
    ? 'Post text copied · paste it into LinkedIn.'
    : 'Copy was blocked. Select the text below and copy it manually.';
}
</script>

<template>
  <q-dialog
    v-model="open"
    position="right"
    full-height
    class="quest-dialog"
    :transition-duration="0"
    aria-labelledby="achievement-details-title"
  >
    <q-card flat class="achievement-drawer">
      <header class="drawer-header spread">
        <div>
          <div class="eyebrow blue">Achievement</div>
          <h2 id="achievement-details-title">{{ achievement.name }}</h2>
        </div>
        <q-btn
          flat
          square
          icon="close"
          class="close-icon"
          aria-label="Close achievement details"
          @click="open = false"
        />
      </header>
      <div class="drawer-body">
        <div class="spread">
          <span class="eyebrow muted">Status</span>
          <span class="achievement-badge" :class="{ unlocked: achievement.unlocked }">
            <q-icon :name="achievement.unlocked ? mdiCheck : mdiLockOutline" size="16px" />
            {{ achievement.unlocked ? 'Unlocked' : 'Locked' }}
          </span>
        </div>
        <section>
          <h3 class="eyebrow muted">Description</h3>
          <p>{{ achievement.description }}</p>
        </section>
        <section>
          <div class="spread">
            <h3 class="eyebrow muted">Progress</h3>
            <strong class="blue">
              {{ achievement.currentProgress.toLocaleString('en-US') }} /
              {{ achievement.targetProgress.toLocaleString('en-US') }}
            </strong>
          </div>
          <q-linear-progress
            class="progress-track"
            :value="achievementProgress(achievement)"
            size="13px"
            aria-label="Achievement progress"
          />
        </section>
        <div class="reward-box spread">
          <span class="eyebrow orange">Reward</span>
          <strong>{{ achievement.rewardLabel }}</strong>
        </div>
        <div v-if="achievement.unlockedAt" class="unlocked-date spread">
          <span class="eyebrow muted">Unlocked</span><strong>{{ achievement.unlockedAt }}</strong>
        </div>
        <section v-if="achievement.unlocked" class="share-section">
          <q-btn
            unelevated
            no-caps
            color="primary"
            :icon="mdiLinkedin"
            label="Share on LinkedIn"
            aria-label="Share this achievement on LinkedIn"
            @click="shareOnLinkedIn"
          />
          <div class="share-feedback" aria-live="polite">{{ shareFeedback }}</div>
          <div v-if="shareText" class="share-copy">
            <label for="achievement-share-text" class="eyebrow muted">Suggested post</label>
            <textarea id="achievement-share-text" :value="shareText" readonly rows="7" />
            <q-btn flat no-caps label="Copy text" @click="copySuggestedText" />
          </div>
        </section>
        <p v-else class="locked-share-note">Unlock this Achievement to share it.</p>
      </div>
      <footer class="drawer-footer">
        <q-btn flat no-caps label="Close" @click="open = false" />
      </footer>
    </q-card>
  </q-dialog>
</template>

<style scoped>
.achievement-drawer {
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
  line-height: 1.3;
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
.achievement-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 11px;
  border-radius: 18px;
  color: var(--muted);
  background: #e4edf3;
  font-size: 10px;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.achievement-badge.unlocked {
  color: var(--green);
  background: #dcefe8;
}
section {
  margin-top: 25px;
}
section h3 {
  margin: 0;
}
section p {
  margin: 11px 0 0;
  color: #43596b;
  font-size: 15px;
  line-height: 1.6;
}
section .progress-track {
  margin-top: 15px;
}
.reward-box {
  margin-top: 20px;
  padding: 15px 16px;
  border: 1px solid #efca8c;
  border-radius: 5px;
  background: #fff8ea99;
}
.reward-box strong {
  color: var(--orange);
  font-size: 19px;
}
.unlocked-date {
  margin-top: 21px;
}
.unlocked-date strong {
  font-size: 14px;
}
.share-section {
  display: grid;
  gap: 10px;
  margin-top: 26px;
}
.share-section > .q-btn {
  min-height: 44px;
}
.share-feedback {
  min-height: 18px;
  color: var(--muted);
  font-size: 12px;
}
.share-copy {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #f6fafc;
}
.share-copy textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--border);
  border-radius: 4px;
  padding: 10px;
  color: var(--ink);
  background: var(--card);
  font: 12px/1.5 monospace;
}
.locked-share-note {
  margin: 24px 0 0;
  color: var(--muted);
  font-size: 13px;
}
.drawer-footer {
  padding: 20px 24px;
  border-top: 1px solid var(--border);
}
.drawer-footer .q-btn {
  width: 100%;
  height: 46px;
  border: 1px solid var(--border);
  color: var(--muted);
  font-weight: 600;
}
</style>
