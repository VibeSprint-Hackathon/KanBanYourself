import { api } from '@/api/http';
import type {
  CreateQuestRequest,
  DemoState,
  MoveQuestRequest,
  Player,
  ProgressionResult,
  UpdateQuestRequest,
} from '@/api/demo.types';

export type ProgressionSource = 'DEMO' | 'GITHUB';

export interface CompleteQuestCommand {
  eventId: string;
  source: ProgressionSource;
}

export async function getDemoState(): Promise<DemoState> {
  const response = await api.get<DemoState>('/demo/state');
  return response.data;
}

export async function getPlayers(): Promise<Player[]> {
  const response = await api.get<Player[]>('/players');
  return response.data;
}

export async function completeDemoQuest(
  questId: number,
  command: CompleteQuestCommand,
): Promise<ProgressionResult> {
  const response = await api.post<ProgressionResult>(`/demo/quests/${questId}/complete`, command);
  return response.data;
}

export async function resetDemoState(): Promise<DemoState> {
  const response = await api.post<DemoState>('/demo/reset');
  return response.data;
}

export async function createDemoQuest(request: CreateQuestRequest): Promise<DemoState> {
  const response = await api.post<DemoState>('/demo/quests', request);
  return response.data;
}

export async function updateDemoQuest(
  questId: number,
  request: UpdateQuestRequest,
): Promise<DemoState> {
  const response = await api.put<DemoState>(`/demo/quests/${questId}`, request);
  return response.data;
}

export async function deleteDemoQuest(questId: number): Promise<DemoState> {
  const response = await api.delete<DemoState>(`/demo/quests/${questId}`);
  return response.data;
}

export async function moveDemoQuest(
  questId: number,
  request: MoveQuestRequest,
): Promise<DemoState> {
  const response = await api.patch<DemoState>(`/demo/quests/${questId}/move`, request);
  return response.data;
}
