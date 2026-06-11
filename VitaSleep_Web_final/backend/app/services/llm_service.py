"""
VitaSleep Agent V1.2 — LLM 服务层
通过 OpenAI 兼容接口调用大模型，支持：
  - 动态从数据库读取 LLM 配置
  - Function Calling（工具调用：查询健康数据、查询/修改日程）
  - 流式/非流式输出
"""

import json
import logging
import os
from datetime import datetime, timedelta
from typing import AsyncGenerator, List, Dict, Optional, Any

from openai import AsyncOpenAI

logger = logging.getLogger("vitasleep.llm")

DEFAULT_LLM_BASE_URL = os.getenv("LLM_BASE_URL", "https://open.bigmodel.cn/api/coding/paas/v4")
DEFAULT_LLM_API_KEY = os.getenv("LLM_API_KEY", "b67e4c585b0a48d9a9eefbb391d0826c.ohwZG7dDVRNcgO9j")
DEFAULT_LLM_MODEL = os.getenv("LLM_MODEL", "glm-5.1")
DEFAULT_LLM_MAX_TOKENS = int(os.getenv("LLM_MAX_TOKENS", "1024"))
DEFAULT_LLM_TEMPERATURE = float(os.getenv("LLM_TEMPERATURE", "0.7"))

SYSTEM_PROMPT_TEMPLATE = """你是 VitaSleep 智能健康助手，一位专业、温暖、值得信赖的健康管理 AI。

## 当前时间
今天是 {current_date}（{current_weekday}），当前时间 {current_time}。
用户说"今天"指 {current_date}，"明天"指 {tomorrow_date}，以此类推。
创建日程时，必须使用正确的年份和日期。

## 核心职责
1. **健康数据分析** — 解读用户的身体电量、睡眠质量、血压心率等数据
2. **健康建议** — 基于用户数据和生活习惯，提供个性化建议
3. **日程管理** — 帮助用户合理安排日程
4. **情绪关怀** — 关注用户身心状态，适时给予鼓励和关怀

## 回复风格
- 亲切自然，像一位关心用户的朋友
- 先给结论，再补充分析和建议
- 适当使用 emoji 增加亲和力
- 如果涉及健康数据，给出具体的数值和趋势
- 不确定时坦诚说明，不编造数据

## 重要约束
- 不要编造不存在的健康数据
- 涉及严重健康问题时，建议用户及时就医
- 保持回复简洁"""

def _build_system_prompt() -> str:
    from datetime import datetime, timedelta
    now = datetime.utcnow()
    weekdays_cn = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]
    tomorrow = now + timedelta(days=1)
    return SYSTEM_PROMPT_TEMPLATE.format(
        current_date=now.strftime("%Y-%m-%d"),
        current_weekday=weekdays_cn[now.weekday()],
        current_time=now.strftime("%H:%M"),
        tomorrow_date=tomorrow.strftime("%Y-%m-%d"),
    )

