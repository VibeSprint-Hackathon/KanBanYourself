<script setup lang="ts">
import { ref, watch } from 'vue';
import type { CharacterReaction } from '@/api/demo.types';
const props = defineProps<{
  src: string | null;
  name: string;
  reaction: CharacterReaction | null;
}>();
const failed = ref(false);
watch(
  () => props.src,
  () => {
    failed.value = false;
  },
);
</script>

<template>
  <div class="character-avatar" :class="reaction && `is-${reaction}`">
    <img v-if="src && !failed" :src="src" :alt="`${name}'s character`" @error="failed = true" />
    <div
      v-else
      class="character-placeholder"
      role="img"
      :aria-label="`${name}: character placeholder`"
    >
      <q-icon name="person" size="190px" />
      <span class="character-code">&lt;/&gt;</span>
      <span v-if="reaction" class="reaction-label">{{ reaction }}</span>
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
img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.character-placeholder {
  position: relative;
  display: grid;
  place-items: center;
  width: 180px;
  height: 360px;
  color: #599bbf;
  border-radius: 100px;
  background: radial-gradient(ellipse, #c2e6f6, transparent 70%);
  filter: drop-shadow(0 18px 12px #56768b24);
}
.character-code {
  position: absolute;
  top: 59%;
  font: bold 27px monospace;
  color: #ecf8ff;
}
.reaction-label {
  position: absolute;
  top: 24px;
  padding: 6px 11px;
  border-radius: 18px;
  color: #fff;
  background: var(--green);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.is-level-up .character-placeholder {
  outline: 4px solid #f4bb54;
  box-shadow: 0 0 28px #f4bb5480;
}
.is-level-up .reaction-label {
  background: var(--orange);
}
.is-happy .character-placeholder {
  outline: 4px solid #79c9a9;
}
</style>
