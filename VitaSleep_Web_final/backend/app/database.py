"""
VitaSleep Agent V1.0 — 数据库连接配置
使用 SQLite 作为开发数据库，通过 SQLAlchemy async engine 管理连接。
"""

from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase
import os

# 数据库文件路径（项目根目录下）
DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "vitasleep.db")
DATABASE_URL = f"sqlite+aiosqlite:///{DB_PATH}"

# 创建异步引擎
engine = create_async_engine(DATABASE_URL, echo=False)

# 异步 session 工厂
async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


class Base(DeclarativeBase):
    """SQLAlchemy 声明式基类"""
    pass


async def get_db() -> AsyncSession:
    """FastAPI 依赖注入：获取异步数据库 session"""
    async with async_session() as session:
        try:
            yield session
        finally:
            await session.close()


async def init_db():
    """初始化数据库（建表）"""
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
