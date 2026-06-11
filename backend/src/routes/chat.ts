import { Router } from 'express';
import { db } from '../db/init.js';

export const chatRouter = Router();

function generateHealthResponse(message: string): string {
  const lower = message.toLowerCase();

  if (lower.includes('\u7761\u7720') || lower.includes('\u7761\u4e0d\u7740') || lower.includes('\u5931\u7720')) {
    return '\u6839\u636e\u60a8\u7684\u7761\u7720\u6570\u636e\u5206\u6790\uff0c\u5efa\u8bae\u60a8\uff1a\n1. \u4fdd\u6301\u89c4\u5f8b\u7684\u4f5c\u606f\u65f6\u95f4\uff0c\u6bcf\u5929\u540c\u4e00\u65f6\u95f4\u4e0a\u5e8a\n2. \u7761\u524d1\u5c0f\u65f6\u907f\u514d\u4f7f\u7528\u7535\u5b50\u8bbe\u5907\n3. \u4fdd\u6301\u5367\u5ba4\u6e29\u5ea6\u572818-22\u00b0C\u4e4b\u95f4\n4. \u5982\u679c\u6301\u7eed\u5931\u7720\u8d85\u8fc72\u5468\uff0c\u5efa\u8bae\u54a8\u8be2\u533b\u751f';
  }

  if (lower.includes('\u5fc3\u7387') || lower.includes('\u5fc3\u810f')) {
    return '\u5173\u4e8e\u5fc3\u7387\u5065\u5eb7\uff1a\n1. \u6b63\u5e38\u9759\u606f\u5fc3\u7387\u4e3a60-100\u6b21/\u5206\u949f\n2. \u89c4\u5f8b\u8fd0\u52a8\u53ef\u4ee5\u964d\u4f4e\u9759\u606f\u5fc3\u7387\n3. \u5982\u679c\u5fc3\u7387\u5f02\u5e38\uff08\u8fc7\u5feb/\u8fc7\u6162/\u4e0d\u89c4\u5219\uff09\uff0c\u8bf7\u53ca\u65f6\u5c31\u533b\n4. \u5efa\u8bae\u5b9a\u671f\u76d1\u6d4b\u5fc3\u7387\u53d8\u5316\u8d8b\u52bf';
  }

  if (lower.includes('\u8840\u538b')) {
    return '\u5173\u4e8e\u8840\u538b\u7ba1\u7406\uff1a\n1. \u6b63\u5e38\u8840\u538b\u8303\u56f4\uff1a\u6536\u7f29\u538b90-140mmHg\uff0c\u8212\u5f20\u538b60-90mmHg\n2. \u5efa\u8bae\u4f4e\u76d0\u996e\u98df\uff0c\u6bcf\u65e5\u98df\u76d0\u6444\u5165\u4e0d\u8d85\u8fc76g\n3. \u4fdd\u6301\u9002\u91cf\u8fd0\u52a8\uff0c\u6bcf\u5468\u81f3\u5c11150\u5206\u949f\u4e2d\u7b49\u5f3a\u5ea6\u8fd0\u52a8\n4. \u5b9a\u671f\u76d1\u6d4b\u8840\u538b\uff0c\u8bb0\u5f55\u53d8\u5316\u8d8b\u52bf';
  }

  if (lower.includes('\u75b2\u52b3') || lower.includes('\u7d2f') || lower.includes('\u4e4f\u529b')) {
    return '\u5173\u4e8e\u75b2\u52b3\u7ba1\u7406\uff1a\n1. \u786e\u4fdd\u6bcf\u665a7-9\u5c0f\u65f6\u7684\u5145\u8db3\u7761\u7720\n2. \u5408\u7406\u5b89\u6392\u5de5\u4f5c\u4e0e\u4f11\u606f\u65f6\u95f4\n3. \u9002\u5ea6\u8fd0\u52a8\u53ef\u4ee5\u63d0\u5347\u7cbe\u529b\n4. \u5982\u679c\u6301\u7eed\u75b2\u52b3\uff0c\u5efa\u8bae\u68c0\u67e5\u662f\u5426\u6709\u8d2b\u8840\u3001\u7532\u72b6\u817a\u7b49\u95ee\u9898';
  }

  if (lower.includes('\u8fd0\u52a8') || lower.includes('\u953b\u70bc') || lower.includes('\u6b65\u6570')) {
    return '\u5173\u4e8e\u8fd0\u52a8\u5efa\u8bae\uff1a\n1. \u6bcf\u5929\u5efa\u8bae\u6b65\u884c8000-10000\u6b65\n2. \u6bcf\u5468\u81f3\u5c11\u8fdb\u884c150\u5206\u949f\u4e2d\u7b49\u5f3a\u5ea6\u6709\u6c27\u8fd0\u52a8\n3. \u8fd0\u52a8\u524d\u505a\u597d\u70ed\u8eab\uff0c\u8fd0\u52a8\u540e\u6ce8\u610f\u62c9\u4f38\n4. \u6839\u636e\u8eab\u4f53\u72b6\u51b5\u9010\u6b65\u589e\u52a0\u8fd0\u52a8\u91cf';
  }

  if (lower.includes('\u8840\u6c27') || lower.includes('spo2')) {
    return '\u5173\u4e8e\u8840\u6c27\u76d1\u6d4b\uff1a\n1. \u6b63\u5e38\u8840\u6c27\u9971\u548c\u5ea6\u5e94\u572895%\u4ee5\u4e0a\n2. \u5982\u679c\u8840\u6c27\u6301\u7eed\u4f4e\u4e8e94%\uff0c\u5efa\u8bae\u5c31\u533b\n3. \u7761\u7720\u65f6\u8840\u6c27\u4e0b\u964d\u53ef\u80fd\u4e0e\u7761\u7720\u547c\u5438\u6682\u505c\u6709\u5173\n4. \u5efa\u8bae\u5728\u5b89\u9759\u72b6\u6001\u4e0b\u6d4b\u91cf\u8840\u6c27';
  }

  return '\u60a8\u597d\uff01\u6211\u662fVitaSleep\u5065\u5eb7\u52a9\u624b\uff0c\u53ef\u4ee5\u4e3a\u60a8\u63d0\u4f9b\u4ee5\u4e0b\u65b9\u9762\u7684\u5065\u5eb7\u5efa\u8bae\uff1a\n\u2022 \u7761\u7720\u7ba1\u7406\u4e0e\u6539\u5584\n\u2022 \u5fc3\u7387\u76d1\u6d4b\u4e0e\u5fc3\u810f\u5065\u5eb7\n\u2022 \u8840\u538b\u7ba1\u7406\n\u2022 \u75b2\u52b3\u6062\u590d\n\u2022 \u8fd0\u52a8\u5efa\u8bae\n\u2022 \u8840\u6c27\u76d1\u6d4b\n\n\u8bf7\u544a\u8bc9\u6211\u60a8\u60f3\u4e86\u89e3\u54ea\u65b9\u9762\u7684\u5185\u5bb9\uff1f';
}

chatRouter.post('/send', (req, res) => {
  const { user_id, content, agent_type } = req.body;
  const userId = user_id || 'test_user_001';
  const now = new Date().toISOString();

  try {
    db.prepare(
      'INSERT INTO chat_history (user_id, role, content, agent_type, created_at) VALUES (?, ?, ?, ?, ?)'
    ).run(userId, 'user', content, agent_type || null, now);

    const responseContent = generateHealthResponse(content);
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
