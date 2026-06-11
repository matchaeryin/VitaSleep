"""
VitaSleep Agent V1.0 — 数据接入 API
接收原始生理信号数据，经过处理后写入 health_metrics 表。
"""

import logging
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.models import MetricType
from app.schemas import SignalData, SignalResponse, ErrorResponse, BatchSignalData
from app.services.data_service import ingest_metric

logger = logging.getLogger("vitasleep.api.data")

router = APIRouter(prefix="/api/data", tags=["数据接入"])


@router.post(
    "/signal",
    response_model=SignalResponse,
    summary="接收原始生理信号数据",
    status_code=202,
    responses={400: {"model": ErrorResponse}},
)
async def receive_signal(
    signal: SignalData,
    db: AsyncSession = Depends(get_db),
):
    logger.info(
        f"接收信号数据: user_id={signal.user_id}, "
        f"type={signal.signal_type}, timestamp={signal.timestamp}"
    )

    metric_ids: List[int] = []

    try:
        if signal.signal_type in ("heart_rate", "hrv"):
            metric = await ingest_metric(
                db=db, user_id=signal.user_id,
                metric_type=MetricType(signal.signal_type),
                value=signal.raw_data, computed_at=signal.timestamp,
            )
            metric_ids.append(metric.id)

        elif signal.signal_type == "blood_pressure":
            metric = await ingest_metric(
                db=db, user_id=signal.user_id,
                metric_type=MetricType.blood_pressure,
                value=signal.raw_data, computed_at=signal.timestamp,
            )
            metric_ids.append(metric.id)

        elif signal.signal_type == "sleep_stage":
            metric = await ingest_metric(
                db=db, user_id=signal.user_id,
                metric_type=MetricType.sleep_stage,
                value=signal.raw_data, computed_at=signal.timestamp,
            )
            metric_ids.append(metric.id)

        elif signal.signal_type == "battery":
            metric = await ingest_metric(
                db=db, user_id=signal.user_id,
                metric_type=MetricType.battery,
                value=signal.raw_data, computed_at=signal.timestamp,
            )
            metric_ids.append(metric.id)

        elif signal.signal_type == "cardio_index":
            metric = await ingest_metric(
                db=db, user_id=signal.user_id,
                metric_type=MetricType.cardio_index,
                value=signal.raw_data, computed_at=signal.timestamp,
            )
            metric_ids.append(metric.id)

        elif signal.signal_type == "multi":
            if not isinstance(signal.raw_data, list):
                raise HTTPException(
                    status_code=400,
                    detail="multi 类型信号的 raw_data 必须为列表"
                )
            for item in signal.raw_data:
                if "metric_type" not in item or "value" not in item:
                    continue
                metric = await ingest_metric(
                    db=db,
                    user_id=signal.user_id,
                    metric_type=MetricType(item["metric_type"]),
                    value=item["value"],
                    computed_at=signal.timestamp,
                )
                metric_ids.append(metric.id)

        else:
            logger.warning(f"未知信号类型: {signal.signal_type}")
            raise HTTPException(
                status_code=400,
                detail=f"不支持的信号类型: {signal.signal_type}"
            )

    except ValueError as e:
        raise HTTPException(status_code=400, detail=f"数据解析错误: {str(e)}")

    logger.info(f"信号处理完成: 生成 {len(metric_ids)} 条指标记录")
    return SignalResponse(
        status="accepted",
        message=f"数据处理完成，生成 {len(metric_ids)} 条指标",
        metric_ids=metric_ids,
    )


@router.post(
    "/batch",
    response_model=SignalResponse,
    summary="批量接收生理信号数据",
    status_code=202,
    responses={400: {"model": ErrorResponse}},
)
async def receive_batch(
    batch: BatchSignalData,
    db: AsyncSession = Depends(get_db),
):
    logger.info(
        f"接收批量信号: user_id={batch.user_id}, count={len(batch.signals)}"
    )

    all_ids: List[int] = []
    errors = []

    for i, signal in enumerate(batch.signals):
        try:
            fake_signal = SignalData(
                user_id=batch.user_id,
                signal_type=signal.signal_type,
                raw_data=signal.raw_data,
                timestamp=signal.timestamp,
                device_id=batch.device_id or signal.device_id,
            )
            result = await receive_signal(fake_signal, db)
            all_ids.extend(result.metric_ids)
        except Exception as e:
            errors.append(f"第{i+1}条: {str(e)}")
            logger.warning(f"批量信号第{i+1}条处理失败: {e}")

    msg = f"批量处理完成: {len(all_ids)} 条成功, {len(errors)} 条失败"
    return SignalResponse(
        status="accepted" if not errors else "partial",
        message=msg,
        metric_ids=all_ids,
    )
