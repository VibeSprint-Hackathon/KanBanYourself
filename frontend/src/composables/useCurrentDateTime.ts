import { onMounted, onUnmounted, ref } from 'vue';

const UPDATE_INTERVAL_MS = 30_000;

const dateFormatter = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
});

const timeFormatter = new Intl.DateTimeFormat('en-GB', {
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

function formatDate(value: Date): string {
  const parts = dateFormatter.formatToParts(value);
  const day = parts.find((part) => part.type === 'day')?.value ?? '';
  const month = parts.find((part) => part.type === 'month')?.value.slice(0, 3) ?? '';
  return `${day} ${month.toUpperCase()}`;
}

export function useCurrentDateTime() {
  const now = new Date();
  const dateLabel = ref(formatDate(now));
  const timeLabel = ref(timeFormatter.format(now));
  let timer: ReturnType<typeof setInterval> | undefined;

  function update(): void {
    const current = new Date();
    dateLabel.value = formatDate(current);
    timeLabel.value = timeFormatter.format(current);
  }

  onMounted(() => {
    update();
    timer = setInterval(update, UPDATE_INTERVAL_MS);
  });

  onUnmounted(() => {
    if (timer) clearInterval(timer);
  });

  return { dateLabel, timeLabel };
}
