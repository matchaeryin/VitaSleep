"""
VitaSleep Agent V1.0 — 用户档案 API
提供用户档案的查询和更新接口。
"""

import logging
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.database import get_db
from app.models.models import UserProfile
from app.schemas import (
    UserProfileCreate, UserProfileUpdate, UserProfileOut, ErrorResponse,
)

logger = logging.getLogger("vitasleep.api.profile")

router = APIRouter(prefix="/api/profile", tags=["用户档案"])


@router.get(
    "/{user_id}",
    response_model=UserProfileOut,
    summary="获取用户档案",
    responses={404: {"model": ErrorResponse}},
)
async def get_profile(
    user_id: str,
    db: AsyncSession = Depends(get_db),
):
    stmt = select(UserProfile).where(UserProfile.user_id == user_id)
    result = await db.execute(stmt)
    profile = result.scalar_one_or_none()

    if not profile:
        raise HTTPException(status_code=404, detail=f"用户档案不存在: user_id={user_id}")

    return profile


@router.put(
    "/{user_id}",
    response_model=UserProfileOut,
    summary="更新用户档案",
    responses={404: {"model": ErrorResponse}},
)
async def update_profile(
    user_id: str,
    update_data: UserProfileUpdate,
    db: AsyncSession = Depends(get_db),
):
    stmt = select(UserProfile).where(UserProfile.user_id == user_id)
    result = await db.execute(stmt)
    profile = result.scalar_one_or_none()

    update_dict = update_data.model_dump(exclude_unset=True)

    if not profile:
        profile = UserProfile(
            user_id=user_id,
            age=update_dict.get("age"),
            gender=update_dict.get("gender"),
            height=update_dict.get("height"),
            weight=update_dict.get("weight"),
            exercise_habits=update_dict.get("exercise_habits"),
            sleep_habits=update_dict.get("sleep_habits"),
            work_type=update_dict.get("work_type"),
            health_goals=update_dict.get("health_goals"),
            medical_history=update_dict.get("medical_history"),
            baseline_battery=update_dict.get("baseline_battery"),
            sleep_pattern=update_dict.get("sleep_pattern"),
            work_pattern=update_dict.get("work_pattern"),
            health_preferences=update_dict.get("health_preferences"),
            is_complete=update_dict.get("is_complete", False),
            updated_at=datetime.utcnow(),
        )
        db.add(profile)
        logger.info(f"创建用户档案: user_id={user_id}")
    else:
        for key, value in update_dict.items():
            setattr(profile, key, value)
        profile.updated_at = datetime.utcnow()
        logger.info(f"更新用户档案: user_id={user_id}, fields={list(update_dict.keys())}")

    await db.commit()
    await db.refresh(profile)
    return profile
