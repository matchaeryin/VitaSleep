import { Router } from 'express';
import { db } from '../db/init.js';

export const veepooRouter = Router();

veepooRouter.post('/origin5min', (req, res) => {
  const { user_id, records } = req.body;
  const userId = user_id || 'test_user_001';
  const now = new Date().toISOString();

  try {
    const metricIds: number[] = [];
    const errors: string[] = [];

    for (const record of (records || [])) {
      try {
        const value = {
          heart_rate: record.heart_rate || record.heartRate,
          heart_rate_array: record.heart_rate_array || record.heartRateArray,
          systolic: record.systolic,
          diastolic: record.diastolic,
          steps: record.steps || 0,
          spo2: record.spo2,
        };

        const result = db.prepare(
          'INSERT INTO health_metrics (user_id, metric_type, value, computed_at, valid_until, source) VALUES (?, ?, ?, ?, ?, ?)'
        ).run(userId, 'origin_5min', JSON.stringify(value), record.timestamp || now, now, 'veepoo');

        metricIds.push(Number(result.lastInsertRowid));
      } catch (e: any) {
        errors.push(e.message);
      }
    }

    res.json({
      status: 'ok',
      message: `Processed ${metricIds.length} records`,
      records_processed: metricIds.length,
      metric_ids: metricIds,
      errors,
    });
  } catch (error: any) {
    console.error('[Veepoo] origin5min error:', error);
    res.status(500).json({
      status: 'error',
      message: error.message,
      records_processed: 0,
      metric_ids: [],
      errors: [error.message],
    });
  }
});

veepooRouter.post('/sleep', (req, res) => {
  const body = req.body;
  const userId = body.user_id || 'test_user_001';
  const now = new Date().toISOString();

  try {
    const value = {
      sleep_date: body.sleep_date,
      sleep_start: body.sleep_start,
      sleep_end: body.sleep_end,
      total_sleep_min: body.total_sleep_min,
      deep_sleep_min: body.deep_sleep_min || 0,
      light_sleep_min: body.light_sleep_min || 0,
      rem_sleep_min: body.rem_sleep_min || 0,
      awake_min: body.awake_min || 0,
      deep_pct: body.deep_pct || 0,
      light_pct: body.light_pct || 0,
      rem_pct: body.rem_pct || 0,
      awake_pct: body.awake_pct || 0,
      quality_score: body.quality_score || null,
    };

    const result = db.prepare(
      'INSERT INTO health_metrics (user_id, metric_type, value, computed_at, valid_until, source) VALUES (?, ?, ?, ?, ?, ?)'
    ).run(userId, 'sleep_stage', JSON.stringify(value), now, now, 'veepoo');

    res.json({
      status: 'ok',
      message: 'Sleep data stored',
      records_processed: 1,
      metric_ids: [Number(result.lastInsertRowid)],
      errors: [],
    });
  } catch (error: any) {
    console.error('[Veepoo] sleep error:', error);
    res.status(500).json({
      status: 'error',
      message: error.message,
      records_processed: 0,
      metric_ids: [],
      errors: [error.message],
    });
  }
});

veepooRouter.post('/sync', (req, res) => {
  const { user_id, records } = req.body;
  const userId = user_id || 'test_user_001';
  const now = new Date().toISOString();

  try {
    const metricIds: number[] = [];
    const errors: string[] = [];

    for (const record of (records || [])) {
      try {
        const value = {
          heart_rate: record.heart_rate || record.heartRate,
          heart_rate_array: record.heart_rate_array || record.heartRateArray,
          systolic: record.systolic,
          diastolic: record.diastolic,
          steps: record.steps || 0,
          spo2: record.spo2,
        };

        const result = db.prepare(
          'INSERT INTO health_metrics (user_id, metric_type, value, computed_at, valid_until, source) VALUES (?, ?, ?, ?, ?, ?)'
        ).run(userId, 'origin_5min', JSON.stringify(value), record.timestamp || now, now, 'veepoo');

        metricIds.push(Number(result.lastInsertRowid));
      } catch (e: any) {
        errors.push(e.message);
      }
    }

    res.json({
      status: 'ok',
      message: `Synced ${metricIds.length} records`,
      records_processed: metricIds.length,
      metric_ids: metricIds,
      errors,
    });
  } catch (error: any) {
    console.error('[Veepoo] sync error:', error);
    res.status(500).json({
      status: 'error',
      message: error.message,
      records_processed: 0,
      metric_ids: [],
      errors: [error.message],
    });
  }
});
