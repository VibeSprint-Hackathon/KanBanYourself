<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import type { CharacterReaction, CharacterState } from '@/api/demo.types';
import idle1 from '@/assets/idle-1.png';
import idle2 from '@/assets/idle-2.png';
import idle3 from '@/assets/idle-3.png';
import idle4 from '@/assets/idle-4.png';
import idle5 from '@/assets/idle-5.png';
import idle6 from '@/assets/idle-6.png';
import idle7 from '@/assets/idle-7.png';
import idle8 from '@/assets/idle-8.png';
import coding1 from '@/assets/coding-1.png';
import coding2 from '@/assets/coding-2.png';
import coding3 from '@/assets/coding-3.png';
import coding4 from '@/assets/coding-4.png';
import celebrating1 from '@/assets/celebrating-1.png';
import celebrating2 from '@/assets/celebrating-2.png';
import celebrating3 from '@/assets/celebrating-3.png';
import celebrating4 from '@/assets/celebrating-4.png';
import starterHoodieSrc from '@/assets/hoody-0.png';
import blueHoodieSrc from '@/assets/hoody.png';

const props = defineProps<{
  name: string;
  persistentState: CharacterState;
  reaction: CharacterReaction | null;
  cosmeticKey: string;
  level: number;
}>();

const FRAME_DURATION: Record<CharacterState | 'celebrating', number> = {
  idle: 420,
  coding: 180,
  celebrating: 150,
};
const CELEBRATION_LOOPS = 2;
const idleFrames = [idle1, idle2, idle3, idle4, idle5, idle6, idle7, idle8];
const codingFrames = [coding1, coding2, coding3, coding4];
const celebratingFrames = [celebrating1, celebrating2, celebrating3, celebrating4];
const persistentFrames: Record<CharacterState, string[]> = {
  idle: idleFrames,
  coding: codingFrames,
};

const frameIndex = ref(0);
const celebrationStep = ref(0);
const isCelebrating = computed(() => props.reaction !== null);
const hoodieSrc = computed<string | null>(() => {
  if (props.cosmeticKey === 'base' || props.persistentState !== 'idle' || isCelebrating.value) {
    return null;
  }
  if (props.level === 5) {
    return starterHoodieSrc;
  }
  return props.level >= 6 ? blueHoodieSrc : null;
});
const activeFrames = computed(() =>
  isCelebrating.value ? celebratingFrames : persistentFrames[props.persistentState],
);
const currentFrame = computed(() => activeFrames.value[frameIndex.value] ?? activeFrames.value[0]);

let frameTimer: ReturnType<typeof setTimeout> | undefined;

function stopAnimation(): void {
  if (frameTimer) {
    clearTimeout(frameTimer);
    frameTimer = undefined;
  }
}

function scheduleNextFrame(): void {
  const mode = isCelebrating.value ? 'celebrating' : props.persistentState;
  frameTimer = setTimeout(() => {
    if (mode === 'celebrating') {
      const lastStep = celebratingFrames.length * CELEBRATION_LOOPS - 1;
      if (celebrationStep.value >= lastStep) {
        frameTimer = undefined;
        return;
      }
      celebrationStep.value += 1;
      frameIndex.value = celebrationStep.value % celebratingFrames.length;
    } else {
      frameIndex.value = (frameIndex.value + 1) % activeFrames.value.length;
    }
    scheduleNextFrame();
  }, FRAME_DURATION[mode]);
}

function restartAnimation(): void {
  stopAnimation();
  frameIndex.value = 0;
  celebrationStep.value = 0;
  scheduleNextFrame();
}

watch(() => [props.persistentState, props.reaction] as const, restartAnimation, {
  immediate: true,
});

onMounted(() => {
  [...idleFrames, ...codingFrames, ...celebratingFrames, starterHoodieSrc, blueHoodieSrc].forEach(
    (src) => {
      const image = new Image();
      image.src = src;
    },
  );
});

onUnmounted(stopAnimation);
</script>

<template>
  <div
    class="character-avatar"
    :class="[
      `is-${persistentState}`,
      reaction && `is-${reaction}`,
      { 'is-celebrating': isCelebrating },
    ]"
    role="img"
    :aria-label="`${name}'s character, ${isCelebrating ? 'celebrating' : persistentState}`"
  >
    <div class="character-stack">
      <img class="character-frame" :src="currentFrame" alt="" draggable="false" />
      <img v-if="hoodieSrc" class="character-cosmetic" :src="hoodieSrc" alt="" draggable="false" />
    </div>
  </div>
</template>

<style scoped>
.character-avatar {
  display: grid;
  place-items: center;
  min-width: 0;
  height: 100%;
}
.character-stack {
  position: relative;
  height: 100%;
  aspect-ratio: 1;
  pointer-events: none;
  filter: drop-shadow(0 18px 12px #56768b24);
  transition: filter 180ms ease;
}
.character-frame,
.character-cosmetic {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  user-select: none;
}
.character-cosmetic {
  z-index: 1;
}
.is-celebrating .character-stack {
  filter: drop-shadow(0 0 18px #79c9a980) drop-shadow(0 18px 12px #56768b24);
}
.is-level-up .character-stack {
  filter: drop-shadow(0 0 22px #f4bb5499) drop-shadow(0 18px 12px #56768b24);
}
@media (prefers-reduced-motion: reduce) {
  .character-stack {
    transition: none;
  }
}
</style>
