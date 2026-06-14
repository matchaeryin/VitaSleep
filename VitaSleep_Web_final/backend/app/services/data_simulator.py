# -*- coding: utf-8 -*-
"""
数据模拟器 + 算法定时计算服务

每 5 秒执行一次：
  1. 生成模拟原始生理信号（心率、HRV 等）
  2. 调用算法引擎计算身体电量、心血管指数等
  3. 将计算结果写入 health_metrics 表
"""

import asyncio
import logging
import random
import sys
import os
from datetime import datetime, timedelta

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'algorithm'))

from app.database import async_session
from app.models.models import HealthMetric, MetricType
from app.services.data_service import ingest_metric

logger = logging.getLogger("vitasleep.simulator")

BASE_HEART_RATE = 72
BASE_HRV_RMSSD = 45
BASE_SYSTOLIC = 118
BASE_DIASTOLIC = 76

USER_ID = "test_user_001"

_simulator_task = None
_running = False


def _hour_factor() -> dict:
    hour = datetime.now().hour
    if 0 <= hour < 7:
        return {"hr": -8, "hrv": 10, "sys": -5, "dia": -3, "activity": 0.05, "sleep_q": 85}
    elif 7 <= hour < 9:
        return {"hr": 2, "hrv": 0, "sys": 3, "dia": 2, "activity": 0.3, "sleep_q": 70}
    elif 9 <= hour < 12:
        return {"hr": 5, "hrv": -5, "sys": 5, "dia": 3, "activity": 0.35, "sleep_q": 60}
    elif 12 <= hour < 14:
        return {"hr": -2, "hrv": 3, "sys": -2, "dia": -1, "activity": 0.15, "sleep_q": 65}
    elif 14 <= hour < 18:
        return {"hr": 6, "hrv": -8, "sys": 6, "dia": 4, "activity": 0.4, "sleep_q": 50}
    elif 18 <= hour < 21:
        return {"hr": 0, "hrv": 2, "sys": 0, "dia": 0, "activity": 0.25, "sleep_q": 55}
    else:
        return {"hr": -5, "hrv": 5, "sys": -3, "dia": -2, "activity": 0.1, "sleep_q": 75}


def _generate_heart_rate() -> dict:
    f = _hour_factor()
    bpm = BASE_HEART_RATE + f["hr"] + random.gauss(0, 3)
    bpm = max(50, min(120, round(bpm)))
    return {"bpm": bpm, "confidence": round(random.uniform(0.85, 0.98), 2)}


def _generate_hrv() -> dict:
    f = _hour_factor()
    rmssd = BASE_HRV_RMSSD + f["hrv"] + random.gauss(0, 5)
    rmssd = max(15, min(80, round(rmssd, 1)))
    sdnn = rmssd * random.uniform(1.1, 1.4)
    return {
        "rmssd": rmssd,
        "sdnn": round(sdnn, 1),
        "pnn50": round(min(60, max(5, rmssd - 20 + random.gauss(0, 3))), 1),
        "score": round(min(100, max(10, rmssd * 1.5 + 10)), 1),
    }


def _generate_blood_pressure() -> dict:
    f = _hour_factor()
    systolic = BASE_SYSTOLIC + f["sys"] + random.gauss(0, 4)
    diastolic = BASE_DIASTOLIC + f["dia"] + random.gauss(0, 3)
    systolic = max(95, min(150, round(systolic)))
    diastolic = max(55, min(100, round(diastolic)))
    return {
        "systolic": systolic,
        "diastolic": diastolic,
        "mean_arterial": round(diastolic + (systolic - diastolic) / 3),
    }


def _generate_spo2() -> dict:
    hour = datetime.now().hour
    if 0 <= hour < 7:
        base = 94 + random.gauss(0, 1.5)
    else:
        base = 97 + random.gauss(0, 1.0)
    spo2 = max(88, min(100, round(base)))
    if spo2 >= 95:
        status = "正常"
    elif spo2 >= 90:
        status = "偏低"
    else:
        status = "偏低"
    return {"spo2": spo2, "status": status}


