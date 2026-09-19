import { onUnmounted } from 'vue';

export function useSoundEffect(url: string) {
  const audio = typeof Audio === 'undefined' ? null : new Audio(url);

  if (audio) {
    audio.preload = 'auto';
  }

  function play(): void {
    if (!audio) return;

    try {
      audio.currentTime = 0;
      void audio.play().catch(() => undefined);
    } catch {
      // Sound is best-effort: missing files and autoplay rejection must not affect progression.
    }
  }

  onUnmounted(() => {
    if (!audio) return;
    audio.pause();
    audio.removeAttribute('src');
    audio.load();
  });

  return { play };
}
