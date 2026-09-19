import { api } from '@/api/http';
import type { DemoState, ProgressionResult } from '@/api/demo.types';

export type ProgressionSource = 'DEMO' | 'GITHUB';

export interface CompleteQuestCommand {
  eventId: string;
  source: ProgressionSource;
}

export async function getDemoState(): Promise<DemoState> {
  const response = await api.get<DemoState>('/demo/state');
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
