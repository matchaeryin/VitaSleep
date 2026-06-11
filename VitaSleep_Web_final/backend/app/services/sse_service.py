"""
VitaSleep Agent V1.0 — SSE 推送服务
管理 Server-Sent Events 连接，支持向特定用户推送实时数据。
"""

import asyncio
import json
import logging
from datetime import datetime
from typing import Dict, Set, Optional, Any

logger = logging.getLogger("vitasleep.sse")

class SSEManager:
    def __init__(self):
        self._connections: Dict[str, Set[asyncio.Queue]] = {}
        self._heartbeat_interval = 30
        self._lock = asyncio.Lock()

    async def connect(self, user_id: str) -> asyncio.Queue:
        queue = asyncio.Queue()
        async with self._lock:
            if user_id not in self._connections:
                self._connections[user_id] = set()
            self._connections[user_id].add(queue)
        logger.info(f"SSE 连接建立: user_id={user_id}")
        return queue

    async def disconnect(self, user_id: str, queue: asyncio.Queue):
        async with self._lock:
            if user_id in self._connections:
                self._connections[user_id].discard(queue)
                if not self._connections[user_id]:
                    del self._connections[user_id]
        logger.info(f"SSE 连接断开: user_id={user_id}")

    async def push_to_user(self, user_id: str, event: str, data: Any):
        async with self._lock:
            queues = list(self._connections.get(user_id, set()))

        message = {
            "event": event,
            "data": data,
            "timestamp": datetime.utcnow().isoformat(),
        }
        payload = json.dumps(message, ensure_ascii=False)

        for queue in queues:
            try:
                await queue.put(payload)
            except Exception as e:
                logger.warning(f"推送消息失败: {e}")

    async def heartbeat_loop(self, user_id: str, queue: asyncio.Queue):
        try:
            while True:
                await asyncio.sleep(self._heartbeat_interval)
                heartbeat_msg = json.dumps({
                    "event": "heartbeat",
                    "data": {"status": "alive"},
                    "timestamp": datetime.utcnow().isoformat(),
                })
                await queue.put(heartbeat_msg)
        except asyncio.CancelledError:
            pass

sse_manager = SSEManager()
