# VitaSleep V2

**版本**: V2.0  
**状态**: 前后端完成，对接已有算法服务

---

## 🚀 快速开始

### 1. 启动算法服务
```bash
cd /home/admin/Desktop/Vita/算法
pip install -r requirements.txt
uvicorn main:app --reload --port 8001
```

### 2. 启动后端
```bash
cd backend
npm install
npm run dev
```

### 3. 启动前端
```bash
cd frontend
npm install
npm run dev
```

### 4. 访问
- 前端：http://localhost:5173
- 后端：http://localhost:3000
- 算法：http://localhost:8001/docs

---

## 📊 数据流

```
Google Health API → 后端 → 算法服务 (8001) → 数据库 → 前端
```

---

## 🔌 核心接口

### 算法服务（已有）
- `POST /calculate/battery` - 电量
- `POST /calculate/fatigue` - 疲劳
- `POST /calculate/sleep` - 睡眠
- `POST /calculate/cardio` - 心血管

### 后端 API（V2 新增）
- `POST /api/metrics/update/:userId` - 触发指标更新
- `GET /api/metrics/:userId` - 获取最新指标

---

## 📁 项目结构

```
vitasleep-v2/
├── backend/
│   └── src/
│       ├── services/
│       │   ├── googleHealthClient.ts
│       │   ├── dataAggregator.ts
│       │   └── algorithmClient.ts
│       ├── routes/
│       │   └── metrics.ts
│       └── db/
│           └── init.ts
└── frontend/
```

---

## 🧪 测试

```bash
# 测试算法服务
curl -X POST http://localhost:8001/calculate/battery \
  -H "Content-Type: application/json" \
  -d '{"user_id":"test","hours_since_last_sleep":8,"metrics":{"resting_hr":62,"hrv_rmssd":45}}'

# 测试后端 API（需要 Google Token）
curl -X POST http://localhost:3000/api/metrics/update/test \
  -d '{"googleAccessToken":"ya29.xxx"}'
```

---

**创建时间**: 2026-05-17  
**文档**: `PROJECT_PLAN.md`

Rebuild v1.0.6
