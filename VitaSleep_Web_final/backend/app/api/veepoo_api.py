"""
VitaSleep Agent V1.0 — Veepoo 设备数据接入 API
"""

import logging
from datetime import datetime
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.models import MetricType
from app.schemas import VeepooOriginDataRequest, VeepooSleepDataRequest, VeepooResponse
from app.services.data_service import ingest_metric

logger = logging.getLogger("vitasleep.api.veepoo")
router = APIRouter(prefix="/api/data/veepoo", tags=["Veepoo 设备接入"])

@router.post("/origin5min", response_model=VeepooResponse, status_code=202)
async def upload_origin5min(req: VeepooOriginDataRequest, db: AsyncSession = Depends(get_db)):
    logger.info(f"[Veepoo] 接收5分钟数据: user={req.user_id}, records={len(req.records)}")
    all_metric_ids: List[int] = []
    errors: List[str] = []

    for i, rec in enumerate(req.records):
        try:
            computed_at = rec.timestamp
            if rec.heart_rate is not None:
                hr_value = {"bpm": rec.heart_rate, "source": "veepoo", "device_id": req.device_id}
                metric_hr = await ingest_metric(db, req.user_id, MetricType.heart_rate, hr_value, computed_at)
                all_metric_ids.append(metric_hr.id)

            if rec.systolic is not None and rec.diastolic is not None:
                bp_value = {"systolic": rec.systolic, "diastolic": rec.diastolic, "source": "veepoo"}
                metric_bp = await ingest_metric(db, req.user_id, MetricType.blood_pressure, bp_value, computed_at)
                all_metric_ids.append(metric_bp.id)

            if rec.spo2 is not None:
                spo2_value = {"spo2": rec.spo2, "source": "veepoo", "device_id": req.device_id}
                metric_spo2 = await ingest_metric(db, req.user_id, MetricType.spo2, spo2_value, computed_at)
                all_metric_ids.append(metric_spo2.id)
        except Exception as e:
            errors.append(f"第{i+1}条失败: {str(e)}")

    await db.commit()
    logger.info(f"[Veepoo] 处理完成: 成功={len(all_metric_ids)}, 失败={len(errors)}")
    return VeepooResponse(status="partial" if errors else "ok", message="处理完成", records_processed=len(req.records), metric_ids=all_metric_ids, errors=errors)

@router.post("/sleep", response_model=VeepooResponse, status_code=202)
async def upload_sleep(req: VeepooSleepDataRequest, db: AsyncSession = Depends(get_db)):
    logger.info(f"[Veepoo] 接收睡眠数据: user={req.user_id}, date={req.sleep_date}")
    try:
        sleep_value = {
            "sleep_date": req.sleep_date,
            "total_sleep_hours": round(req.total_sleep_min / 60, 1),
            "deep_pct": req.deep_pct,
            "light_pct": req.light_pct,
            "rem_pct": req.rem_pct,
            "awake_pct": req.awake_pct,
            "quality_score": req.quality_score or 0,
            "source": "veepoo",
        }
        computed_at = datetime.strptime(req.sleep_date, "%Y-%m-%d") if req.sleep_date else datetime.utcnow()
        metric = await ingest_metric(db, req.user_id, MetricType.sleep_stage, sleep_value, computed_at)
        await db.commit()
        return VeepooResponse(status="ok", message="睡眠数据写入成功", records_processed=1, metric_ids=[metric.id])
    except Exception as e:
        await db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/sync", response_model=VeepooResponse, status_code=202)
async def sync_all(req: VeepooOriginDataRequest, db: AsyncSession = Depends(get_db)):
    return await upload_origin5min(req, db)
