import { api } from '@/api/http';
import type { CreateRaidRequest, Raid, UpdateRaidRequest } from '@/api/demo.types';

export async function getRaids(): Promise<Raid[]> {
  const response = await api.get<Raid[]>('/raids');
  return response.data;
}

export async function createRaid(request: CreateRaidRequest): Promise<Raid> {
  const response = await api.post<Raid>('/raids', request);
  return response.data;
}

export async function updateRaid(raidId: number, request: UpdateRaidRequest): Promise<Raid> {
  const response = await api.put<Raid>(`/raids/${raidId}`, request);
  return response.data;
}

export async function activateRaid(raidId: number): Promise<Raid> {
  const response = await api.post<Raid>(`/raids/${raidId}/activate`);
  return response.data;
}

export async function cancelRaid(raidId: number): Promise<Raid> {
  const response = await api.post<Raid>(`/raids/${raidId}/cancel`);
  return response.data;
}

export async function completeRaid(raidId: number): Promise<Raid> {
  const response = await api.post<Raid>(`/raids/${raidId}/complete`);
  return response.data;
}

export async function deleteRaid(raidId: number): Promise<void> {
  await api.delete(`/raids/${raidId}`);
}