def _compute_battery(hrv_score: float, activity: float) -> dict:
    f = _hour_factor()
    hour = datetime.now().hour
    if 0 <= hour < 7:
        base = 60 + (hour / 7) * 30
    elif 7 <= hour < 12:
        base = 90 - (hour - 7) * 8
    elif 12 <= hour < 14:
        base = 55 + (hour - 12) * 10
    elif 14 <= hour < 18:
        base = 75 - (hour - 14) * 8
    else:
        base = 45 - (hour - 18) * 3

    level = max(10, min(100, round(base + random.gauss(0, 3))))

    if level >= 80:
        status = "充沛"
        trend = "rising"
    elif level >= 60:
        status = "正常"
        trend = "stable"
    elif level >= 40:
        status = "偏低"
        trend = "declining"
    else:
        status = "recovery"
        trend = "declining"

    return {"level": level, "status": status, "trend": trend}


def _compute_cardio_index(hrv_score: float, systolic: int, diastolic: int, hr: int) -> dict:
    hr_score = max(0, 100 - abs(hr - 65) * 2)
    bp_score = max(0, 100 - abs(systolic - 115) - abs(diastolic - 75))
    hrv_score_norm = min(100, hrv_score)
    score = round(hr_score * 0.25 + bp_score * 0.35 + hrv_score_norm * 0.4)

    if score >= 80:
        risk = "low"
    elif score >= 60:
        risk = "moderate"
    else:
        risk = "high"

    cardio_age = 30 + (100 - score) // 5

    return {
        "score": score,
        "risk_level": risk,
        "cardio_age": cardio_age,
        "summary": f"心血管指数 {score}/100，风险等级：{risk}",
    }


async def _run_one_cycle():
    now = datetime.utcnow()

    hr_data = _generate_heart_rate()
    hrv_data = _generate_hrv()
    bp_data = _generate_blood_pressure()
    spo2_data = _generate_spo2()

    f = _hour_factor()
    battery_data = _compute_battery(hrv_data["score"], f["activity"])
    cardio_data = _compute_cardio_index(
        hrv_data["score"], bp_data["systolic"], bp_data["diastolic"], hr_data["bpm"]
    )

    async with async_session() as db:
        try:
            await ingest_metric(db, USER_ID, MetricType.heart_rate, hr_data, now)
            await ingest_metric(db, USER_ID, MetricType.hrv, hrv_data, now)
            await ingest_metric(db, USER_ID, MetricType.blood_pressure, bp_data, now)
            await ingest_metric(db, USER_ID, MetricType.battery, battery_data, now)
            await ingest_metric(db, USER_ID, MetricType.cardio_index, cardio_data, now)
            await ingest_metric(db, USER_ID, MetricType.spo2, spo2_data, now)

            if 0 <= now.hour < 7:
                sleep_data = {
                    "stages": [
                        {"stage": "deep", "duration_min": random.randint(60, 120)},
                        {"stage": "light", "duration_min": random.randint(120, 200)},
                        {"stage": "rem", "duration_min": random.randint(40, 90)},
                        {"stage": "awake", "duration_min": random.randint(10, 40)},
                    ],
                    "sleep_hours": round(random.uniform(6, 8.5), 1),
                    "deep_pct": round(random.uniform(15, 25), 1),
                    "light_pct": round(random.uniform(45, 55), 1),
                    "rem_pct": round(random.uniform(15, 25), 1),
                    "awake_pct": round(random.uniform(3, 10), 1),
                    "quality_score": round(random.uniform(65, 90), 1),
                }
                await ingest_metric(db, USER_ID, MetricType.sleep_stage, sleep_data, now)

            logger.info(f"[模拟器] 数据生成完成: HR={hr_data['bpm']} HRV={hrv_data['rmssd']} "
                       f"BP={bp_data['systolic']}/{bp_data['diastolic']} "
                       f"SpO2={spo2_data['spo2']}% "
                       f"电量={battery_data['level']}% 心血管={cardio_data['score']}")
        except Exception as e:
            logger.error(f"[模拟器] 数据写入失败: {e}")
            await db.rollback()


async def _simulator_loop():
    logger.info("[模拟器] 启动，每 5 秒生成一次数据")
    while _running:
        try:
            await _run_one_cycle()
        except Exception as e:
            logger.error(f"[模拟器] 循环异常: {e}")
        await asyncio.sleep(5)


def start_simulator():
    global _simulator_task, _running
    if _running:
        return
    _running = True
    _simulator_task = asyncio.get_event_loop().create_task(_simulator_loop())
    logger.info("[模拟器] 已启动")


def stop_simulator():
    global _running, _simulator_task
    _running = False
    if _simulator_task:
        _simulator_task.cancel()
        _simulator_task = None
    logger.info("[模拟器] 已停止")
