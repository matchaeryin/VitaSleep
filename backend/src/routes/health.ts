import { Router } from 'express';
import { db } from '../db/init.js';

export const healthRouter = Router();

healthRouter.get('/metrics', (req, res) => {
  const userId = (req.query.user_id as string) || 'test_user_001';
  const metricType = req.query.metric_type as string | undefined;
  const limit = parseInt(req.query.limit as string) || 100;

  try {
    let rows: any[];
    if (metricType) {
      rows = db.prepare(
        'SELECT * FROM health_metrics WHERE user_id = ? AND metric_type = ? ORDER BY computed_at DESC LIMIT ?'
      ).all(userId, metricType, limit) as any[];
    } else {
      rows = db.prepare(
        'SELECT * FROM health_metrics WHERE user_id = ? ORDER BY computed_at DESC LIMIT ?'
      ).all(userId, limit) as any[];
    }

    const result = rows.map(row => ({
      id: row.id,
      user_id: row.user_id,
      metric_type: row.metric_type,
      value: JSON.parse(row.value),
      computed_at: row.computed_at,
      valid_until: row.valid_until,
    }));

    res.json(result);
  } catch (error: any) {
    console.error('[Health] metrics error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

healthRouter.get('/battery', (req, res) => {
  const userId = (req.query.user_id as string) || 'test_user_001';
  try {
    const rows = db.prepare(
      "SELECT * FROM health_metrics WHERE user_id = ? AND metric_type = 'battery' ORDER BY computed_at DESC LIMIT 10"
    ).all(userId) as any[];

    const result = rows.map(row => ({
      id: row.id,
      user_id: row.user_id,
      metric_type: row.metric_type,
      value: JSON.parse(row.value),
      computed_at: row.computed_at,
      valid_until: row.valid_until,
    }));

    res.json(result);
  } catch (error: any) {
    console.error('[Health] battery error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

healthRouter.get('/sleep', (req, res) => {
  const userId = (req.query.user_id as string) || 'test_user_001';
  const days = parseInt(req.query.days as string) || 7;
  try {
    const rows = db.prepare(
      "SELECT * FROM health_metrics WHERE user_id = ? AND metric_type = 'sleep_stage' AND computed_at >= datetime('now', '-' || ? || ' days') ORDER BY computed_at DESC LIMIT 100"
    ).all(userId, days) as any[];

    const result = rows.map(row => ({
      id: row.id,
      user_id: row.user_id,
      metric_type: row.metric_type,
      value: JSON.parse(row.value),
      computed_at: row.computed_at,
      valid_until: row.valid_until,
    }));

    res.json(result);
  } catch (error: any) {
    console.error('[Health] sleep error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});
