import { Router } from 'express';
import { db } from '../db/init.js';

export const schedulesRouter = Router();

schedulesRouter.get('/', (req, res) => {
  const userId = (req.query.user_id as string) || 'test_user_001';
  const startTime = req.query.start_time as string | undefined;
  const endTime = req.query.end_time as string | undefined;

  try {
    let rows: any[];
    if (startTime && endTime) {
      rows = db.prepare(
        'SELECT * FROM schedules WHERE user_id = ? AND start_time >= ? AND end_time <= ? ORDER BY start_time ASC'
      ).all(userId, startTime, endTime) as any[];
    } else {
      rows = db.prepare(
        'SELECT * FROM schedules WHERE user_id = ? ORDER BY start_time ASC'
      ).all(userId) as any[];
    }

    const result = rows.map(row => ({
      id: row.id,
      user_id: row.user_id,
      title: row.title,
      event_type: row.event_type,
      start_time: row.start_time,
      end_time: row.end_time,
      source: row.source,
      status: row.status,
      created_at: row.created_at,
      updated_at: row.updated_at,
    }));

    res.json(result);
  } catch (error: any) {
    console.error('[Schedules] get error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

schedulesRouter.post('/', (req, res) => {
  const { user_id, title, event_type, start_time, end_time } = req.body;
  const userId = user_id || 'test_user_001';
  const now = new Date().toISOString();

  try {
    const result = db.prepare(
      'INSERT INTO schedules (user_id, title, event_type, start_time, end_time, source, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
    ).run(userId, title, event_type || 'flexible', start_time, end_time, 'user_manual', 'pending', now, now);

    res.json({
      id: Number(result.lastInsertRowid),
      user_id: userId,
      title,
      event_type: event_type || 'flexible',
      start_time,
      end_time,
      source: 'user_manual',
      status: 'pending',
      created_at: now,
      updated_at: now,
    });
  } catch (error: any) {
    console.error('[Schedules] create error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

schedulesRouter.delete('/:id', (req, res) => {
  const { id } = req.params;

  try {
    db.prepare('DELETE FROM schedules WHERE id = ?').run(id);
    res.sendStatus(204);
  } catch (error: any) {
    console.error('[Schedules] delete error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});
