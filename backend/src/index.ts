import express from 'express';
import cors from 'cors';
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

// 健康检查
app.get('/health', (req, res) => {
  // 同时检查算法服务是否在线
  import('axios').then(({ default: axios }) => {
    axios.get(`${process.env.ALGORITHM_URL || 'http://localhost:8001'}/docs`, { timeout: 3000 })
      .then(() => res.json({ status: 'ok', algorithm_service: 'online' }))
      .catch(() => res.json({ status: 'ok', algorithm_service: 'offline' }));
  });
});

// 指标路由
app.use('/api/metrics', metricsRouter);

// 错误处理
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(500).json({ success: false, error: err.message });
});

app.listen(PORT, () => {
  console.log('');
  console.log('╔════════════════════════════════════════════════╗');
  console.log('║  🚀 VitaSleep V2 Backend                       ║');
  console.log(`║  📍 Port: ${PORT}                                ║`);
  console.log(`║  🔗 API: http://localhost:${PORT}/api           ║`);
  console.log('║  🤖 Algorithm: http://localhost:8001           ║');
  console.log('╚════════════════════════════════════════════════╝');
  console.log('');
  console.log('📡 测试接口:');
  console.log(`  POST http://localhost:${PORT}/api/metrics/update/test_user`);
  console.log(`  Body: {"useMock": true}`);
  console.log('');
});

process.on('SIGINT', () => {
  console.log('\n正在关闭...');
  db.close();
  process.exit(0);
});
