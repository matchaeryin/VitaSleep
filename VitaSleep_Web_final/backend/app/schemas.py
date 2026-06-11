"""
VitaSleep Agent V1.0 — Pydantic Schema 定义
所有 API 的请求/响应模型。
"""

from datetime import datetime
from typing import Optional, Any, List
from pydantic import BaseModel, Field
from app.models.models import (
    MetricType, EventType, ScheduleSource, ScheduleStatus, ChatRole,
)


# ──────────────── 通用 ────────────────

class HealthCheckResponse(BaseModel):
    """健康检查响应"""
    status: str = "ok"
    version: str = "1.0.0"
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class ErrorResponse(BaseModel):
    """统一错误响应"""
    error: str
    detail: Optional[str] = None


# ──────────────── 健康指标 ────────────────

class HealthMetricBase(BaseModel):
    """健康指标基础 Schema"""
    user_id: str = Field(..., description="用户 ID")
    metric_type: MetricType = Field(..., description="指标类型")
    value: Any = Field(..., description="指标值（JSON）")
    computed_at: datetime = Field(..., description="指标计算时间")
    valid_until: Optional[datetime] = Field(None, description="指标有效期至")


class HealthMetricCreate(HealthMetricBase):
    """创建健康指标"""
    pass


class HealthMetricOut(HealthMetricBase):
    """健康指标输出"""
    id: int

    model_config = {"from_attributes": True}


class HealthMetricsQuery(BaseModel):
    """健康指标查询参数"""
    user_id: str = Field(..., description="用户 ID")
    metric_type: Optional[MetricType] = Field(None, description="指标类型（可选）")
    start_time: Optional[datetime] = Field(None, description="查询开始时间")
    end_time: Optional[datetime] = Field(None, description="查询结束时间")
    limit: int = Field(100, ge=1, le=1000, description="返回条数限制")


# ──────────────── 日程 ────────────────

class ScheduleBase(BaseModel):
    """日程基础 Schema"""
    user_id: str = Field(..., description="用户 ID")
    title: str = Field(..., max_length=256, description="事件标题")
    event_type: EventType = Field(EventType.flexible, description="事件类型")
    start_time: datetime = Field(..., description="开始时间")
    end_time: datetime = Field(..., description="结束时间")
    source: ScheduleSource = Field(ScheduleSource.user_manual, description="来源")


class ScheduleCreate(ScheduleBase):
    """创建日程"""
    pass


class ScheduleUpdate(BaseModel):
    """更新日程（所有字段可选）"""
    title: Optional[str] = Field(None, max_length=256, description="事件标题")
    event_type: Optional[EventType] = Field(None, description="事件类型")
    start_time: Optional[datetime] = Field(None, description="开始时间")
    end_time: Optional[datetime] = Field(None, description="结束时间")
    status: Optional[ScheduleStatus] = Field(None, description="状态")
    source: Optional[ScheduleSource] = Field(None, description="来源")


class ScheduleOut(ScheduleBase):
    """日程输出"""
    id: int
    status: ScheduleStatus
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class ScheduleQuery(BaseModel):
    """日程查询参数"""
    user_id: str = Field(..., description="用户 ID")
    status: Optional[ScheduleStatus] = Field(None, description="状态筛选")
    start_time: Optional[datetime] = Field(None, description="开始时间范围")
    end_time: Optional[datetime] = Field(None, description="结束时间范围")
    limit: int = Field(50, ge=1, le=500, description="返回条数限制")


# ──────────────── 用户档案 ────────────────

class UserProfileBase(BaseModel):
    """用户档案基础 Schema"""
    user_id: str = Field(..., description="用户 ID")

    # 基本身体信息
    age: Optional[int] = Field(None, description="年龄")
    gender: Optional[str] = Field(None, description="性别")
    height: Optional[float] = Field(None, description="身高（cm）")
    weight: Optional[float] = Field(None, description="体重（kg）")

    # 生活习惯
    exercise_habits: Optional[str] = Field(None, description="运动习惯")
    sleep_habits: Optional[str] = Field(None, description="睡眠习惯")
    work_type: Optional[str] = Field(None, description="工作性质")

    # 健康目标与病史
    health_goals: Optional[list] = Field(None, description="健康目标")
    medical_history: Optional[str] = Field(None, description="病史记录")

    # 原有字段
    baseline_battery: Optional[Any] = Field(None, description="基线身体电量")
    sleep_pattern: Optional[Any] = Field(None, description="睡眠模式")
    work_pattern: Optional[Any] = Field(None, description="工作模式")
    health_preferences: Optional[Any] = Field(None, description="健康偏好")

    # 完成标记
    is_complete: Optional[bool] = Field(False, description="画像是否完整")


class UserProfileCreate(UserProfileBase):
    """创建用户档案"""
    pass


