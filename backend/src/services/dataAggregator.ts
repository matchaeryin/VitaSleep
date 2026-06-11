/**
 * ??????
 * ? Google Health ???????????? MetricPayload ??
 */

import GoogleHealthClient, { HeartRateData, SleepData, StepsData } from './googleHealthClient.js';
import type { MetricPayload } from './algorithmClient.js';

export class DataAggregator {
  private googleClient: GoogleHealthClient;

  constructor(accessToken: string) {
    this.googleClient = new GoogleHealthClient({ accessToken });
  }

  async aggregateForAlgorithm(userId: string, timeRange: { start: number; end: number }): Promise<{
    metricPayload: MetricPayload;
    hoursSinceLastSleep: number | null;
  }> {
    const [heartRateData, sleepData, stepsData] = await Promise.all([
      this.googleClient.getHeartRate(timeRange.start, timeRange.end, 300).catch(() => null),
      this.googleClient.getSleep(timeRange.start, timeRange.end).catch(() => null),
      this.googleClient.getSteps(timeRange.start, timeRange.end).catch(() => null),
    ]);

    const metricPayload: MetricPayload = {
      resting_hr: this.extractRestingHeartRate(heartRateData),
      hrv_rmssd: this.calculateHRV_RMSSD(heartRateData),
      hrv_sdnn: this.calculateHRV_SDNN(heartRateData),
      deep_sleep_minutes: this.calculateSleepStage(sleepData, 'deep'),
      light_sleep_minutes: this.calculateSleepStage(sleepData, 'light'),
      rem_sleep_minutes: this.calculateSleepStage(sleepData, 'rem'),
      awake_minutes: this.calculateSleepStage(sleepData, 'awake'),
      steps: this.calculateTotalSteps(stepsData),
      active_calories: null,
      active_minutes: null,
    };

    const hoursSinceLastSleep = this.calculateHoursSinceLastSleep(sleepData);

    return { metricPayload, hoursSinceLastSleep };
  }

  private extractRestingHeartRate(data: HeartRateData | null): number | null {
    if (!data || !data.dataPoints.length) return null;
    const values = data.dataPoints.map(p => p.value).filter(v => v > 0 && v < 250);
    return values.length ? Math.min(...values) : null;
  }

  private calculateHRV_RMSSD(data: HeartRateData | null): number | null {
    if (!data || data.dataPoints.length < 2) return null;
    const values = data.dataPoints.map(p => p.value).filter(v => v > 0 && v < 250);
    const diffs = [];
    for (let i = 1; i < values.length; i++) diffs.push(values[i] - values[i - 1]);
    const mean = diffs.reduce((a, b) => a + b, 0) / diffs.length;
    const variance = diffs.reduce((a, b) => a + Math.pow(b - mean, 2), 0) / diffs.length;
    return Math.sqrt(variance) * 10;
  }

  private calculateHRV_SDNN(data: HeartRateData | null): number | null {
    if (!data || data.dataPoints.length < 2) return null;
    const values = data.dataPoints.map(p => p.value).filter(v => v > 0 && v < 250);
    const mean = values.reduce((a, b) => a + b, 0) / values.length;
    const variance = values.reduce((a, b) => a + Math.pow(b - mean, 2), 0) / values.length;
    return Math.sqrt(variance);
  }

  private calculateSleepStage(data: SleepData | null, stage: string): number | null {
    if (!data || !data.dataPoints.length) return null;
    const stageMap: Record<string, number> = { awake: 1, light: 2, deep: 3, rem: 4 };
    const total = data.dataPoints.filter(p => p.sleepStage === stageMap[stage]).reduce((sum, p) => sum + p.duration, 0);
    return total > 0 ? total : null;
  }

  private calculateHoursSinceLastSleep(data: SleepData | null): number | null {
    if (!data || !data.dataPoints.length) return null;
    const endTimes = data.dataPoints.map((p: any) => p.endTime || p.timestamp).filter((t: any) => t);
    if (!endTimes.length) return null;
    const lastSleepEnd = Math.max(...endTimes);
    return Math.min((Date.now() - lastSleepEnd) / (1000 * 60 * 60), 24);
  }

  private calculateTotalSteps(data: StepsData | null): number | null {
    if (!data || !data.dataPoints.length) return null;
    const total = data.dataPoints.reduce((sum, p) => sum + p.value, 0);
    return total > 0 ? total : null;
  }

  updateToken(accessToken: string) {
    this.googleClient.updateToken(accessToken);
  }
}

export default DataAggregator;
