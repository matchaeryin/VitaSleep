/**
 * Google Health Connect API 客户端
 * 
 * 负责从 Google Health 拉取用户健康数据
 * 包括：心率、睡眠、步数、血压等
 */

import axios, { AxiosInstance } from 'axios';

export interface GoogleHealthConfig {
  accessToken: string;
  userId?: string;
}

export interface DataPoint {
  timestamp: number;
  value: number;
  endTime?: number;
}

export interface HeartRateData {
  source: 'google_health';
  metric: 'heart_rate';
  dataPoints: DataPoint[];
  unit: 'bpm';
}

export interface SleepData {
  source: 'google_health';
  metric: 'sleep';
  dataPoints: Array<{
    timestamp: number;
    sleepStage: number;
    duration: number;
  }>;
  unit: 'minutes';
}

export interface StepsData {
  source: 'google_health';
  metric: 'steps';
  dataPoints: DataPoint[];
  unit: 'count';
}

export class GoogleHealthClient {
  private client: AxiosInstance;
  private accessToken: string;
  private userId: string = 'me';

  constructor(config: GoogleHealthConfig) {
    this.accessToken = config.accessToken;
    if (config.userId) {
      this.userId = config.userId;
    }

    this.client = axios.create({
      baseURL: 'https://www.googleapis.com/fitness/v1',
      headers: {
        'Authorization': `Bearer ${this.accessToken}`,
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    });
  }

  updateToken(accessToken: string) {
    this.accessToken = accessToken;
    this.client.defaults.headers['Authorization'] = `Bearer ${accessToken}`;
  }

  async getHeartRate(startTime: number, endTime: number, bucketDuration: number = 300): Promise<HeartRateData> {
    try {
      const response = await this.client.post(
        `/users/${this.userId}/dataset/aggregate`,
        {
          aggregateBy: [{ dataTypeName: 'com.google.heart_rate.bpm' }],
          bucketByTime: { duration: `${bucketDuration}s` },
          startTimeMillis: startTime.toString(),
          endTimeMillis: endTime.toString(),
        }
      );
      return this.transformHeartRate(response.data);
    } catch (error: any) {
      console.error('Failed to fetch heart rate:', error.message);
      throw error;
    }
  }

  async getSleep(startTime: number, endTime: number): Promise<SleepData> {
    try {
      const response = await this.client.post(
        `/users/${this.userId}/dataset/aggregate`,
        {
          aggregateBy: [{ dataTypeName: 'com.google.sleep.segment' }],
          bucketByTime: { duration: '86400s' },
          startTimeMillis: startTime.toString(),
          endTimeMillis: endTime.toString(),
        }
      );
      return this.transformSleep(response.data);
    } catch (error: any) {
      console.error('Failed to fetch sleep:', error.message);
      throw error;
    }
  }

  async getSteps(startTime: number, endTime: number): Promise<StepsData> {
    try {
      const response = await this.client.post(
        `/users/${this.userId}/dataset/aggregate`,
        {
          aggregateBy: [{ dataTypeName: 'com.google.step_count.delta' }],
          bucketByTime: { duration: '3600s' },
          startTimeMillis: startTime.toString(),
          endTimeMillis: endTime.toString(),
        }
      );
      return this.transformSteps(response.data);
    } catch (error: any) {
      console.error('Failed to fetch steps:', error.message);
      throw error;
    }
  }

  private transformHeartRate(googleData: any): HeartRateData {
    return {
      source: 'google_health',
      metric: 'heart_rate',
      dataPoints: googleData.bucket
        .flatMap((b: any) => b.dataset || [])
        .flatMap((d: any) => d.point || [])
        .map((p: any) => ({
          timestamp: parseInt(p.startTimeNanos) / 1e6,
          value: p.value?.fpVal || 0,
          endTime: parseInt(p.endTimeNanos) / 1e6,
        }))
        .filter((p: any) => p.value > 0),
      unit: 'bpm',
    };
  }

  private transformSleep(googleData: any): SleepData {
    return {
      source: 'google_health',
      metric: 'sleep',
      dataPoints: googleData.bucket
        .flatMap((b: any) => b.dataset || [])
        .flatMap((d: any) => d.point || [])
        .map((p: any) => ({
          timestamp: parseInt(p.startTimeNanos) / 1e6,
          sleepStage: p.value?.intVal || 0,
          duration: (parseInt(p.endTimeNanos) - parseInt(p.startTimeNanos)) / 6e7,
        })),
      unit: 'minutes',
    };
  }

  private transformSteps(googleData: any): StepsData {
    return {
      source: 'google_health',
      metric: 'steps',
      dataPoints: googleData.bucket
        .flatMap((b: any) => b.dataset || [])
        .flatMap((d: any) => d.point || [])
        .map((p: any) => ({
          timestamp: parseInt(p.startTimeNanos) / 1e6,
          value: p.value?.intVal || 0,
          endTime: parseInt(p.endTimeNanos) / 1e6,
        })),
      unit: 'count',
    };
  }
}

export default GoogleHealthClient;