class UserProfileUpdate(BaseModel):
    """更新用户档案（所有字段可选）"""
    age: Optional[int] = Field(None, description="年龄")
    gender: Optional[str] = Field(None, description="性别")
    height: Optional[float] = Field(None, description="身高（cm）")
    weight: Optional[float] = Field(None, description="体重（kg）")
    exercise_habits: Optional[str] = Field(None, description="运动习惯")
    sleep_habits: Optional[str] = Field(None, description="睡眠习惯")
    work_type: Optional[str] = Field(None, description="工作性质")
    health_goals: Optional[list] = Field(None, description="健康目标")
    medical_history: Optional[str] = Field(None, description="病史记录")
    baseline_battery: Optional[Any] = Field(None, description="基线身体电量")
    sleep_pattern: Optional[Any] = Field(None, description="睡眠模式")
    work_pattern: Optional[Any] = Field(None, description="工作模式")
    health_preferences: Optional[Any] = Field(None, description="健康偏好")
    is_complete: Optional[bool] = Field(None, description="画像是否完整")


class UserProfileOut(UserProfileBase):
    """用户档案输出"""
    updated_at: datetime

    model_config = {"from_attributes": True}


# ──────────────── 聊天 ────────────────

class ChatMessageBase(BaseModel):
    """聊天消息基础 Schema"""
    user_id: str = Field(..., description="用户 ID")
    role: ChatRole = Field(..., description="角色")
    content: str = Field(..., description="消息内容")
    agent_type: Optional[str] = Field(None, description="Agent 类型标识")


class ChatMessageCreate(BaseModel):
    """发送聊天消息"""
    user_id: str = Field(..., description="用户 ID")
    content: str = Field(..., description="消息内容")
    agent_type: Optional[str] = Field("vitasleep-agent", description="Agent 类型标识")


class ChatMessageOut(ChatMessageBase):
    """聊天消息输出"""
    id: int
    created_at: datetime

    model_config = {"from_attributes": True}


class ChatHistoryQuery(BaseModel):
    """聊天历史查询参数"""
    user_id: str = Field(..., description="用户 ID")
    limit: int = Field(50, ge=1, le=500, description="返回条数限制")
    before: Optional[datetime] = Field(None, description="查询此时间之前的消息")


# ──────────────── 数据接入 ────────────────

class SignalData(BaseModel):
    """原始生理信号数据"""
    user_id: str = Field(..., description="用户 ID")
    signal_type: str = Field(..., description="信号类型（如 heart_rate, hrv, motion）")
    raw_data: Any = Field(..., description="原始数据（JSON）")
    timestamp: datetime = Field(..., description="数据采集时间")
    device_id: Optional[str] = Field(None, description="设备 ID")


class BatchSignalData(BaseModel):
    """批量信号数据"""
    user_id: str = Field(..., description="用户 ID")
    device_id: Optional[str] = Field(None, description="设备 ID")
    signals: List[SignalData] = Field(..., description="信号数据列表")


class SignalResponse(BaseModel):
    """信号数据接收响应"""
    status: str = "accepted"
    message: str = "数据已接收"
    metric_ids: List[int] = Field(default_factory=list, description="生成的指标 ID 列表")


# ──────────────── LLM 配置 ────────────────

class LLMConfigBase(BaseModel):
    """LLM 配置基础 Schema"""
    name: str = Field(..., max_length=64, description="配置名称（如 default）")
    base_url: str = Field(..., max_length=512, description="API 基础地址")
    api_key: str = Field(..., max_length=256, description="API 密钥")
    model: str = Field("glm-5.1", max_length=128, description="模型名称")
    max_tokens: int = Field(1024, ge=1, le=8192, description="最大输出 token")
    temperature: float = Field(0.7, ge=0.0, le=2.0, description="生成温度")


class LLMConfigCreate(LLMConfigBase):
    """创建 LLM 配置"""
    pass


class LLMConfigUpdate(BaseModel):
    """更新 LLM 配置（所有字段可选）"""
    name: Optional[str] = Field(None, max_length=64, description="配置名称")
    base_url: Optional[str] = Field(None, max_length=512, description="API 基础地址")
    api_key: Optional[str] = Field(None, max_length=256, description="API 密钥")
    model: Optional[str] = Field(None, max_length=128, description="模型名称")
    max_tokens: Optional[int] = Field(None, ge=1, le=8192, description="最大输出 token")
    temperature: Optional[float] = Field(None, ge=0.0, le=2.0, description="生成温度")
    is_active: Optional[bool] = Field(None, description="是否激活")


class LLMConfigOut(LLMConfigBase):
    """LLM 配置输出"""
    id: int
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


# ──────────────── Agent 工具调用 ────────────────

class AgentToolCall(BaseModel):
    """大模型工具调用请求"""
    user_id: str = Field(..., description="用户 ID")
    tool_name: str = Field(..., description="工具名称（如 query_health, query_schedule, modify_schedule）")
    arguments: dict = Field(default_factory=dict, description="工具参数")


