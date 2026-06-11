"""
VitaSleep Agent V1.2 — LLM 配置管理 API
提供大模型 API 参数的增删改查，支持动态切换模型。
"""

import logging
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.database import get_db
from app.models.models import LLMConfig
from app.schemas import (
    LLMConfigCreate, LLMConfigUpdate, LLMConfigOut, ErrorResponse,
)
from app.services.llm_service import reload_llm_client

logger = logging.getLogger("vitasleep.api.llm_config")

router = APIRouter(prefix="/api/llm-config", tags=["LLM 配置"])


@router.get(
    "",
    response_model=List[LLMConfigOut],
    summary="获取所有 LLM 配置",
)
async def list_configs(db: AsyncSession = Depends(get_db)):
    stmt = select(LLMConfig).order_by(LLMConfig.is_active.desc(), LLMConfig.updated_at.desc())
    result = await db.execute(stmt)
    return list(result.scalars().all())


@router.get(
    "/active",
    response_model=LLMConfigOut,
    summary="获取当前激活的 LLM 配置",
    responses={404: {"model": ErrorResponse}},
)
async def get_active_config(db: AsyncSession = Depends(get_db)):
    stmt = select(LLMConfig).where(LLMConfig.is_active == True)
    result = await db.execute(stmt)
    config = result.scalar_one_or_none()
    if not config:
        raise HTTPException(status_code=404, detail="没有激活的 LLM 配置")
    return config


@router.post(
    "",
    response_model=LLMConfigOut,
    summary="创建 LLM 配置",
    status_code=201,
)
async def create_config(
    config: LLMConfigCreate,
    db: AsyncSession = Depends(get_db),
):
    new_config = LLMConfig(
        name=config.name,
        base_url=config.base_url,
        api_key=config.api_key,
        model=config.model,
        max_tokens=config.max_tokens,
        temperature=config.temperature,
        is_active=True,
    )

    stmt = select(LLMConfig).where(LLMConfig.is_active == True)
    result = await db.execute(stmt)
    for existing in result.scalars().all():
        existing.is_active = False

    db.add(new_config)
    await db.commit()
    await db.refresh(new_config)

    reload_llm_client()

    logger.info(f"创建 LLM 配置: {config.name}, model={config.model}")
    return new_config


@router.put(
    "/{config_id}",
    response_model=LLMConfigOut,
    summary="更新 LLM 配置",
    responses={404: {"model": ErrorResponse}},
)
async def update_config(
    config_id: int,
    update: LLMConfigUpdate,
    db: AsyncSession = Depends(get_db),
):
    stmt = select(LLMConfig).where(LLMConfig.id == config_id)
    result = await db.execute(stmt)
    config = result.scalar_one_or_none()
    if not config:
        raise HTTPException(status_code=404, detail="配置不存在")

    update_data = update.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(config, key, value)

    if update.is_active:
        stmt2 = select(LLMConfig).where(LLMConfig.is_active == True, LLMConfig.id != config_id)
        result2 = await db.execute(stmt2)
        for other in result2.scalars().all():
            other.is_active = False

    await db.commit()
    await db.refresh(config)

    if config.is_active:
        reload_llm_client()

    logger.info(f"更新 LLM 配置: {config.name}")
    return config


@router.delete(
    "/{config_id}",
    summary="删除 LLM 配置",
)
async def delete_config(
    config_id: int,
    db: AsyncSession = Depends(get_db),
):
    stmt = select(LLMConfig).where(LLMConfig.id == config_id)
    result = await db.execute(stmt)
    config = result.scalar_one_or_none()
    if not config:
        raise HTTPException(status_code=404, detail="配置不存在")

    await db.delete(config)
    await db.commit()
    return {"status": "ok", "message": f"配置 '{config.name}' 已删除"}
