"""
VitaSleep Agent V1.0 — Pydantic Schema 定义
包含 API 请求/响应模型、健康指标数据结构、Veepoo 设备数据模型。
"""

from datetime import datetime
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field

# ─── 基础响应 ───

class HealthCheckResponse(BaseModel):
    status: str
    version: str
    timestamp: datetime

class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None

# ─── 健康指标 ───

class HealthMetricOut(BaseModel):
    id: int
    user_id: str
    metric_type: str
    value: Any
    computed_at: datetime
    valid_until: Optional[datetime] = None

class HealthMetricsQuery(BaseModel):
    user_id: str
    metric_type: Optional[str] = None
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    limit: int = 100

# ─── 日程 ───

class ScheduleCreate(BaseModel):
    user_id: str
    title: str
    event_type: str = "flexible"
    start_time: datetime
    end_time: datetime

class ScheduleOut(BaseModel):
    id: int
    user_id: str
    title: str
    event_type: str
    start_time: datetime
    end_time: datetime
    status: str
    created_at: datetime

# ─── 聊天 ───

class ChatMessage(BaseModel):
    user_id: str
    content: str
    agent_type: Optional[str] = None

class ChatResponse(BaseModel):
    id: int
    user_id: str
    role: str
    content: str
    created_at: datetime

# ─── 用户档案 ───

class UserProfileUpdate(BaseModel):
    age: Optional[int] = None
    gender: Optional[str] = None
    height: Optional[float] = None
    weight: Optional[float] = None
    exercise_habits: Optional[str] = None
    sleep_habits: Optional[str] = None

# ─── Veepoo 设备数据 ───

class VeepooOriginRecord(BaseModel):
    timestamp: datetime
    heart_rate: int = Field(..., ge=30, le=200)
    heart_rate_array: Optional[List[int]] = None
    systolic: int = Field(..., ge=60, le=220)
    diastolic: int = Field(..., ge=40, le=150)
    steps: int = Field(default=0, ge=0)
    spo2: Optional[int] = None
    package_number: Optional[int] = None
    total_packages: Optional[int] = None

class VeepooOriginDataRequest(BaseModel):
    user_id: str
    device_id: Optional[str] = None
    device_model: Optional[str] = None
    records: List[VeepooOriginRecord]
    data_date: Optional[str] = None

class VeepooSleepDataRequest(BaseModel):
    user_id: str
    device_id: Optional[str] = None
    sleep_date: str
    sleep_start: datetime
    sleep_end: datetime
    total_sleep_min: int
    deep_sleep_min: int = 0
    light_sleep_min: int = 0
    rem_sleep_min: int = 0
    awake_min: int = 0
    deep_pct: float = 0.0
    light_pct: float = 0.0
    rem_pct: float = 0.0
    awake_pct: float = 0.0
    quality_score: Optional[float] = None

class VeepooBatteryRequest(BaseModel):
    user_id: str
    device_id: Optional[str] = None
    battery_level: int = Field(..., ge=0, le=100)
    is_charging: bool = False

class VeepooResponse(BaseModel):
    status: str = "ok"
    message: str = "操作成功"
    records_processed: int = 0
    metric_ids: List[int] = Field(default_factory=list)
    errors: List[str] = Field(default_factory=list)
