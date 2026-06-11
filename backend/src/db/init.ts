import Database, { type Database as DatabaseType } from 'better-sqlite3';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DB_PATH = path.join(__dirname, '../../data/vitasleep.db');

export function initDatabase(): DatabaseType {
  const db = new Database(DB_PATH);
  db.pragma('foreign_keys = ON');
  
  // ?????
  db.exec(`
    CREATE TABLE IF NOT EXISTS health_metrics (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id VARCHAR(64) NOT NULL,
      metric_type VARCHAR(32) NOT NULL,
      value JSON NOT NULL,
      computed_at DATETIME NOT NULL,
      valid_until DATETIME NOT NULL,
      source VARCHAR(32) DEFAULT 'google_health',
      algorithm_run_id INTEGER
    )
  `);
  
  // ???
  db.exec(`
    CREATE TABLE IF NOT EXISTS schedules (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id VARCHAR(64) NOT NULL,
      title VARCHAR(255) NOT NULL,
      event_type VARCHAR(32) NOT NULL DEFAULT 'fixed',
      start_time DATETIME NOT NULL,
      end_time DATETIME NOT NULL,
      source VARCHAR(32) NOT NULL DEFAULT 'user_manual',
      status VARCHAR(32) NOT NULL DEFAULT 'pending',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
  
  // ???????(V2 ??)
  db.exec(`
    CREATE TABLE IF NOT EXISTS algorithm_runs (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id VARCHAR(64) NOT NULL,
      battery_level REAL,
      fatigue_level REAL,
      sleep_score REAL,
      cardio_index REAL,
      confidence REAL,
      computed_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
  
  // ?????
  db.exec(`
    CREATE TABLE IF NOT EXISTS chat_history (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id VARCHAR(64) NOT NULL,
      role VARCHAR(32) NOT NULL,
      content TEXT NOT NULL,
      agent_type VARCHAR(32),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
  
  // ??
  db.exec(`
    CREATE INDEX IF NOT EXISTS idx_health_metrics_user ON health_metrics(user_id, metric_type, computed_at);
    CREATE INDEX IF NOT EXISTS idx_schedules_user ON schedules(user_id, start_time);
    CREATE INDEX IF NOT EXISTS idx_algorithm_runs_user ON algorithm_runs(user_id, computed_at);
    CREATE INDEX IF NOT EXISTS idx_chat_history_user ON chat_history(user_id, created_at);
  `);
  
  console.log('? Database initialized');
  return db;
}

export const db: DatabaseType = initDatabase();
