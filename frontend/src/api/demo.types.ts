export type QuestStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type RaidStatus = 'ACTIVE' | 'DEFEATED';
export type CharacterState = 'idle' | 'coding';
export type ProgressionReaction = 'happy' | 'level-up';

export interface QuestView {
  id: number;
  title: string;
  status: QuestStatus;
  xpReward: number;
  assigneeId: number;
  externalReference: string | null;
}

export interface PlayerView {
  id: number;
  name: string;
  totalXp: number;
  level: number;
  nextLevelXp: number | null;
  title: string;
  cosmeticKey: string;
  characterState: CharacterState;
}

export interface RaidView {
  id: number;
  name: string;
  maxHp: number;
  currentHp: number;
  status: RaidStatus;
}

export interface CosmeticView {
  key: string;
  displayName: string;
}

export interface ProgressionResponse {
  eventId: string;
  applied: boolean;
  reason: 'ALREADY_COMPLETED' | null;
  xpGained: number;
  raidDamage: number;
  levelUp: boolean;
  unlockedCosmetic: CosmeticView | null;
  reaction: ProgressionReaction | null;
  bossDefeated: boolean;
  quest: QuestView;
  player: PlayerView;
  raid: RaidView;
}
