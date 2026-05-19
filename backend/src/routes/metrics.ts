/**
 * 指标路由 - 对接已有算法服务
 * 支持模拟数据模式（无需 Google Health Token）
 */

import { Router } from 'express';
import { db } from '../db/init.js';
import { AlgorithmClient, MetricPayload } from '../services/algorithmClient.js';

export const metricsRouter = Router();
const algorithmClient = new AlgorithmClient(process.env.ALGORITHM_URL || 'http://localhost:8001');

// 模拟数据：模拟 Google Health API 返回的典型数据
function generateMockMetrics(): { metrics: MetricPayload; hoursSinceLastSleep: number } {
  const rand = (min: number, max: number) => Math.round((Math.random() * (max - min) + min) * 10) / 10;

  return {
    hoursSinceLastSleep: rand(4, 12),
    metrics: {
      resting_hr: rand(55, 75),
      hrv_rmssd: rand(25, 80),
      hrv_sdnn: rand(30, 100),
      systolic: rand(100, 135),
      diastolic: rand(60, 90),
      spo2: rand(94, 99),
      deep_sleep_minutes: rand(30, 120),
      light_sleep_minutes: rand(100, 300),
      rem_sleep_minutes: rand(40, 150),
      awake_minutes: rand(5, 45),
      steps: rand(2000, 15000),
      active_calories: rand(100, 600),
      active_minutes: rand(10, 90),
    },
  };
}

// POST /api/metrics/update/:userId — 触发指标计算
metricsRouter.post('/update/:userId', async (req, res) => {
  const { userId } = req.params;
  const { googleAccessToken, useMock } = req.body;

  try {
    let metrics: MetricPayload;
    let hoursSinceLastSleep: number | null;

    // 如果没有 Google Token 或显式要求使用模拟数据，则用模拟数据
    if (!googleAccessToken || useMock) {
      console.log(`[Metrics] 使用模拟数据 (userId=${userId})`);
      const mock = generateMockMetrics();
      metrics = mock.metrics;
      hoursSinceLastSleep = mock.hoursSinceLastSleep;
    } else {
      // 真实模式：需要 Google Health Token
      return res.status(501).json({
        success: false,
        error: 'Google Health API 集成待实现，请使用 useMock: true',
      });
    }

    console.log(`[Metrics] 输入数据:`, JSON.stringify({ hoursSinceLastSleep, metrics }, null, 2));

    // 并行调用 4 个算法接口
    const [battery, fatigue, sleep, cardio] = await Promise.all([
      algorithmClient.calculateBattery({
        user_id: userId,
        hours_since_last_sleep: hoursSinceLastSleep,
        metrics,
      }),
      algorithmClient.calculateFatigue({
        user_id: userId,
        hours_since_last_sleep: hoursSinceLastSleep,
        metrics,
      }),
      algorithmClient.calculateSleep(userId, metrics),
      algorithmClient.calculateCardio(userId, metrics),
    ]);

    console.log(`[Metrics] 算法结果: battery=${battery.battery}, fatigue=${fatigue.fatigue_index}, sleep=${sleep.sleep_score}, cardio=${cardio.cardio_index}`);

    // 存储结果到数据库
    const runId = storeResults(userId, { battery, fatigue, sleep, cardio });

    res.json({
      success: true,
      data: {
        input: { hours_since_last_sleep: hoursSinceLastSleep, metrics },
        battery,
        fatigue,
        sleep,
        cardio,
        run_id: runId,
      },
    });
  } catch (error: any) {
    console.error('[Metrics] 更新失败:', error.message);
    if (error.response) {
      console.error('[Metrics] 算法服务响应:', error.response.status, error.response.data);
    }
    res.status(500).json({ success: false, error: error.message });
  }
});

// GET /api/metrics/:userId — 获取最新指标
metricsRouter.get('/:userId', (req, res) => {
  const { userId } = req.params;
  const latest = db
    .prepare('SELECT * FROM algorithm_runs WHERE user_id = ? ORDER BY computed_at DESC LIMIT 1')
    .get(userId) as any;

  if (!latest) {
    return res.json({ success: true, data: null, message: '暂无数据，请先调用 POST /api/metrics/update/:userId 触发计算' });
  }

  // 获取关联的指标详情
  const metricDetails = db
    .prepare('SELECT * FROM health_metrics WHERE user_id = ? AND algorithm_run_id = ? ORDER BY metric_type')
    .all(userId, latest.id) as any[];

  res.json({
    success: true,
    data: {
      summary: {
        battery_level: latest.battery_level,
        fatigue_level: latest.fatigue_level,
        sleep_score: latest.sleep_score,
        cardio_index: latest.cardio_index,
        confidence: latest.confidence,
        computed_at: latest.computed_at,
      },
      details: metricDetails.map((m: any) => ({
        type: m.metric_type,
        value: JSON.parse(m.value),
        source: m.source,
        computed_at: m.computed_at,
      })),
    },
  });
});

// GET /api/metrics/:userId/history — 获取历史记录
metricsRouter.get('/:userId/history', (req, res) => {
  const { userId } = req.params;
  const limit = parseInt(req.query.limit as string) || 10;

  const history = db
    .prepare('SELECT * FROM algorithm_runs WHERE user_id = ? ORDER BY computed_at DESC LIMIT ?')
    .all(userId, limit) as any[];

  res.json({
    success: true,
    data: history.map((h: any) => ({
      id: h.id,
      battery_level: h.battery_level,
      fatigue_level: h.fatigue_level,
      sleep_score: h.sleep_score,
      cardio_index: h.cardio_index,
      confidence: h.confidence,
      computed_at: h.computed_at,
    })),
  });
});

// 存储结果到数据库
function storeResults(userId: string, results: any): number {
  const now = new Date().toISOString();

  const result = db
    .prepare(
      `INSERT INTO algorithm_runs (user_id, battery_level, fatigue_level, sleep_score, cardio_index, confidence, computed_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)`
    )
    .run(
      userId,
      results.battery?.battery ?? null,
      results.fatigue?.fatigue_index ?? null,
      results.sleep?.sleep_score ?? null,
      results.cardio?.cardio_index ?? null,
      results.battery?.confidence ?? 0.5,
      now
    );

  const runId = result.lastInsertRowid as number;
  const validUntil = new Date(Date.now() + 5 * 60 * 1000).toISOString();

  // 存储各指标详情
  const insertMetric = db.prepare(
    `INSERT INTO health_metrics (user_id, metric_type, value, computed_at, valid_until, source, algorithm_run_id)
     VALUES (?, ?, ?, ?, ?, 'algorithm', ?)`
  );

  if (results.battery) {
    insertMetric.run(userId, 'battery', JSON.stringify(results.battery), now, validUntil, runId);
  }
  if (results.fatigue) {
    insertMetric.run(userId, 'fatigue', JSON.stringify(results.fatigue), now, validUntil, runId);
  }
  if (results.sleep) {
    insertMetric.run(userId, 'sleep', JSON.stringify(results.sleep), now, validUntil, runId);
  }
  if (results.cardio) {
    insertMetric.run(userId, 'cardio', JSON.stringify(results.cardio), now, validUntil, runId);
  }

  console.log(`[Metrics] 已存储到数据库 (runId=${runId})`);
  return runId;
}

export default metricsRouter;
