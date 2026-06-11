import express from 'express';
import cors from 'cors';
import { metricsRouter } from './routes/metrics.js';
import { healthRouter } from './routes/health.js';
import { chatRouter } from './routes/chat.js';
import { schedulesRouter } from './routes/schedules.js';
import { veepooRouter } from './routes/veepoo.js';
import { db } from './db/init.js';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

app.get('/health', (req, res) => {
  import('axios').then(({ default: axios }) => {
    axios.get(`${process.env.ALGORITHM_URL || 'http://localhost:8001'}/docs`, { timeout: 3000 })
      .then(() => res.json({ status: 'ok', algorithm_service: 'online' }))
      .catch(() => res.json({ status: 'ok', algorithm_service: 'offline' }));
  });
});

app.use('/api/metrics', metricsRouter);
app.use('/api/health', healthRouter);
app.use('/api/chat', chatRouter);
app.use('/api/schedules', schedulesRouter);
app.use('/api/data/veepoo', veepooRouter);

app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(500).json({ success: false, error: err.message });
});

app.listen(PORT, () => {
  console.log('');
  console.log('\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557');
  console.log('\u2551  \ud83d\ude80 VitaSleep V2 Backend                       \u2551');
  console.log(`\u2551  \ud83d\udccd Port: ${PORT}                                \u2551`);
  console.log(`\u2551  \ud83d\udd17 API: http://localhost:${PORT}/api           \u2551`);
  console.log('\u2551  \ud83e\udd16 Algorithm: http://localhost:8001           \u2551');
  console.log('\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d');
  console.log('');
  console.log('\ud83d\udce1 \u6d4b\u8bd5\u63a5\u53e3:');
  console.log(`  POST http://localhost:${PORT}/api/metrics/update/test_user`);
  console.log(`  Body: {"useMock": true}`);
  console.log('');
});

process.on('SIGINT', () => {
  console.log('\n\u6b63\u5728\u5173\u95ed...');
  db.close();
  process.exit(0);
});
