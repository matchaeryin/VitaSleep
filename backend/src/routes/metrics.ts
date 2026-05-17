/**
 * 指标更新路由 - 对接已有算法服务
 */

import { Router } from 'express';
import { db } from '../db/init.js';
import { DataAggregator } from '../services/dataAggregator.js';
import { AlgorithmClient } from '../services/algorithmClient.js';

export const metricsRouter = Router();
const algorithmClient = new AlgorithmClient(process.env.ALGORITHM_URL || 'http://localhost:8001');

metricsRouter.post('/update/:userId', async (req, res) => {
  const { userId } = req.params;
  const { googleAccessToken } = req.body;

  if (!googleAccessToken) {
    return res.status(400).json({ success: false, error: 'Google access token required' });
  }

  try {
    const aggregator = new DataAggregator(googleAccessToken);
    const timeRange = { end: Date.now(), start: Date.now() - 24 * 60 * 60 * 1000 };

    const { metricPayload, hoursSinceLastSleep } = await aggregator.aggregateForAlgorithm(userId, timeRange);

    const [battery, fatigue, sleep, cardio] = await Promise.all([
      algorithmClient.calculateBattery({ user_id: userId, hours_since_last_sleep: hoursSinceLastSleep, metrics: metricPayload }),
      algorithmClient.calculateFatigue({ user_id: userId, hours_since_last_sleep: hoursSinceLastSleep, metrics: metricPayload }),
      (metricPayload.deep_sleep_minutes || metricPayload.light_sleep_minutes) ? algorithmClient.calculateSleep(userId, metricPayload) : Promise.resolve(null),
      (metricPayload.resting_hr || metricPayload.hrv_rmssd) ? algorithmClient.calculateCardio(userId, metricPayload) : Promise.resolve(null),
    ]);

    const runId = await storeResults(userId, { battery, fatigue, sleep, cardio });

    res.json({ success: true, data: { battery, fatigue, sleep, cardio, run_id: runId } });
  } catch (error: any) {
    console.error('Metrics update failed:', error.message);
    res.status(500).json({ success: false, error: error.message });
  }
});

metricsRouter.get('/:userId', async (req, res) => {
  const { userId } = req.params;
  const latest = db.prepare('SELECT * FROM algorithm_runs WHERE user_id = ? ORDER BY computed_at DESC LIMIT 1').get(userId) as any;
  
  if (!latest) {
    return res.json({ success: true, data: null, message: 'No data available' });
  }

  res.json({
    success: true,
    data: {
      battery_level: latest.battery_level,
      fatigue_level: latest.fatigue_level,
      sleep_score: latest.sleep_score,
      cardio_index: latest.cardio_index,
      confidence: latest.confidence,
      computed_at: latest.computed_at,
    },
  });
});

async function storeResults(userId: string, results: any): Promise<number> {
  const now = new Date().toISOString();
  const result = db.prepare(`
    INSERT INTO algorithm_runs (user_id, battery_level, fatigue_level, sleep_score, cardio_index, confidence, computed_at)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `).run(userId, results.battery?.battery, results.fatigue?.fatigue_index, results.sleep?.sleep_score, results.cardio?.cardio_index, results.battery?.confidence || 0.5, now);

  await updateHealthMetrics(userId, results, result.lastInsertRowid as number, now);
  return result.lastInsertRowid as number;
}

async function updateHealthMetrics(userId: string, results: any, runId: number, now: string): Promise<void> {
  const validUntil = new Date(Date.now() + 5 * 60 * 1000).toISOString();

  if (results.battery) {
    db.prepare('INSERT INTO health_metrics (user_id, metric_type, value, computed_at, valid_until, source, algorithm_run_id) VALUES (?, "battery", ?, ?, ?, "algorithm", ?)')
      .run(userId, JSON.stringify({ current: results.battery.battery, raw: results.battery.raw_battery, decay: results.battery.decay_factor }), now, validUntil, runId);
  }

  if (results.fatigue) {
    db.prepare('INSERT INTO health_metrics (user_id, metric_type, value, computed_at, valid_until, source, algorithm_run_id) VALUES (?, "fatigue", ?, ?, ?, "algorithm", ?)')
      .run(userId, JSON.stringify({ level: results.fatigue.fatigue_index, fatigue_level: results.fatigue.level }), now, validUntil, runId);
  }

  if (results.sleep) {
    db.prepare('INSERT INTO health_metrics (user_id, metric_type, value, computed_at, valid_until, source, algorithm_run_id) VALUES (?, "sleep", ?, ?, ?, "algorithm", ?)')
      .run(userId, JSON.stringify({ score: results.sleep.sleep_score, detail: results.sleep.detail }), now, validUntil, runId);
  }

  if (results.cardio) {
    db.prepare('INSERT INTO health_metrics (user_id, metric_type, value, computed_at, valid_until, source, algorithm_run_id) VALUES (?, "cardio", ?, ?, ?, "algorithm", ?)')
      .run(userId, JSON.stringify({ index: results.cardio.cardio_index, detail: results.cardio.detail }), now, validUntil, runId);
  }
}

export default metricsRouter;
