import express from 'express';
import cors from 'cors';
import { healthRouter } from './routes/health.js';
import { scheduleRouter } from './routes/schedule.js';
import { chatRouter } from './routes/chat.js';
import { metricsRouter } from './routes/metrics.js';
import { db } from './db/init.js';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

app.get('/health', (req, res) => res.json({ status: 'ok' }));

app.use('/api/health', healthRouter);
app.use('/api/schedules', scheduleRouter);
app.use('/api/chat', chatRouter);
app.use('/api/metrics', metricsRouter);

app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(500).json({ success: false, error: err.message });
});

app.listen(PORT, () => {
  console.log(`
╔════════════════════════════════════════════════╗
║  🚀 VitaSleep V2 Backend                       ║
║  📍 Port: ${PORT}                                ║
║  🔗 API: http://localhost:${PORT}/api           ║
║  🤖 Algorithm: http://localhost:8001           ║
╚════════════════════════════════════════════════╝
  `);
});

process.on('SIGINT', () => { db.close(); process.exit(0); });