TOOLS_DEFINITION = [
    {
        "type": "function",
        "function": {
            "name": "get_current_time",
            "description": "获取当前的准确日期和时间",
            "parameters": {
                "type": "object",
                "properties": {
                    "timezone": {
                        "type": "string",
                        "description": "时区，默认 Asia/Shanghai",
                        "default": "Asia/Shanghai"
                    }
                }
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "query_health_data",
            "description": "查询用户的健康数据",
            "parameters": {
                "type": "object",
                "properties": {
                    "metric_type": {
                        "type": "string",
                        "enum": ["battery", "blood_pressure", "sleep_stage", "heart_rate", "hrv", "cardio_index"],
                        "description": "要查询的指标类型"
                    },
                    "hours": {
                        "type": "integer",
                        "description": "查询最近多少小时的数据，默认24",
                        "default": 24
                    }
                }
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "query_schedule",
            "description": "查询用户的日程安排",
            "parameters": {
                "type": "object",
                "properties": {
                    "days": {
                        "type": "integer",
                        "description": "查询未来多少天的日程，默认7",
                        "default": 7
                    },
                    "status": {
                        "type": "string",
                        "enum": ["pending", "completed", "cancelled"],
                        "description": "按状态筛选"
                    }
                }
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "create_schedule",
            "description": "为用户创建新的日程安排",
            "parameters": {
                "type": "object",
                "required": ["user_id", "title", "start_time", "end_time"],
                "properties": {
                    "user_id": {"type": "string", "description": "用户ID"},
                    "title": {"type": "string", "description": "日程标题"},
                    "start_time": {"type": "string", "description": "开始时间，ISO 8601格式"},
                    "end_time": {"type": "string", "description": "结束时间，ISO 8601格式"},
                    "event_type": {
                        "type": "string",
                        "enum": ["fixed", "flexible", "health_intervention"],
                        "default": "flexible"
                    }
                }
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "modify_schedule",
            "description": "修改用户的日程安排",
            "parameters": {
                "type": "object",
                "required": ["schedule_id"],
                "properties": {
                    "schedule_id": {"type": "integer", "description": "日程ID"},
                    "title": {"type": "string", "description": "新标题"},
                    "start_time": {"type": "string", "description": "新的开始时间"},
                    "end_time": {"type": "string", "description": "新的结束时间"},
                    "status": {"type": "string", "enum": ["pending", "completed", "cancelled"]}
                }
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "delete_schedule",
            "description": "删除用户的日程",
            "parameters": {
                "type": "object",
                "required": ["schedule_id"],
                "properties": {
                    "schedule_id": {"type": "integer", "description": "要删除的日程ID"}
                }
            }
        }
    }
]

_client: Optional[AsyncOpenAI] = None
_current_config: Optional[Dict] = None

def _build_client_from_config(config: Dict) -> AsyncOpenAI:
    return AsyncOpenAI(
        base_url=config["base_url"],
        api_key=config["api_key"],
    )

def get_llm_client() -> AsyncOpenAI:
    global _client
    if _client is None:
        config = {
            "base_url": DEFAULT_LLM_BASE_URL,
            "api_key": DEFAULT_LLM_API_KEY,
        }
        _client = _build_client_from_config(config)
        _current_config = config
        logger.info(f"LLM 客户端初始化: base_url={DEFAULT_LLM_BASE_URL}, model={DEFAULT_LLM_MODEL}")
    return _client

def get_llm_config() -> Dict:
    return {
        "model": _current_config.get("model", DEFAULT_LLM_MODEL) if _current_config else DEFAULT_LLM_MODEL,
        "max_tokens": _current_config.get("max_tokens", DEFAULT_LLM_MAX_TOKENS) if _current_config else DEFAULT_LLM_MAX_TOKENS,
        "temperature": _current_config.get("temperature", DEFAULT_LLM_TEMPERATURE) if _current_config else DEFAULT_LLM_TEMPERATURE,
    }

def reload_llm_client():
    global _client
    _client = None
    logger.info("LLM 客户端已标记为需要重载")

async def load_config_from_db(db):
    from app.models.models import LLMConfig
    from sqlalchemy import select

    stmt = select(LLMConfig).where(LLMConfig.is_active == True)
    result = await db.execute(stmt)
    config = result.scalar_one_or_none()

    global _client, _current_config
    if config:
        new_config = {
            "base_url": config.base_url,
            "api_key": config.api_key,
            "model": config.model,
            "max_tokens": config.max_tokens,
            "temperature": config.temperature,
        }
        if _current_config != new_config:
            _client = _build_client_from_config(new_config)
            _current_config = new_config
            logger.info(f"从数据库加载 LLM 配置: model={config.model}")
    else:
        if _client is None:
            get_llm_client()

async def execute_tool_call(tool_name: str, arguments: Dict, db, user_id: str) -> Any:
    from datetime import datetime, timedelta, timezone
    from app.models.models import (
        HealthMetric, MetricType, Schedule,
        ScheduleSource, ScheduleStatus,
    )
    from sqlalchemy import select

    if tool_name == "get_current_time":
        tz_name = arguments.get("timezone", "Asia/Shanghai")
        try:
            import zoneinfo
            tz = zoneinfo.ZoneInfo(tz_name)
        except Exception:
            tz = zoneinfo.ZoneInfo("Asia/Shanghai")
        now = datetime.now(tz)
        weekdays_cn = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]
        tomorrow = now + timedelta(days=1)
        return {
            "current_time": now.strftime("%Y-%m-%d %H:%M:%S"),
            "date": now.strftime("%Y-%m-%d"),
            "weekday": weekdays_cn[now.weekday()],
            "time": now.strftime("%H:%M"),
            "tomorrow": tomorrow.strftime("%Y-%m-%d"),
            "timezone": tz_name,
        }

    elif tool_name == "query_health_data":
        metric_type = arguments.get("metric_type")
        hours = arguments.get("hours", 24)

        stmt = select(HealthMetric).where(HealthMetric.user_id == user_id)
        if metric_type:
            try:
                mt = MetricType(metric_type)
                stmt = stmt.where(HealthMetric.metric_type == mt)
            except ValueError:
                pass
        start_time = datetime.utcnow() - timedelta(hours=hours)
        stmt = stmt.where(HealthMetric.computed_at >= start_time)
        stmt = stmt.order_by(HealthMetric.computed_at.desc()).limit(100)

        result = await db.execute(stmt)
        metrics = list(result.scalars().all())
        return [
            {
                "type": m.metric_type.value if hasattr(m.metric_type, "value") else str(m.metric_type),
                "value": m.value,
                "time": m.computed_at.isoformat() if m.computed_at else None,
            }
            for m in metrics
        ]

    elif tool_name == "query_schedule":
        days = arguments.get("days", 7)
        status = arguments.get("status")
        now = datetime.utcnow()

        stmt = (
            select(Schedule)
            .where(Schedule.user_id == user_id)
            .where(Schedule.start_time >= now)
            .where(Schedule.start_time < now + timedelta(days=days))
            .order_by(Schedule.start_time.asc())
        )
        if status:
            try:
                stmt = stmt.where(Schedule.status == ScheduleStatus(status))
            except ValueError:
                pass

        result = await db.execute(stmt)
        schedules = list(result.scalars().all())
        return [
            {
                "id": s.id,
                "title": s.title,
                "type": s.event_type.value if hasattr(s.event_type, "value") else str(s.event_type),
                "start": s.start_time.isoformat() if s.start_time else None,
                "end": s.end_time.isoformat() if s.end_time else None,
                "status": s.status.value if hasattr(s.status, "value") else str(s.status),
            }
            for s in schedules
        ]

    elif tool_name == "create_schedule":
        new_schedule = Schedule(
            user_id=user_id,
            title=arguments["title"],
            event_type=arguments.get("event_type", "flexible"),
            start_time=datetime.fromisoformat(arguments["start_time"]),
            end_time=datetime.fromisoformat(arguments["end_time"]),
            source=ScheduleSource.agent_generated,
            status=ScheduleStatus.pending,
        )
        db.add(new_schedule)
        await db.commit()
        await db.refresh(new_schedule)
        logger.info(f"LLM 创建日程: {new_schedule.title}")
        return {"id": new_schedule.id, "title": new_schedule.title, "message": "日程创建成功"}

    elif tool_name == "modify_schedule":
        schedule_id = arguments["schedule_id"]
        stmt = select(Schedule).where(Schedule.id == schedule_id, Schedule.user_id == user_id)
        result = await db.execute(stmt)
        schedule = result.scalar_one_or_none()
        if not schedule:
            return {"error": "日程不存在"}
        for key in ["title", "status"]:
            if key in arguments:
                setattr(schedule, key, arguments[key])
        for key in ["start_time", "end_time"]:
            if key in arguments:
                setattr(schedule, key, datetime.fromisoformat(arguments[key]))
        await db.commit()
        logger.info(f"LLM 修改日程: {schedule.title}")
        return {"id": schedule.id, "title": schedule.title, "message": "日程修改成功"}

    elif tool_name == "delete_schedule":
        schedule_id = arguments["schedule_id"]
        stmt = select(Schedule).where(Schedule.id == schedule_id, Schedule.user_id == user_id)
        result = await db.execute(stmt)
        schedule = result.scalar_one_or_none()
        if not schedule:
            return {"error": "日程不存在"}
        title = schedule.title
        await db.delete(schedule)
        await db.commit()
        logger.info(f"LLM 删除日程: {title}")
        return {"message": f"日程 '{title}' 已删除"}

    else:
        return {"error": f"未知工具: {tool_name}"}

def build_health_context(
    battery_data: Optional[List[dict]] = None,
    bp_data: Optional[List[dict]] = None,
    sleep_data: Optional[List[dict]] = None,
) -> str:
    parts = []

    if battery_data:
        latest = battery_data[-1] if battery_data else None
        if latest:
            level = latest.get("value", {}).get("level", "N/A")
            trend = latest.get("value", {}).get("trend", "N/A")
            parts.append(f"【身体电量】当前 {level}%，趋势：{trend}")

    if bp_data:
        latest = bp_data[-1] if bp_data else None
        if latest:
            val = latest.get("value", {})
            sys_val = val.get("systolic", val.get("sys", "N/A"))
            dia_val = val.get("diastolic", val.get("dia", "N/A"))
            parts.append(f"【血压】最近一次：{sys_val}/{dia_val} mmHg")

    if sleep_data:
        total = sum(d.get("value", {}).get("duration", 0) for d in sleep_data)
        parts.append(f"【睡眠】最近7天总睡眠记录 {len(sleep_data)} 条")

    if parts:
        return "\n".join(["以下是用户的最新健康数据："] + parts + ["请结合以上数据回答。"])
    return ""

async def chat_stream(
    user_message: str,
    chat_history: Optional[List[Dict[str, str]]] = None,
    health_context: str = "",
    db=None,
    user_id: str = "test_user_001",
) -> AsyncGenerator[str, None]:
    client = get_llm_client()
    config = get_llm_config()

    messages = [{"role": "system", "content": _build_system_prompt()}]

    if chat_history:
        for msg in chat_history[-20:]:
            messages.append({
                "role": msg.get("role", "user"),
                "content": msg.get("content", ""),
            })

    full_message = user_message
    if health_context:
        full_message = f"{health_context}\n\n用户说：{user_message}"

    messages.append({"role": "user", "content": full_message})

    logger.info(f"LLM 请求: messages={len(messages)}条")

    try:
        response = await client.chat.completions.create(
            model=config["model"],
            messages=messages,
            max_tokens=config["max_tokens"],
            temperature=config["temperature"],
            stream=True,
            tools=TOOLS_DEFINITION,
            tool_choice="auto",
        )

        tool_calls = {}
        current_text = ""

        async for chunk in response:
            if chunk.choices and chunk.choices[0].delta.content:
                token = chunk.choices[0].delta.content
                current_text += token
                yield token

            if chunk.choices and chunk.choices[0].delta.tool_calls:
                for tc in chunk.choices[0].delta.tool_calls:
                    idx = tc.index
                    if idx not in tool_calls:
                        tool_calls[idx] = {"id": "", "name": "", "arguments": ""}
                    if tc.id:
                        tool_calls[idx]["id"] = tc.id
                    if tc.function:
                        if tc.function.name:
                            tool_calls[idx]["name"] += tc.function.name
                        if tc.function.arguments:
                            tool_calls[idx]["arguments"] += tc.function.arguments

        if tool_calls and db:
            messages.append({
                "role": "assistant",
                "content": current_text or None,
                "tool_calls": [
                    {
                        "id": tc["id"],
                        "type": "function",
                        "function": {
                            "name": tc["name"],
                            "arguments": tc["arguments"],
                        }
                    }
                    for tc in tool_calls.values()
                ],
            })

            schedule_changed = False
            for idx, tc in tool_calls.items():
                tool_name = tc["name"]
                try:
                    args = json.loads(tc["arguments"]) if tc["arguments"] else {}
                except json.JSONDecodeError:
                    args = {}

                logger.info(f"工具调用: {tool_name}({args})")
                result = await execute_tool_call(tool_name, args, db, user_id)

                if tool_name in ("create_schedule", "modify_schedule", "delete_schedule"):
                    schedule_changed = True

                messages.append({
                    "role": "tool",
                    "tool_call_id": tc["id"],
                    "content": json.dumps(result, ensure_ascii=False, default=str),
                })

            if schedule_changed:
                yield ("__event__", "schedule_changed")

            response2 = await client.chat.completions.create(
                model=config["model"],
                messages=messages,
                max_tokens=config["max_tokens"],
                temperature=config["temperature"],
                stream=True,
            )

            async for chunk in response2:
                if chunk.choices and chunk.choices[0].delta.content:
                    yield chunk.choices[0].delta.content

    except Exception as e:
        logger.error(f"LLM 调用失败: {e}")
        err_msg = str(e)[:100]
        yield f"\n[抱歉，AI 服务暂时不可用，请稍后再试。错误：{err_msg}]"

async def chat_completion(
    user_message: str,
    chat_history: Optional[List[Dict[str, str]]] = None,
    health_context: str = "",
) -> str:
    client = get_llm_client()
    config = get_llm_config()

    messages = [{"role": "system", "content": _build_system_prompt()}]

    if chat_history:
        for msg in chat_history[-20:]:
            messages.append({
                "role": msg.get("role", "user"),
                "content": msg.get("content", ""),
            })

    full_message = user_message
    if health_context:
        full_message = f"{health_context}\n\n用户说：{user_message}"

    messages.append({"role": "user", "content": full_message})

    try:
        response = await client.chat.completions.create(
            model=config["model"],
            messages=messages,
            max_tokens=config["max_tokens"],
            temperature=config["temperature"],
            stream=False,
        )
        return response.choices[0].message.content

    except Exception as e:
        logger.error(f"LLM 调用失败: {e}")
        return "抱歉，AI 服务暂时不可用，请稍后再试。"
