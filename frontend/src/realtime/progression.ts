import { Client, type IMessage } from '@stomp/stompjs';
import type { ProgressionResponse } from '@/api/demo.types';

export type RealtimeStatus = 'connecting' | 'connected' | 'disconnected';
export type ProgressionHandler = (progression: ProgressionResponse) => void;
export type RealtimeStatusHandler = (status: RealtimeStatus) => void;

const PROGRESSION_TOPIC = '/topic/progression';

class ProgressionRealtimeClient {
  private readonly client: Client;
  private readonly progressionHandlers = new Set<ProgressionHandler>();
  private readonly statusHandlers = new Set<RealtimeStatusHandler>();
  private status: RealtimeStatus = 'disconnected';

  constructor() {
    this.client = new Client({
      brokerURL: resolveWebSocketUrl(),
      reconnectDelay: 3_000,
      connectionTimeout: 5_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      debug: () => undefined,
    });

    this.client.onConnect = () => {
      this.setStatus('connected');
      this.client.subscribe(PROGRESSION_TOPIC, (message) => this.handleMessage(message));
    };
    this.client.onWebSocketClose = () => this.setStatus('disconnected');
    this.client.onWebSocketError = () => this.setStatus('disconnected');
    this.client.onStompError = () => this.setStatus('disconnected');
  }

  connect(): void {
    if (this.client.active) {
      return;
    }
    this.setStatus('connecting');
    this.client.activate();
  }

  async disconnect(): Promise<void> {
    if (this.client.active) {
      await this.client.deactivate();
    }
    this.setStatus('disconnected');
  }

  subscribe(handler: ProgressionHandler): () => void {
    this.progressionHandlers.add(handler);
    return () => this.progressionHandlers.delete(handler);
  }

  subscribeStatus(handler: RealtimeStatusHandler): () => void {
    this.statusHandlers.add(handler);
    handler(this.status);
    return () => this.statusHandlers.delete(handler);
  }

  private handleMessage(message: IMessage): void {
    const progression = parseProgression(message.body);
    if (progression === null) {
      return;
    }
    this.progressionHandlers.forEach((handler) => handler(progression));
  }

  private setStatus(status: RealtimeStatus): void {
    if (this.status === status) {
      return;
    }
    this.status = status;
    this.statusHandlers.forEach((handler) => handler(status));
  }
}

function resolveWebSocketUrl(): string {
  if (import.meta.env.VITE_WS_URL) {
    return import.meta.env.VITE_WS_URL;
  }
  if (import.meta.env.DEV) {
    return 'ws://localhost:8080/ws';
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

function parseProgression(body: string): ProgressionResponse | null {
  try {
    const value: unknown = JSON.parse(body);
    if (
      !isRecord(value) ||
      typeof value.eventId !== 'string' ||
      typeof value.applied !== 'boolean'
    ) {
      return null;
    }
    if (
      !isRecord(value.quest) ||
      !isRecord(value.player) ||
      (value.raid !== null && !isRecord(value.raid))
    ) {
      return null;
    }
    return value as unknown as ProgressionResponse;
  } catch {
    return null;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

export const progressionRealtime = new ProgressionRealtimeClient();
