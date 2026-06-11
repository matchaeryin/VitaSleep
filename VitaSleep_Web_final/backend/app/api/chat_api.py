"""
VitaSleep Agent V2.0 — 聊天 API
提供聊天历史查询、发送消息、SSE 实时流式响应接口。
V2.0 新增：集成 Agent 服务，通过 Agent 处理用户请求。
"""

import asyncio
import json
import logging
import os
import httpx
from datetime import datetime, timedelta
from typing import List, Optional
from fastapi import APIRouter, Depends, Query, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.database import get_db
from app.models.models import ChatHistory, ChatRole
from app.schemas import (
    ChatMessageCreate, ChatMessageOut, ErrorResponse,
)
from app.services.llm_service import chat_stream, chat_completion, build_health_context, load_config_from_db
from app.services.data_service import get_metrics_range
from app.models.models import MetricType, Schedule

logger = logging.getLogger("vitasleep.api.chat")

router = APIRouter(prefix="/api/chat", tags=["聊天"])

# Agent 服务配置
AGENT_SERVICE_URL = os.getenv("AGENT_SERVICE_URL", "http://localhost:8001")
USE_AGENT_SERVICE = os.getenv("USE_AGENT_SERVICE", "true").lower() == "true"

logger.info(f"Agent 服务地址: {AGENT_SERVICE_URL}, 启用状态: {USE_AGENT_SERVICE}")


# ──────────────── Agent 服务调用 ────────────────

async def _call_agent_service(user_id: str, message: str, conversation_id: str = None) -> dict:
    """
    调用 Agent 服务获取回复
    """
    url = f"{AGENT_SERVICE_URL}/api/v1/chat"
    payload = {
        "user_id": user_id,
        "message": message,
        "conversation_id": conversation_id
    }

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(url, json=payload)
            response.raise_for_status()
            return response.json()
    except httpx.ConnectError:
        logger.error(f"无法连接 Agent 服务: {url}")
        raise
    except httpx.TimeoutException:
        logger.error(f"Agent 服务超时: {url}")
        raise
    except httpx.HTTPStatusError as e:
        logger.error(f"Agent 服务返回错误: {e.response.status_code}")
        raise


async def _stream_agent_service(user_id: str, message: str):
    """
    流式调用 Agent 服务（生成器）
    """
    url = f"{AGENT_SERVICE_URL}/api/v1/stream/{user_id}"
    params = {"message": message}

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            async with client.stream("GET", url, params=params) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if line.startswith("data: "):
                        yield line[6:]
    except Exception as e:
        logger.error(f"Agent 流式调用失败: {e}")
        yield json.dumps({"error": str(e)}, ensure_ascii=False)


async def _get_battery(db: AsyncSession, user_id: str):
    """并行辅助：获取电量数据"""
    return await get_metrics_range(
        db=db, user_id=user_id,
        start_time=datetime.utcnow().replace(hour=0, minute=0, second=0),
        end_time=datetime.utcnow(),
        metric_type=MetricType.battery,
        limit=50,
    )

async def _get_bp(db: AsyncSession, user_id: str):
    """并行辅助：获取血压数据"""
    return await get_metrics_range(
        db=db, user_id=user_id,
        start_time=datetime.utcnow() - timedelta(hours=48),
        end_time=datetime.utcnow(),
        metric_type=MetricType.blood_pressure,
        limit=50,
    )

async def _get_sleep(db: AsyncSession, user_id: str):
    """并行辅助：获取睡眠数据"""
    return await get_metrics_range(
        db=db, user_id=user_id,
        start_time=datetime.utcnow() - timedelta(days=7),
        end_time=datetime.utcnow(),
        metric_type=MetricType.sleep_stage,
        limit=100,
    )

