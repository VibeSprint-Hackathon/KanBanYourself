import { api } from '@/api/http';
import type { AchievementResponse } from '@/components/achievements/achievement.types';

export async function getPlayerAchievements(playerId: number): Promise<AchievementResponse> {
  const response = await api.get<AchievementResponse>(`/players/${playerId}/achievements`);
  return response.data;
}
