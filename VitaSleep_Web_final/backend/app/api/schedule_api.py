"""
VitaSleep Agent V1.0 — 日程管理 API
提供日程的 CRUD 操作接口。
"""

import logging
from datetime import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, Query, HTTPException, Body
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.database import get_db
from app.models.models import Schedule, ScheduleStatus, EventType, ScheduleSource
from app.schemas import (
    ScheduleCreate, ScheduleUpdate, ScheduleOut, ScheduleQuery, ErrorResponse,
)

logger = logging.getLogger("vitasleep.api.schedule")

router = APIRouter(prefix="/api/schedules", tags=["日程管理"])


@router.get(
    "",
    response_model=List[ScheduleOut],
    summary="获取日程列表",
    responses={400: {"model": ErrorResponse}},
)
async def list_schedules(
    user_id: str = Query(..., description="用户 ID"),
    status: Optional[ScheduleStatus] = Query(None, description="状态筛选"),
    start_time: Optional[datetime] = Query(None, description="开始时间范围（起始）"),
    end_time: Optional[datetime] = Query(None, description="结束时间范围（截止）"),
    limit: int = Query(50, ge=1, le=500, description="返回条数限制"),
    db: AsyncSession = Depends(get_db),
):
    """
    获取用户的日程列表。
    - 支持按状态筛选。
    - 支持按时间范围筛选。
    """
    stmt = select(Schedule).where(Schedule.user_id == user_id)

    if status:
        stmt = stmt.where(Schedule.status == status)
    if start_time and end_time:
        # 日程数据存储为本地时间，查询时用日期范围
        # 使用 start_time <= end_of_range AND end_time >= start_of_range（交叉查询）
        stmt = stmt.where(
            Schedule.start_time <= end_time,
            Schedule.end_time >= start_time,
        )
    if start_time:
        stmt = stmt.where(Schedule.end_time >= start_time)
    if end_time:
        stmt = stmt.where(Schedule.start_time <= end_time)

    stmt = stmt.order_by(Schedule.start_time.asc()).limit(limit)

    result = await db.execute(stmt)
    schedules = result.scalars().all()
    return schedules


@router.get("/{schedule_id}", response_model=ScheduleOut, summary="获取单个日程")
async def get_schedule(
    schedule_id: int,
    db: AsyncSession = Depends(get_db),
):
    """根据日程 ID 获取详情。"""
    result = await db.execute(
        select(Schedule).where(Schedule.id == schedule_id)
    )
    schedule = result.scalar_one_or_none()
    if not schedule:
        raise HTTPException(status_code=404, detail="日程不存在")
    return schedule


@router.post("", response_model=ScheduleOut, status_code=201, summary="创建日程")
async def create_schedule(
    schedule: ScheduleCreate,
    db: AsyncSession = Depends(get_db),
):
    """创建新日程。"""
    db_schedule = Schedule(
        user_id=schedule.user_id,
        title=schedule.title,
        description=schedule.description,
        start_time=schedule.start_time,
        end_time=schedule.end_time,
        event_type=schedule.event_type,
        location=schedule.location,
        remind_minutes=schedule.remind_minutes,
        status=ScheduleStatus.pending,
        source=ScheduleSource.app,
    )
    db.add(db_schedule)
    await db.commit()
    await db.refresh(db_schedule)
    logger.info(f"创建日程: id={db_schedule.id}, title={db_schedule.title}")
    return db_schedule


@router.put("/{schedule_id}", response_model=ScheduleOut, summary="更新日程")
async def update_schedule(
    schedule_id: int,
    schedule_update: ScheduleUpdate,
    db: AsyncSession = Depends(get_db),
):
    """更新日程信息。"""
    result = await db.execute(
        select(Schedule).where(Schedule.id == schedule_id)
    )
    db_schedule = result.scalar_one_or_none()
    if not db_schedule:
        raise HTTPException(status_code=404, detail="日程不存在")

    update_data = schedule_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_schedule, field, value)

    await db.commit()
    await db.refresh(db_schedule)
    logger.info(f"更新日程: id={db_schedule.id}")
    return db_schedule


@router.delete("/{schedule_id", status_code=204, summary="删除日程")
async def delete_schedule(
    schedule_id: int,
    db: AsyncSession = Depends(get_db),
):
    """删除指定日程。"""
    result = await db.execute(
        select(Schedule).where(Schedule.id == schedule_id)
    )
    db_schedule = result.scalar_one_or_none()
    if not db_schedule:
        raise HTTPException(status_code=404, detail="日程不存在")

    await db.delete(db_schedule)
    await db.commit()
    logger.info(f"删除日程: id={schedule_id}")


@router.post("/complete/{schedule_id}", response_model=ScheduleOut, summary="标记日程为完成")
async def complete_schedule(
    schedule_id: int,
    db: AsyncSession = Depends(get_db),
):
    """将指定日程标记为完成。"""
    result = await db.execute(
        select(Schedule).where(Schedule.id == schedule_id)
    )
    db_schedule = result.scalar_one_or_none()
    if not db_schedule:
        raise HTTPException(status_code=404, detail="日程不存在")

    db_schedule.status = ScheduleStatus.completed
    await db.commit()
    await db.refresh(db_schedule)
    logger.info(f"日程完成: id={db_schedule.id}")
    return db_schedule
