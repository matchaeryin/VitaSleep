# VitaSleep V2 开发计划

**创建时间**: 2026-05-17  
**版本**: V2.0  
**目标**: 集成 Google Health API + 对接已有算法服务

---

## 📋 V2 核心变更

### 数据流
```
Google Health API → 后端 (拉取/转换) → 算法服务 (已有) → 数据库 → 前端
```

### 职责划分
| 模块 | 状态 | 说明 |
|------|------|------|
| 前端 | ✅ 完成 | React + TypeScript |
| 后端 | ✅ 完成 | Node.js + TypeScript |
| 算法服务 | ✅ 已存在 | `/home/admin/Desktop/Vita/算法/` |

---

## 🎯 开发任务

### Phase 1: Google Health API 集成 ✅
- [x] 创建 Google Health 客户端
- [x] 实现 OAuth2 认证
- [x] 拉取心率/睡眠/步数数据
- [x] 数据格式转换

### Phase 2: 数据聚合服务 ✅
- [x] 多源数据聚合
- [x] 标准化数据格式 (MetricPayload)
- [x] 时间序列对齐

### Phase 3: 算法服务对接 ✅
- [x] 对接已有算法接口
- [x] 创建 HTTP 客户端
- [x] 实现重试机制
- [x] 添加健康检查

### Phase 4: 数据库更新 ✅
- [x] 新增算法运行记录表
- [x] 添加数据源标识
- [x] 创建定时任务

### Phase 5: 前端更新 ⏳
- [ ] 添加 Google 登录
- [ ] 数据源标识显示
- [ ] 算法置信度展示

---

## 📁 项目结构

```
vitasleep-v2/
├── frontend/              # 前端（React + TS）
│   └── src/
│       ├── components/
│       ├── pages/
│       ├── api/
│       └── store/
│
├── backend/               # 后端（Node.js + TS）
│   └── src/
│       ├── services/
│       │   ├── googleHealthClient.ts    ← 新增
│       │   ├── dataAggregator.ts        ← 新增
│       │   └── algorithmClient.ts       ← 新增
│       ├── routes/
│       │   ├── metrics.ts               ← 新增
│       │   ├── health.ts
│       │   ├── schedule.ts
│       │   └── chat.ts
│       └── db/
│           └── init.ts
│
└── docs/
    ├── ALGORITHM_API.md   # 算法接口文档（已有）
    └── GOOGLE_HEALTH.md   # Google 集成文档
```

---

## 🔌 算法服务接口

### 已有算法服务位置
```
/home/admin/Desktop/Vita/算法/
├── main.py                      # FastAPI 入口
├── battery.py                   # 电量计算
├── fatigue.py                   # 疲劳计算
├── sleep_scorer.py             # 睡眠评分
├── cardio.py                    # 心血管指数
└── VitaSleep_算法服务对接文档.md  # 接口文档
```

### 启动方式
```bash
cd /home/admin/Desktop/Vita/算法
uvicorn main:app --reload --port 8001
```

### 核心接口
| 接口 | 方法 | 功能 |
|------|------|------|
| `/calculate/battery` | POST | 机体电量 |
| `/calculate/fatigue` | POST | 疲劳评估 |
| `/calculate/sleep` | POST | 睡眠质量 |
| `/calculate/cardio` | POST | 心血管指数 |

---

## 📊 MetricPayload 格式

```typescript
interface MetricPayload {
  // 心率 / HRV
  resting_hr?: number | null;
  hrv_rmssd?: number | null;
  hrv_sdnn?: number | null;
  
  // 血压
  systolic?: number | null;
  diastolic?: number | null;
  
  // 血氧
  spo2?: number | null;
  
  // 睡眠分期（分钟）
  deep_sleep_minutes?: number | null;
  light_sleep_minutes?: number | null;
  rem_sleep_minutes?: number | null;
  awake_minutes?: number | null;
  
  // 活动
  steps?: number | null;
  active_calories?: number | null;
  active_minutes?: number | null;
}
```

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
cd vitasleep-v2/backend
npm install
npm run dev
```

### 3. 启动前端
```bash
cd vitasleep-v2/frontend
npm install
npm run dev
```

### 4. 访问
- 前端：http://localhost:5173
- 后端：http://localhost:3000
- 算法：http://localhost:8001

---

## 📅 开发排期

| 阶段 | 时间 | 状态 | 负责人 |
|------|------|------|--------|
| Phase 1 | Day 1-2 | ✅ 完成 | 你 |
| Phase 2 | Day 3-4 | ✅ 完成 | 你 |
| Phase 3 | Day 5 | ✅ 完成 | 你 |
| Phase 4 | Day 6 | ✅ 完成 | 你 |
| Phase 5 | Day 7-8 | ⏳ 进行中 | 你 |
| 联调测试 | Day 9-10 | ⏳ 待开始 | 全体 |
| 上线部署 | Day 11-12 | ⏳ 待开始 | 全体 |

---

**最后更新**: 2026-05-17  
**状态**: 开发中
