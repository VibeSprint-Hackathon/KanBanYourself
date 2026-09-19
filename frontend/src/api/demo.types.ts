// Контракты GET /api/demo/state и POST /api/demo/quests/{questId}/complete.
export type QuestStatus = 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'TESTING' | 'DONE';
export type RaidStatus = 'ACTIVE' | 'DEFEATED';
export type CharacterState = 'idle' | 'coding';
export type CharacterReaction = 'happy' | 'level-up';

export interface Player {
  id: number;
  name: string;
  totalXp: number;
  level: number;
  nextLevelXp: number | null;
  title: string;
  cosmeticKey: string;
  characterState: CharacterState;
}

export interface Quest {
  id: number;
  title: string;
  description: string;
  status: QuestStatus;
  progress: number | null;
  xpReward: number;
  assigneeId: number;
  externalReference: string | null;
  sortOrder: number;
}

export interface CreateQuestRequest {
  title: string;
  description: string;
  status: Exclude<QuestStatus, 'DONE'>;
  progress: number | null;
  xpReward: number;
  externalReference: string | null;
}

export interface UpdateQuestRequest {
  title: string;
  description: string;
  status: QuestStatus;
  progress: number | null;
  xpReward: number;
  externalReference: string | null;
}

export interface MoveQuestRequest {
  status: Exclude<QuestStatus, 'DONE'>;
  beforeQuestId: number | null;
}

export interface Raid {
  id: number;
  name: string;
  maxHp: number;
  currentHp: number;
  status: RaidStatus;
}

export interface UnlockTarget {
  level: number;
  cosmeticKey: string;
  displayName: string;
}

export interface DemoState {
  player: Player;
  quests: Quest[];
  raid: Raid;
  nextUnlock: UnlockTarget | null;
}

export interface CosmeticUnlock {
  key: string;
  displayName: string;
}

export interface ApiError {
  code: string;
  message: string;
}

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

// Совместимые имена для realtime-слоя и имён DTO backend.
export type PlayerView = Player;
export type QuestView = Quest;
export type RaidView = Raid;
export type CosmeticView = CosmeticUnlock;
export type ProgressionReaction = CharacterReaction;
export type ProgressionResponse = ProgressionResult;