async def _get_schedules(db: AsyncSession, user_id: str):
    """并行辅助：获取今日日程"""
    today_start = datetime.utcnow().replace(hour=0, minute=0, second=0)
    today_end = today_start + timedelta(days=1)

    stmt = (
        select(Schedule)
        .where(Schedule.user_id == user_id)
        .where(Schedule.start_time >= today_start)
        .where(Schedule.start_time < today_end)
        .order_by(Schedule.start_time.asc())
        .limit(20)
    )
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def _get_full_context(db: AsyncSession, user_id: str) -> str:
    """并行获取用户健康数据 + 日程，构建完整上下文"""
    parts = []

    try:
        battery, bp, sleep = await asyncio.gather(
            _get_battery(db, user_id),
            _get_bp(db, user_id),
            _get_sleep(db, user_id),
        )

        battery_dicts = [
            {"value": {"level": m.value.get("level"), "trend": m.value.get("trend")}}
            for m in battery
        ] if battery else []
        bp_dicts = [{"value": m.value} for m in bp] if bp else []
        sleep_dicts = [{"value": m.value} for m in sleep] if sleep else []

        health_ctx = build_health_context(
            battery_data=battery_dicts,
            bp_data=bp_dicts,
            sleep_data=sleep_dicts,
        )
        if health_ctx:
            parts.append(health_ctx)
    except Exception as e:
        logger.warning(f"获取健康上下文失败: {e}")

    try:
        schedules = await _get_schedules(db, user_id)

        if schedules:
            schedule_lines = ["以下是用户今天的日程安排："]
            type_map = {"fixed": "固定", "flexible": "弹性", "health_intervention": "健康干预"}
            source_map = {"user_manual": "手动创建", "agent_generated": "AI建议", "calendar_sync": "日历同步"}
            status_map = {"pending": "待处理", "completed": "已完成", "cancelled": "已取消"}
            for s in schedules:
                t = s.start_time.strftime("%H:%M") if s.start_time else "?"
                e = s.end_time.strftime("%H:%M") if s.end_time else "?"
                etype = type_map.get(s.event_type.value if hasattr(s.event_type, 'value') else s.event_type, s.event_type)
                src = source_map.get(s.source.value if hasattr(s.source, 'value') else s.source, s.source)
                st = status_map.get(s.status.value if hasattr(s.status, 'value') else s.status, s.status)
                schedule_lines.append(f"  • {t}-{e} {s.title}（{etype}，{src}，{st}）")
            parts.append("\n".join(schedule_lines))
        else:
            parts.append("用户今天暂无日程安排。")
    except Exception as e:
        logger.warning(f"获取日程上下文失败: {e}")

    return "\n\n".join(parts) if parts else ""


async def _get_chat_history(db: AsyncSession, user_id: str, limit: int = 20) -> list:
    """获取最近对话历史"""
    stmt = (
        select(ChatHistory)
        .where(ChatHistory.user_id == user_id)
        .order_by(ChatHistory.created_at.desc())
        .limit(limit)
    )
    result = await db.execute(stmt)
    messages = list(reversed(result.scalars().all()))
    return [{"role": m.role.value, "content": m.content} for m in messages]


@router.get(
    "/history",
    response_model=List[ChatMessageOut],
    summary="获取聊天历史",
    responses={400: {"model": ErrorResponse}},
)
async def get_chat_history(
    user_id: str = Query(..., description="用户 ID"),
    limit: int = Query(50, ge=1, le=500, description="返回条数限制"),
    before: Optional[datetime] = Query(None, description="查询此时间之前的消息（分页用）"),
    db: AsyncSession = Depends(get_db),
):
    stmt = select(ChatHistory).where(ChatHistory.user_id == user_id)
    if before:
        stmt = stmt.where(ChatHistory.created_at < before)
    stmt = stmt.order_by(ChatHistory.created_at.desc()).limit(limit)
    result = await db.execute(stmt)
    return list(result.scalars().all())


