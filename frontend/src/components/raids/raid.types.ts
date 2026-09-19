import type { Raid } from '@/api/demo.types';

export interface RaidDamageFeedback {
  previousHp: number;
  damageReceived: number;
}

export function raidPercent(raid: Raid): number {
  if (raid.maxHp <= 0) return 0;
  return Math.round((raid.currentHp / raid.maxHp) * 100);
}
