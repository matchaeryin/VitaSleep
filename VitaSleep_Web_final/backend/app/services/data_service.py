"""
VitaSleep Agent V1.0 — 数据接入服务
接收算法处理后的指标数据，写入 health_metrics 表，并触发 SSE 实时推送。
"""

import logging
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.models.models import HealthMetric, MetricType
from app.services.sse_service import sse_manager

logger = logging.getLogger("vitasleep.data_service")

METRIC_VALIDITY: Dict[str, int] = {
    "battery": 3600,
    "blood_pressure": 1800,
    "cardio_index": 86400,
    "sleep_stage": 1800,
    "heart_rate": 300,
    "hrv": 600,
}

async def ingest_metric(
    db: AsyncSession,
    user_id: str,
    metric_type: MetricType,
    value: Any,
    computed_at: Optional[datetime] = None,
    valid_until: Optional[datetime] = None,
) -> HealthMetric:
    now = computed_at or datetime.utcnow()
    if valid_until is None:
        validity_seconds = METRIC_VALIDITY.get(metric_type.value, 3600)
        valid_until = now + timedelta(seconds=validity_seconds)

    metric = HealthMetric(
        user_id=user_id,
        metric_type=metric_type,
        value=value,
        computed_at=now,
        valid_until=valid_until,
    )
    db.add(metric)
    await db.commit()
    await db.refresh(metric)

    logger.info(f"写入指标: user_id={user_id}, type={metric_type.value}, id={metric.id}")

    await sse_manager.push_to_user(user_id, "metric_update", {
        "metric_id": metric.id,
        "metric_type": metric_type.value,
        "value": value,
        "computed_at": now.isoformat(),
    })

    return metric

async def get_latest_metrics(
    db: AsyncSession,
    user_id: str,
    metric_type: Optional[MetricType] = None,
    hours: int = 24,
    limit: int = 100,
) -> List[HealthMetric]:
    since = datetime.utcnow() - timedelta(hours=hours)
    stmt = select(HealthMetric).where(
        HealthMetric.user_id == user_id,
        HealthMetric.computed_at >= since,
    )
    if metric_type:
        stmt = stmt.where(HealthMetric.metric_type == metric_type)
    stmt = stmt.order_by(HealthMetric.computed_at.desc()).limit(limit)

    result = await db.execute(stmt)
    return list(result.scalars().all())

async def get_metrics_range(
    db: AsyncSession,
    user_id: str,
    start_time: datetime,
    end_time: datetime,
    metric_type: Optional[MetricType] = None,
    limit: int = 500,
) -> List[HealthMetric]:
    stmt = select(HealthMetric).where(
        HealthMetric.user_id == user_id,
        HealthMetric.computed_at >= start_time,
        HealthMetric.computed_at <= end_time,
    )
    if metric_type:
        stmt = stmt.where(HealthMetric.metric_type == metric_type)
    stmt = stmt.order_by(HealthMetric.computed_at.asc()).limit(limit)

    result = await db.execute(stmt)
    return list(result.scalars().all())