@router.post(
    "/send",
    response_model=ChatMessageOut,
    summary="发送聊天消息（非流式）",
    status_code=201,
    responses={400: {"model": ErrorResponse}},
)
async def send_chat_message(
    message: ChatMessageCreate,
    db: AsyncSession = Depends(get_db),
):
    now = datetime.utcnow()

    user_msg = ChatHistory(
        user_id=message.user_id,
        role=ChatRole.user,
        content=message.content,
        agent_type=message.agent_type,
        created_at=now,
    )
    db.add(user_msg)
    await db.commit()

    reply_content = ""
    agent_name = "unknown"

    if USE_AGENT_SERVICE:
        try:
            agent_response = await _call_agent_service(
                user_id=message.user_id,
                message=message.content,
                conversation_id=f"conv_{user_msg.id}"
            )
            reply_content = agent_response.get("response", "")
            agent_name = agent_response.get("agent_name", "agent")
            logger.info(f"Agent 服务返回: {agent_name}")
        except Exception as e:
            logger.warning(f"Agent 服务调用失败，降级为直接 LLM: {e}")

    if not reply_content:
        health_ctx = await _get_full_context(db, message.user_id)
        history = await _get_chat_history(db, message.user_id)

        reply_content = await chat_completion(
            user_message=message.content,
            chat_history=history,
            health_context=health_ctx,
        )
        agent_name = "llm_fallback"

    assistant_msg = ChatHistory(
        user_id=message.user_id,
        role=ChatRole.assistant,
        content=reply_content,
        agent_type=agent_name,
        created_at=datetime.utcnow(),
    )
    db.add(assistant_msg)
    await db.commit()
    await db.refresh(assistant_msg)

    return assistant_msg


@router.get(
    "/stream",
    summary="SSE 流式对话（支持 Agent 服务）",
    description="发送消息并通过 SSE 流式接收回复。V2.0 优先调用 Agent 服务。",
)
async def chat_stream_endpoint(
    user_id: str = Query("test_user_001", description="用户 ID"),
    message: str = Query(..., description="用户消息内容"),
    db: AsyncSession = Depends(get_db),
):
    async def event_generator():
        full_reply = ""
        agent_name = "unknown"
        use_agent = USE_AGENT_SERVICE

        try:
            user_msg = ChatHistory(
                user_id=user_id,
                role=ChatRole.user,
                content=message,
                agent_type="vitasleep-agent",
                created_at=datetime.utcnow(),
            )
            db.add(user_msg)
            await db.commit()

            if use_agent:
                try:
                    async for line in _stream_agent_service(user_id, message):
                        try:
                            data = json.loads(line)

                            if "response" in data:
                                full_reply = data["response"]
                                agent_name = data.get("agent_name", "agent")

                                for char in full_reply:
                                    payload = json.dumps({"text": char}, ensure_ascii=False)
                                    yield f"data: {payload}\n\n"
                                    await asyncio.sleep(0.01)
                            elif "text" in data:
                                token = data["text"]
                                full_reply += token
                                yield f"data: {line}\n\n"
                            elif "error" in data:
                                logger.warning(f"Agent 返回错误: {data['error']}")
                                use_agent = False
                                break
                            elif "DONE" in line:
                                break
                        except json.JSONDecodeError:
                            if "DONE" in line:
                                break

                    if full_reply:
                        logger.info(f"Agent 服务返回成功: {agent_name}")

                except Exception as e:
                    logger.warning(f"Agent 流式调用失败，降级为 LLM: {e}")
                    use_agent = False

            if not full_reply and not use_agent:
                await load_config_from_db(db)

                health_ctx, history = await asyncio.gather(
                    _get_full_context(db, user_id),
                    _get_chat_history(db, user_id),
                )

                async for item in chat_stream(
                    user_message=message,
                    chat_history=history,
                    health_context=health_ctx,
                    db=db,
                    user_id=user_id,
                ):
                    if isinstance(item, tuple) and item[0] == "__event__":
                        event_payload = json.dumps({"event": item[1]}, ensure_ascii=False)
                        yield f"data: {event_payload}\n\n"
                        continue

                    token = item
                    full_reply += token
                    payload = json.dumps({"text": token}, ensure_ascii=False)
                    yield f"data: {payload}\n\n"

                agent_name = "llm_fallback"

            if full_reply:
                assistant_msg = ChatHistory(
                    user_id=user_id,
                    role=ChatRole.assistant,
                    content=full_reply,
                    agent_type=agent_name,
                    created_at=datetime.utcnow(),
                )
                db.add(assistant_msg)
                await db.commit()

            yield "data: [DONE]\n\n"

        except Exception as e:
            logger.error(f"SSE 流式对话异常: {e}")
            error_payload = json.dumps({"error": str(e)[:200]}, ensure_ascii=False)
            yield f"data: {error_payload}\n\n"
            yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
