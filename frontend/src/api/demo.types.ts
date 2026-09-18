// GET /api/demo/state — docs/stages/00-contracts/CONTRACTS.md.
export interface Player {
  id: number;
  name: string;
  totalXp: number;
  level: number;
  nextLevelXp: number;
  title: string;
  cosmeticKey: string;
  characterState: 'idle' | 'coding';
}

export interface Quest {
  id: number;
  title: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  xpReward: number;
  assigneeId: number;
  externalReference: string | null;
}

export interface Raid {
  id: number;
  name: string;
  maxHp: number;
  currentHp: number;
  status: 'ACTIVE' | 'DEFEATED';
}

export interface DemoState {
  player: Player;
  quests: Quest[];
  raid: Raid;
  nextUnlock: { level: number; cosmeticKey: string; displayName: string };
}

export interface CosmeticUnlock {
  key: string;
  displayName: string;
}

export type CharacterReaction = 'happy' | 'level-up';

// POST /api/demo/quests/{questId}/complete response.
export interface ProgressionResult {
  eventId: string;
  applied: boolean;
  reason: 'ALREADY_COMPLETED' | null;
  xpGained: number;
  raidDamage: number;
  levelUp: boolean;
  unlockedCosmetic: CosmeticUnlock | null;
  reaction: CharacterReaction | null;
  bossDefeated: boolean;
  quest: Quest;
  player: Player;
  raid: Raid;
}
