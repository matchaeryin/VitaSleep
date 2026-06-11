"""
VitaSleep Agent V1.0 — FastAPI 应用入口
负责应用初始化、CORS 配置、路由注册。
"""

import logging
import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.database import init_db
from app.schemas import HealthCheckResponse
from app.api import health_api, schedule_api, chat_api, profile_api, data_ingestion, llm_config_api, agent_tools_api, veepoo_api

# ──────────────── 日志配置 ────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("vitasleep")


# ──────────────── 应用生命周期 ────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用启动和关闭时的生命周期管理"""
    # 启动时初始化数据库
    logger.info("VitaSleep Agent V1.0 启动中...")
    await init_db()
    logger.info("数据库初始化完成")

    # 启动数据模拟器（当 USE_VEEPOO_DATA=true 时禁用，改为接收真实设备数据）
    use_veepoo = os.getenv("USE_VEEPOO_DATA", "false").lower() in ("true", "1", "yes")
    if use_veepoo:
        logger.info("Veepoo 设备数据模式已启用，禁用模拟器")
    else:
        from app.services.data_simulator import start_simulator, stop_simulator
        start_simulator()
        logger.info("数据模拟器已启动（每5秒生成一次）")

    yield

    # 关闭时清理资源
    if not use_veepoo:
        from app.services.data_simulator import stop_simulator
        stop_simulator()
    logger.info("VitaSleep Agent 关闭")


# ──────────────── 创建 FastAPI 应用 ────────────────

app = FastAPI(
    title="VitaSleep Agent API",
    description="VitaSleep 智能健康管家后端服务",
    version="1.0.0",
    lifespan=lifespan,
)


# ──────────────── CORS 配置 ────────────────

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",      # 前端开发服务器
        "http://127.0.0.1:5173",     # 前端开发服务器（IP 形式）
        "http://localhost:3000",      # 备选端口
        "http://127.0.0.1:3000",
        "http://localhost:8888",      # nginx 本地
        "http://127.0.0.1:8888",
        # Android App（调试时允许）
        "http://10.0.2.2:8000",      # Android 模拟器访问宿主机
        "http://localhost:8000",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ──────────────── 路由注册 ────────────────

app.include_router(health_api.router)
app.include_router(schedule_api.router)
app.include_router(chat_api.router)
app.include_router(profile_api.router)
app.include_router(data_ingestion.router)
app.include_router(llm_config_api.router)
app.include_router(agent_tools_api.router)
app.include_router(veepoo_api.router)  # Veepoo 设备数据接入


# ──────────────── 健康检查 ────────────────

@app.get(
    "/api/health-check",
    response_model=HealthCheckResponse,
    summary="服务健康检查",
    tags=["系统"],
)
async def health_check():
    """
    服务健康检查接口。
    - 返回服务状态、版本号和当前时间。
    """
    from datetime import datetime
    return HealthCheckResponse(
        status="ok",
        version="1.0.0",
        timestamp=datetime.utcnow(),
    )
