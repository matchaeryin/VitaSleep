"""
VitaSleep Agent V1.2 — Agent 工具接口
提供大模型可调用的工具 API：
  - query_health: 读取用户健康数据
  - query_schedule: 读取用户日程
  - modify_schedule: 创建/修改/删除日程
  - create_schedule: 创建新日程
  - delete_schedule: 删除日程
"""

import logging
from datetime import datetime, timedelta
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.database import get_db
from app.models.models import (
    HealthMetric, MetricType, Schedule, EventType,
    ScheduleSource, ScheduleStatus,
)
from app.schemas import (
    AgentToolCall, AgentToolResult,
    ScheduleCreate, ScheduleUpdate, ScheduleOut, ErrorResponse,
)

logger = logging.getLogger("vitasleep.api.agent_tools")

router = APIRouter(prefix="/api/agent", tags=["Agent 工具"])


@router.post(
    "/query-health",
    summary="Agent 查询健康数据",
    description="大模型调用此接口读取用户的健康数据（身体电量、血压、睡眠、心率等）。",
)
async def agent_query_health(
    user_id: str,
    metric_type: Optional[str] = None,
    hours: int = 24,
    limit: int = 100,
    db: AsyncSession = Depends(get_db),
):
    stmt = select(HealthMetric).where(HealthMetric.user_id == user_id)

    if metric_type:
        try:
            mt = MetricType(metric_type)
            stmt = stmt.where(HealthMetric.metric_type == mt)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"不支持的指标类型: {metric_type}")

    start_time = datetime.utcnow() - timedelta(hours=hours)
    stmt = stmt.where(HealthMetric.computed_at >= start_time)
    stmt = stmt.order_by(HealthMetric.computed_at.desc()).limit(limit)

    result = await db.execute(stmt)
    metrics = list(result.scalars().all())

    return {
        "user_id": user_id,
        "count": len(metrics),
        "hours": hours,
        "data": [
            {
                "id": m.id,
                "metric_type": m.metric_type.value if hasattr(m.metric_type, "value") else m.metric_type,
                "value": m.value,
                "computed_at": m.computed_at.isoformat() if m.computed_at else None,
            }
            for m in metrics
        ],
    }


@router.post(
    "/query-schedule",
    summary="Agent 查询日程",
    description="大模型调用此接口读取用户的日程安排。",
)
async def agent_query_schedule(
    user_id: str,
    days: int = 7,
    status: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
):
    now = datetime.utcnow()
    end_time = now + timedelta(days=days)

    stmt = (
        select(Schedule)
        .where(Schedule.user_id == user_id)
        .where(Schedule.start_time >= now)
        .where(Schedule.start_time < end_time)
        .order_by(Schedule.start_time.asc())
        .limit(100)
    )

    if status:
        try:
            ss = ScheduleStatus(status)
            stmt = stmt.where(Schedule.status == ss)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"不支持的状态: {status}")

    result = await db.execute(stmt)
    schedules = list(result.scalars().all())

    return {
        "user_id": user_id,
        "count": len(schedules),
        "days": days,
        "data": [
            {
                "id": s.id,
                "title": s.title,
                "event_type": s.event_type.value if hasattr(s.event_type, "value") else s.event_type,
                "start_time": s.start_time.isoformat() if s.start_time else None,
                "end_time": s.end_time.isoformat() if s.end_time else None,
                "source": s.source.value if hasattr(s.source, "value") else s.source,
                "status": s.status.value if hasattr(s.status, "value") else s.status,
            }
            for s in schedules
        ],
    }


@router.post(
    "/create-schedule",
    response_model=ScheduleOut,
    summary="Agent 创建日程",
    description="大模型调用此接口为用户创建新日程。来源自动标记为 agent_generated。",
    status_code=201,
)
async def agent_create_schedule(
    schedule: ScheduleCreate,
    db: AsyncSession = Depends(get_db),
):
    new_schedule = Schedule(
        user_id=schedule.user_id,
        title=schedule.title,
        event_type=schedule.event_type,
        start_time=schedule.start_time,
        end_time=schedule.end_time,
        source=ScheduleSource.agent_generated,
        status=ScheduleStatus.pending,
    )
    db.add(new_schedule)
    await db.commit()
    await db.refresh(new_schedule)
    logger.info(f"Agent 创建日程: {new_schedule.title} (id={new_schedule.id})")
    return new_schedule


@router.put(
    "/modify-schedule/{schedule_id}",
    response_model=ScheduleOut,
    summary="Agent 修改日程",
    description="大模型调用此接口修改用户日程。",
    responses={404: {"model": ErrorResponse}},
)
async def agent_modify_schedule(
    schedule_id: int,
    update: ScheduleUpdate,
    db: AsyncSession = Depends(get_db),
):
    stmt = select(Schedule).where(Schedule.id == schedule_id)
    result = await db.execute(stmt)
    schedule = result.scalar_one_or_none()
    if not schedule:
        raise HTTPException(status_code=404, detail="日程不存在")

    update_data = update.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(schedule, key, value)

    await db.commit()
    await db.refresh(schedule)
    logger.info(f"Agent 修改日程: {schedule.title} (id={schedule.id})")
    return schedule


@router.delete(
    "/delete-schedule/{schedule_id}",
    summary="Agent 删除日程",
    description="大模型调用此接口删除用户日程。",
)
async def agent_delete_schedule(
    schedule_id: int,
    db: AsyncSession = Depends(get_db),
):
    stmt = select(Schedule).where(Schedule.id == schedule_id)
    result = await db.execute(stmt)
    schedule = result.scalar_one_or_none()
    if not schedule:
        raise HTTPException(status_code=404, detail="日程不存在")

    title = schedule.title
    await db.delete(schedule)
    await db.commit()
    logger.info(f"Agent 删除日程: {title} (id={schedule_id})")
    return {"status": "ok", "message": f"日程 '{title}' 已删除"}
