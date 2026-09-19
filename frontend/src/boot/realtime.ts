import { defineBoot } from '#q-app';
import { progressionRealtime } from '@/realtime/progression';

export default defineBoot(() => {
  // Fixture-only frontend runs stay offline. Realtime becomes opt-in when integration supplies a URL.
  if (!import.meta.env.VITE_WS_URL) return;

  const unsubscribeStatus = import.meta.env.DEV
    ? progressionRealtime.subscribeStatus((status) => {
        console.info(`[realtime] ${status}`);
      })
    : undefined;
  const unsubscribeProgression = import.meta.env.DEV
    ? progressionRealtime.subscribe((progression) => {
        console.info(`[realtime] progression ${progression.eventId}`);
      })
    : undefined;

  progressionRealtime.connect();

  window.addEventListener(
    'beforeunload',
    () => {
      unsubscribeStatus?.();
      unsubscribeProgression?.();
      void progressionRealtime.disconnect();
    },
    { once: true },
  );
});
