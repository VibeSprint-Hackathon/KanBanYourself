<script setup lang="ts">
import { computed, onUnmounted, ref, shallowRef } from 'vue';
import { mdiOpenInNew } from '@quasar/extras/mdi-v7';
import type { CharacterReaction, CharacterState } from '@/api/demo.types';
import CharacterAvatar from '@/components/dashboard/CharacterAvatar.vue';

interface DocumentPictureInPictureApi {
  requestWindow(options: { width: number; height: number }): Promise<Window>;
}

type PictureInPictureCapableWindow = Window & {
  documentPictureInPicture?: DocumentPictureInPictureApi;
};

defineProps<{
  name: string;
  persistentState: CharacterState;
  reaction: CharacterReaction | null;
  cosmeticKey: string;
  level: number;
}>();

const pipWindow = shallowRef<Window | null>(null);
const pipHost = shallowRef<HTMLElement | null>(null);
const notice = ref<string | null>(null);
const noticeVisible = ref(false);
const isOpen = computed(() => pipWindow.value !== null && !pipWindow.value.closed);
let noticeTimer: ReturnType<typeof setTimeout> | undefined;

async function openCompanion(): Promise<void> {
  if (isOpen.value) {
    pipWindow.value?.focus();
    return;
  }

  const api = (window as PictureInPictureCapableWindow).documentPictureInPicture;
  if (!api) {
    showNotice('Floating companion requires Chrome or Chromium desktop.');
    return;
  }

  try {
    clearReferences();
    const companionWindow = await api.requestWindow({ width: 260, height: 320 });
    const host = prepareDocument(companionWindow.document);
    pipWindow.value = companionWindow;
    pipHost.value = host;
    companionWindow.addEventListener('pagehide', handleWindowClosed, { once: true });
  } catch (cause) {
    clearReferences();
    if (cause instanceof DOMException && cause.name === 'NotAllowedError') return;
    showNotice('Could not open the floating companion.');
  }
}

function prepareDocument(targetDocument: Document): HTMLElement {
  targetDocument.title = 'VibeSprint Companion';
  copyStyles(document, targetDocument);

  const style = targetDocument.createElement('style');
  style.textContent = `
    :root { color-scheme: light; }
    html, body { width: 100%; height: 100%; margin: 0; overflow: hidden; }
    body {
      background-color: #edf9ff;
      background-image:
        linear-gradient(#9ac6d51f 1px, transparent 1px),
        linear-gradient(90deg, #9ac6d51f 1px, transparent 1px);
      background-size: 24px 24px;
    }
    #vibesprint-companion { width: 100%; height: 100%; }
  `;
  targetDocument.head.append(style);

  const host = targetDocument.createElement('div');
  host.id = 'vibesprint-companion';
  targetDocument.body.append(host);
  return host;
}

function copyStyles(source: Document, target: Document): void {
  source
    .querySelectorAll<HTMLStyleElement | HTMLLinkElement>('style, link[rel="stylesheet"]')
    .forEach((node) => {
      const clone = node.cloneNode(true) as HTMLStyleElement | HTMLLinkElement;
      if (clone instanceof HTMLLinkElement && node instanceof HTMLLinkElement) {
        clone.href = node.href;
      }
      target.head.append(clone);
    });
}

function handleWindowClosed(): void {
  pipWindow.value?.removeEventListener('pagehide', handleWindowClosed);
  clearReferences();
}

function clearReferences(): void {
  pipHost.value = null;
  pipWindow.value = null;
}

function showNotice(message: string): void {
  notice.value = message;
  noticeVisible.value = true;
  if (noticeTimer) clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => {
    noticeVisible.value = false;
    notice.value = null;
    noticeTimer = undefined;
  }, 2_800);
}

onUnmounted(() => {
  const companionWindow = pipWindow.value;
  companionWindow?.removeEventListener('pagehide', handleWindowClosed);
  if (companionWindow && !companionWindow.closed) companionWindow.close();
  if (noticeTimer) clearTimeout(noticeTimer);
  clearReferences();
});
</script>

<template>
  <q-btn
    flat
    round
    dense
    :icon="mdiOpenInNew"
    :class="['companion-button', { 'is-open': isOpen }]"
    :aria-label="isOpen ? 'Companion open' : 'Open companion'"
    @click.stop="openCompanion"
  >
    <q-tooltip v-model="noticeVisible">
      {{ notice ?? (isOpen ? 'Companion open' : 'Open companion') }}
    </q-tooltip>
  </q-btn>

  <Teleport v-if="pipHost" :to="pipHost">
    <main class="pip-companion">
      <CharacterAvatar
        class="pip-character"
        :name="name"
        :persistent-state="persistentState"
        :reaction="reaction"
        :cosmetic-key="cosmeticKey"
        :level="level"
      />
      <div class="pip-player">
        <strong class="pip-player__name">{{ name }}</strong>
        <span class="pip-player__level">LVL {{ level }}</span>
      </div>
    </main>
  </Teleport>
</template>

<style scoped>
.companion-button {
  flex: 0 0 auto;
  color: var(--muted);
}
.companion-button:hover,
.companion-button.is-open {
  color: var(--blue);
  background: #c1e5f5;
}
.pip-companion {
  box-sizing: border-box;
  display: grid;
  width: 100%;
  height: 100%;
  padding: 12px 12px 14px;
  grid-template-rows: minmax(0, 1fr) 28px;
  color: #17364a;
  font-family: Inter, system-ui, sans-serif;
}
.pip-character {
  width: 100%;
  min-height: 0;
}
.pip-player {
  align-self: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: 0;
  padding: 0 10px;
}
.pip-player__name {
  min-width: 0;
  overflow: hidden;
  color: #17364a;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pip-player__level {
  flex: 0 0 auto;
  padding: 3px 7px;
  border: 1px solid #7fcbe5;
  border-radius: 999px;
  background: #d9f3fc;
  color: #087ca3;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.8px;
  line-height: 1;
}
</style>