class AgentToolResult(BaseModel):
    """大模型工具调用结果"""
    tool_name: str
    success: bool
    data: Optional[Any] = None
    error: Optional[str] = None


# ═══════════════════════════════════════════════════════════════
# Veepoo 设备数据接入 Schema
# ═══════════════════════════════════════════════════════════════

class VeepooOriginRecord(BaseModel):
    """Veepoo 5分钟原始数据的一条记录"""
    timestamp: datetime = Field(..., description="数据采集时间")
    heart_rate: int = Field(..., ge=30, le=200, description="心率值（5分钟平均）")
    heart_rate_array: Optional[List[int]] = Field(None, description="5分钟内每秒心率数组（300个）")
    systolic: int = Field(..., ge=60, le=220, description="收缩压（高压）")
    diastolic: int = Field(..., ge=40, le=150, description="舒张压（低压）")
    steps: int = Field(default=0, ge=0, description="5分钟计步数")
    sport_value: Optional[int] = Field(None, description="运动量")
    spo2: Optional[int] = Field(None, ge=70, le=100, description="血氧饱和度")
    package_number: Optional[int] = Field(None, description="数据包序号（1-based）")
    total_packages: Optional[int] = Field(None, description="该天总包数")


class VeepooOriginDataRequest(BaseModel):
    """批量上传 Veepoo 5分钟原始数据"""
    user_id: str = Field(..., description="用户 ID")
    device_id: Optional[str] = Field(None, description="设备 MAC 地址")
    device_model: Optional[str] = Field(None, description="设备型号")
    records: List[VeepooOriginRecord] = Field(..., description="5分钟原始数据记录列表")
    data_date: Optional[str] = Field(None, description="数据日期，如 2025-01-15")


class VeepooSleepStageRecord(BaseModel):
    """睡眠阶段的一条记录"""
    stage: str = Field(..., description="睡眠阶段: deep/light/rem/awake")
    start_minute: int = Field(..., ge=0, description="该阶段开始时间（距入睡的分钟数）")
    duration_min: int = Field(..., ge=0, description="该阶段持续时长（分钟）")


class VeepooSleepDataRequest(BaseModel):
    """上传 Veepoo 睡眠数据"""
    user_id: str = Field(..., description="用户 ID")
    device_id: Optional[str] = Field(None, description="设备 MAC 地址")
    sleep_date: str = Field(..., description="睡眠日期，格式 YYYY-MM-DD")
    sleep_start: datetime = Field(..., description="入睡时间")
    sleep_end: datetime = Field(..., description="起床时间")
    total_sleep_min: int = Field(..., ge=0, description="总睡眠时长（分钟）")
    deep_sleep_min: int = Field(default=0, ge=0, description="深睡时长（分钟）")
    light_sleep_min: int = Field(default=0, ge=0, description="浅睡时长（分钟）")
    rem_sleep_min: int = Field(default=0, ge=0, description="快速眼动时长（分钟）")
    awake_min: int = Field(default=0, ge=0, description="清醒时长（分钟）")
    deep_pct: float = Field(default=0.0, ge=0, le=100, description="深睡占比（%）")
    light_pct: float = Field(default=0.0, ge=0, le=100, description="浅睡占比（%）")
    rem_pct: float = Field(default=0.0, ge=0, le=100, description="快速眼动占比（%）")
    awake_pct: float = Field(default=0.0, ge=0, le=100, description="清醒占比（%）")
    quality_score: Optional[float] = Field(None, ge=0, le=100, description="睡眠质量评分")
    stages: Optional[List[VeepooSleepStageRecord]] = Field(None, description="睡眠阶段详细列表")


class VeepooBatteryRequest(BaseModel):
    """上传 Veepoo 设备电量"""
    user_id: str = Field(..., description="用户 ID")
    device_id: Optional[str] = Field(None, description="设备 MAC 地址")
    battery_level: int = Field(..., ge=0, le=100, description="设备电量（0-100%）")
    is_charging: bool = Field(default=False, description="是否正在充电")


class VeepooDeviceInfoRequest(BaseModel):
    """上传 Veepoo 设备信息"""
    user_id: str = Field(..., description="用户 ID")
    device_mac: str = Field(..., description="设备 MAC 地址")
    device_name: Optional[str] = Field(None, description="设备名称")
    device_model: Optional[str] = Field(None, description="设备型号")
    firmware_version: Optional[str] = Field(None, description="固件版本")
    protocol_version: Optional[str] = Field(None, description="协议版本")
    battery_level: Optional[int] = Field(None, ge=0, le=100, description="当前电量")


class VeepooResponse(BaseModel):
    """Veepoo API 统一响应"""
    status: str = "ok"
    message: str = "操作成功"
    records_processed: int = Field(default=0, description="处理的记录数")
    metric_ids: List[int] = Field(default_factory=list, description="生成的指标 ID 列表")
    errors: List[str] = Field(default_factory=list, description="处理中的错误列表")
