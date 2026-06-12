import { Router } from 'express';
import { db } from '../db/init.js';
import OpenAI from 'openai';

export const chatRouter = Router();

const client = new OpenAI({
  apiKey: 'b67e4c585b0a48d9a9eefbb391d0826c.ohwZG7dDVRNcgO9j',
  baseURL: 'https://open.bigmodel.cn/api/coding/paas/v4',
});

const SYSTEM_PROMPT = `你是 VitaSleep 健康助手，一位专业、温暖、关心用户的健康顾问。

你的职责：
- 基于用户的健康数据（心率、血压、睡眠、血氧、身体电量等）提供个性化的健康建议
- 回答用户关于健康、睡眠、运动等方面的问题
- 用通俗易懂的语言解释健康数据的意义
- 在发现异常数据时提醒用户关注

注意事项：
- 你不是医生，遇到严重健康问题应建议用户就医
- 回复要简洁友好，使用中文
- 如果有用户的健康数据，请结合数据给出针对性建议
- 适当使用emoji让回复更亲切`;

function generateHealthResponse(message: string): string {
  const lower = message.toLowerCase();

  if (lower.includes('睡眠') || lower.includes('睡不着') || lower.includes('失眠')) {
    return '根据您的睡眠数据分析，建议您：\n1. 保持规律的作息时间，每天同一时间上床\n2. 睡前1小时避免使用电子设备\n3. 保持卧室温度在18-22°C之间\n4. 如果持续失眠超过2周，建议咨询医生';
  }

  if (lower.includes('心率') || lower.includes('心脏')) {
    return '关于心率健康：\n1. 正常静息心率为60-100次/分钟\n2. 规律运动可以降低静息心率\n3. 如果心率异常（过快/过慢/不规则），请及时就医\n4. 建议定期监测心率变化趋势';
  }

  if (lower.includes('血压')) {
    return '关于血压管理：\n1. 正常血压范围：收缩压90-140mmHg，舒张压60-90mmHg\n2. 建议低盐饮食，每日食盐摄入不超过6g\n3. 保持适量运动，每周至少150分钟中等强度运动\n4. 定期监测血压，记录变化趋势';
  }

  if (lower.includes('疲劳') || lower.includes('累') || lower.includes('乏力')) {
    return '关于疲劳管理：\n1. 确保每晚7-9小时的充足睡眠\n2. 合理安排工作与休息时间\n3. 适度运动可以提升精力\n4. 如果持续疲劳，建议检查是否有贫血、甲状腺等问题';
  }

  if (lower.includes('运动') || lower.includes('锻炼') || lower.includes('步数')) {
    return '关于运动建议：\n1. 每天建议步行8000-10000步\n2. 每周至少进行150分钟中等强度有氧运动\n3. 运动前做好热身，运动后注意拉伸\n4. 根据身体状况逐步增加运动量';
  }

  if (lower.includes('血氧') || lower.includes('spo2')) {
    return '关于血氧监测：\n1. 正常血氧饱和度应在95%以上\n2. 如果血氧持续低于94%，建议就医\n3. 睡眠时血氧下降可能与睡眠呼吸暂停有关\n4. 建议在安静状态下测量血氧';
  }

  return '您好！我是VitaSleep健康助手，可以为您提供以下方面的健康建议：\n• 睡眠管理与改善\n• 心率监测与心脏健康\n• 血压管理\n• 疲劳恢复\n• 运动建议\n• 血氧监测\n\n请告诉我您想了解哪方面的内容？';
}

function buildHealthContext(userId: string): string {
  try {
    const types = db.prepare(
      "SELECT DISTINCT metric_type FROM health_metrics WHERE user_id = ?"
    ).all(userId) as any[];

    if (types.length === 0) return '';

    const parts: string[] = ['当前用户的最新健康数据：'];
    for (const { metric_type } of types) {
      const row = db.prepare(
        "SELECT value, computed_at FROM health_metrics WHERE user_id = ? AND metric_type = ? ORDER BY computed_at DESC LIMIT 1"
      ).get(userId, metric_type) as any;

      if (row) {
        const value = JSON.parse(row.value);
        const time = row.computed_at;
        parts.push(`- ${metric_type}: ${JSON.stringify(value)} (更新时间: ${time})`);
      }
    }
    return parts.join('\n');
  } catch {
    return '';
  }
}

chatRouter.post('/send', async (req, res) => {
  const { user_id, content, agent_type } = req.body;
  const userId = user_id || 'test_user_001';
  const now = new Date().toISOString();

  try {
    db.prepare(
      'INSERT INTO chat_history (user_id, role, content, agent_type, created_at) VALUES (?, ?, ?, ?, ?)'
    ).run(userId, 'user', content, agent_type || null, now);

    let responseContent: string;

    try {
      const healthContext = buildHealthContext(userId);

      const historyRows = db.prepare(
        'SELECT role, content FROM chat_history WHERE user_id = ? ORDER BY created_at DESC LIMIT 10'
      ).all(userId) as any[];

      const messages: any[] = [
        { role: 'system', content: SYSTEM_PROMPT },
      ];

      if (healthContext) {
        messages.push({ role: 'system', content: healthContext });
      }

      const historyMessages = historyRows
        .reverse()
        .map((row: any) => ({
          role: row.role as 'user' | 'assistant',
          content: row.content,
        }))
        .filter((m: any) => m.role === 'user' || m.role === 'assistant');

      messages.push(...historyMessages);

      const completion = await client.chat.completions.create({
        model: 'glm-4-flash',
        messages,
        temperature: 0.7,
        max_tokens: 1024,
      });

      responseContent = completion.choices[0]?.message?.content || generateHealthResponse(content);
    } catch (llmError: any) {
      console.error('[Chat] LLM error, falling back to rule-based:', llmError.message);
      responseContent = generateHealthResponse(content);
    }

    const result = db.prepare(
      'INSERT INTO chat_history (user_id, role, content, agent_type, created_at) VALUES (?, ?, ?, ?, ?)'
    ).run(userId, 'assistant', responseContent, 'vitasleep-agent', now);

    res.json({
      id: Number(result.lastInsertRowid),
      user_id: userId,
      role: 'assistant',
      content: responseContent,
      agent_type: 'vitasleep-agent',
      created_at: now,
    });
  } catch (error: any) {
    console.error('[Chat] send error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

chatRouter.get('/history', (req, res) => {
  const userId = (req.query.user_id as string) || 'test_user_001';
  const limit = parseInt(req.query.limit as string) || 50;

  try {
    const rows = db.prepare(
      'SELECT * FROM chat_history WHERE user_id = ? ORDER BY created_at DESC LIMIT ?'
    ).all(userId, limit) as any[];

    const result = rows.map(row => ({
      id: row.id,
      user_id: row.user_id,
      role: row.role,
      content: row.content,
      agent_type: row.agent_type,
      created_at: row.created_at,
    }));

    res.json(result);
  } catch (error: any) {
    console.error('[Chat] history error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});
