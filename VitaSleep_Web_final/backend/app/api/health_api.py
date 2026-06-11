"""
VitaSleep Agent V1.0 — 健康指标 API
提供健康数据的查询接口。
"""

import logging
from datetime import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.models import MetricType
from app.services.data_service import get_metrics_range

logger = logging.getLogger("vitasleep.api.health")
router = APIRouter(prefix="/api/health", tags=["健康指标"])

@router.get("/metrics")
async def get_metrics(
    user_id: str = Query(...),
    metric_type: Optional[MetricType] = Query(None),
    start_time: Optional[datetime] = Query(None),
    end_time: Optional[datetime] = Query(None),
    limit: int = Query(100),
    db: AsyncSession = Depends(get_db),
):
    if not start_time:
        start_time = datetime.utcnow().replace(hour=0, minute=0, second=0)
    if not end_time:
        end_time = datetime.utcnow()

    metrics = await get_metrics_range(db, user_id, start_time, end_time, metric_type, limit)
    return metrics

@router.get("/battery")
async def get_battery(
    user_id: str = Query(...),
    hours: int = Query(24),
    limit: int = Query(100),
    db: AsyncSession = Depends(get_db),
):
    from datetime import timedelta
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=hours)
    metrics = await get_metrics_range(db, user_id, start_time, end_time, MetricType.battery, limit)
    return metrics

@router.get("/sleep")
async def get_sleep(
    user_id: str = Query(...),
    days: int = Query(7),
    limit: int = Query(200),
    db: AsyncSession = Depends(get_db),
):
    from datetime import timedelta
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(days=days)
    metrics = await get_metrics_range(db, user_id, start_time, end_time, MetricType.sleep_stage, limit)
    return metrics
