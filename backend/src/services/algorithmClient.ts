/**
 * 算法服务客户端
 * 对接已有算法服务：/home/admin/Desktop/Vita/算法/main.py
 * 启动：cd /home/admin/Desktop/Vita/算法 && uvicorn main:app --reload --port 8001
 */

import axios from 'axios';

export interface MetricPayload {
  resting_hr?: number | null;
  hrv_rmssd?: number | null;
  hrv_sdnn?: number | null;
  systolic?: number | null;
  diastolic?: number | null;
  spo2?: number | null;
  deep_sleep_minutes?: number | null;
  light_sleep_minutes?: number | null;
  rem_sleep_minutes?: number | null;
  awake_minutes?: number | null;
  steps?: number | null;
  active_calories?: number | null;
  active_minutes?: number | null;
}

export interface BatteryRequest {
  user_id: string;
  hours_since_last_sleep?: number | null;
  metrics: MetricPayload;
}

export interface BatteryResponse {
  user_id: string;
  battery: number;
  raw_battery: number;
  decay_factor: number;
  components: {
    sleep: { score: number; detail: any };
    hrv: { score: number; detail: any };
    spo2: { score: number };
    activity: { score: number; detail: any };
  };
  confidence: number;
}

export interface FatigueResponse {
  user_id: string;
  fatigue_index: number;
  level: 'low' | 'moderate' | 'high' | 'severe';
  confidence: number;
}

export interface SleepResponse {
  user_id: string;
  sleep_score: number;
  detail: any;
  confidence: number;
}

export interface CardioResponse {
  user_id: string;
  cardio_index: number;
  detail: any;
  confidence: number;
}

export class AlgorithmClient {
  private client: any;
  private baseUrl: string;

  constructor(baseUrl: string = 'http://localhost:8001') {
    this.baseUrl = baseUrl;
    this.client = axios.create({
      baseURL: this.baseUrl,
      timeout: 30000,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  async calculateBattery(request: BatteryRequest): Promise<BatteryResponse> {
    const response = await this.client.post('/calculate/battery', request);
    return response.data as BatteryResponse;
  }

  async calculateFatigue(request: BatteryRequest): Promise<FatigueResponse> {
    const response = await this.client.post('/calculate/fatigue', request);
    return response.data as FatigueResponse;
  }

  async calculateSleep(userId: string, metrics: MetricPayload): Promise<SleepResponse> {
    const response = await this.client.post('/calculate/sleep', { user_id: userId, metrics });
    return response.data as SleepResponse;
  }

  async calculateCardio(userId: string, metrics: MetricPayload): Promise<CardioResponse> {
    const response = await this.client.post('/calculate/cardio', { user_id: userId, metrics });
    return response.data as CardioResponse;
  }

  async healthCheck(): Promise<boolean> {
    try {
      const response = await this.client.get('/health', { timeout: 5000 });
      return response.status === 200;
    } catch {
      return false;
    }
  }
}

export default AlgorithmClient;
