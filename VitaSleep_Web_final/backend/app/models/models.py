"""
VitaSleep Agent V1.0 — 数据库模型定义
包含 4 张核心表：health_metrics, schedules, user_profiles, chat_history
"""

import enum
import json
from datetime import datetime
from sqlalchemy import (
    Column, Integer, String, Text, DateTime, Enum, ForeignKey, JSON,
    Float, Date, Boolean
)
from sqlalchemy.types import TypeDecorator
from app.database import Base


# ──────────────── 枚举类型 ────────────────

class MetricType(str, enum.Enum):
    """健康指标类型"""
    battery = "battery"                # 身体电量
    blood_pressure = "blood_pressure"  # 血压
    cardio_index = "cardio_index"      # 心血管指数
    sleep_stage = "sleep_stage"        # 睡眠阶段
    heart_rate = "heart_rate"          # 心率
    hrv = "hrv"                        # 心率变异性


class EventType(str, enum.Enum):
    """日程事件类型"""
    fixed = "fixed"                      # 固定日程
    flexible = "flexible"                # 弹性日程
    health_intervention = "health_intervention"  # 健康干预


class ScheduleSource(str, enum.Enum):
    """日程来源"""
    user_manual = "user_manual"        # 用户手动创建
    agent_generated = "agent_generated"  # Agent 自动生成
    calendar_sync = "calendar_sync"    # 日历同步


class ScheduleStatus(str, enum.Enum):
    """日程状态"""
    pending = "pending"        # 待执行
    completed = "completed"    # 已完成
    cancelled = "cancelled"    # 已取消


class ChatRole(str, enum.Enum):
    """聊天角色"""
    user = "user"              # 用户
    assistant = "assistant"    # AI 助手
    system = "system"          # 系统消息


# ──────────────── JSON 类型适配器（SQLite 兼容） ────────────────

class JSONEncoded(TypeDecorator):
    """将 Python 对象序列化为 JSON 字符串存储，读取时自动反序列化。SQLite 兼容。"""
    impl = Text
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is not None:
            return json.dumps(value, ensure_ascii=False)
        return None

    def process_result_value(self, value, dialect):
        if value is not None:
            return json.loads(value)
        return None


# ──────────────── 表模型 ────────────────

class HealthMetric(Base):
    """健康指标表：存储各类生理数据的时序记录"""
    __tablename__ = "health_metrics"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="主键 ID")
    user_id = Column(String(64), nullable=False, index=True, comment="用户 ID")
    metric_type = Column(Enum(MetricType), nullable=False, index=True, comment="指标类型")
    value = Column(JSONEncoded, nullable=False, comment="指标值（JSON 格式）")
    computed_at = Column(DateTime, nullable=False, index=True, comment="指标计算时间")
    valid_until = Column(DateTime, nullable=True, comment="指标有效期至")

    def __repr__(self):
        return f"<HealthMetric(id={self.id}, user_id={self.user_id}, type={self.metric_type})>"


class Schedule(Base):
    """日程表：管理用户的日程事件"""
    __tablename__ = "schedules"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="主键 ID")
    user_id = Column(String(64), nullable=False, index=True, comment="用户 ID")
    title = Column(String(256), nullable=False, comment="事件标题")
    event_type = Column(Enum(EventType), nullable=False, default=EventType.flexible, comment="事件类型")
    start_time = Column(DateTime, nullable=False, comment="开始时间")
    end_time = Column(DateTime, nullable=False, comment="结束时间")
    source = Column(Enum(ScheduleSource), nullable=False, default=ScheduleSource.user_manual, comment="来源")
    status = Column(Enum(ScheduleStatus), nullable=False, default=ScheduleStatus.pending, comment="状态")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, comment="创建时间")
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow, comment="更新时间")

    def __repr__(self):
        return f"<Schedule(id={self.id}, title={self.title}, status={self.status})>"


class UserProfile(Base):
    """用户档案表：存储用户的基本信息、健康基线与偏好设置"""
    __tablename__ = "user_profiles"

    user_id = Column(String(64), primary_key=True, comment="用户 ID（主键）")

    # 基本身体信息
    age = Column(Integer, nullable=True, comment="年龄")
    gender = Column(String(10), nullable=True, comment="性别")
    height = Column(Float, nullable=True, comment="身高（cm）")
    weight = Column(Float, nullable=True, comment="体重（kg）")

    # 生活习惯
    exercise_habits = Column(Text, nullable=True, comment="运动习惯")
    sleep_habits = Column(Text, nullable=True, comment="睡眠习惯")
    work_type = Column(String(100), nullable=True, comment="工作性质")

    # 健康目标与病史
    health_goals = Column(JSONEncoded, nullable=True, comment="健康目标（JSON 数组）")
    medical_history = Column(Text, nullable=True, comment="病史记录")

    # 原有字段
    baseline_battery = Column(JSONEncoded, nullable=True, comment="基线身体电量（JSON）")
    sleep_pattern = Column(JSONEncoded, nullable=True, comment="睡眠模式（JSON）")
    work_pattern = Column(JSONEncoded, nullable=True, comment="工作模式（JSON）")
    health_preferences = Column(JSONEncoded, nullable=True, comment="健康偏好（JSON）")

    # 完成标记
    is_complete = Column(Boolean, nullable=False, default=False, comment="画像是否完整")

    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow, comment="更新时间")

    def __repr__(self):
        return f"<UserProfile(user_id={self.user_id}, complete={self.is_complete})>"


class LLMConfig(Base):
    """大模型配置表：存储 LLM API 连接参数，支持动态切换模型"""
    __tablename__ = "llm_config"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="主键 ID")
    name = Column(String(64), nullable=False, unique=True, comment="配置名称（如 default）")
    base_url = Column(String(512), nullable=False, comment="API 基础地址")
    api_key = Column(String(256), nullable=False, comment="API 密钥")
    model = Column(String(128), nullable=False, default="glm-5.1", comment="模型名称")
    max_tokens = Column(Integer, nullable=False, default=1024, comment="最大输出 token")
    temperature = Column(Float, nullable=False, default=0.7, comment="生成温度")
    is_active = Column(Boolean, nullable=False, default=True, comment="是否为当前激活配置")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, comment="创建时间")
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow, comment="更新时间")

    def __repr__(self):
        return f"<LLMConfig(id={self.id}, name={self.name}, model={self.model}, active={self.is_active})>"


class ChatHistory(Base):
    """聊天记录表：存储用户与 Agent 的对话历史"""
    __tablename__ = "chat_history"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="主键 ID")
    user_id = Column(String(64), nullable=False, index=True, comment="用户 ID")
    role = Column(Enum(ChatRole), nullable=False, comment="角色")
    content = Column(Text, nullable=False, comment="消息内容")
    agent_type = Column(String(64), nullable=True, comment="Agent 类型标识")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, comment="创建时间")

    def __repr__(self):
        return f"<ChatHistory(id={self.id}, user_id={self.user_id}, role={self.role})>"
