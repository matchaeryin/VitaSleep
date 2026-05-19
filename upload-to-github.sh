#!/bin/bash

# VitaSleep V2 GitHub 上传脚本

REPO_URL="https://github.com/matchaeryin/VitaSleep-前后端.git"
# 或者使用英文仓库名：
# REPO_URL="https://github.com/matchaeryin/VitaSleep.git"

echo "╔════════════════════════════════════════════════╗"
echo "║  🚀 VitaSleep V2 上传到 GitHub                  ║"
echo "╚════════════════════════════════════════════════╝"
echo ""

# 1. 在 GitHub 上创建仓库（需要手动）
echo "📝 第一步：在 GitHub 上创建仓库"
echo ""
echo "请访问：https://github.com/new"
echo "仓库名称：VitaSleep-前后端"
echo "或者：VitaSleep"
echo ""
echo "✅ 创建完成后，按回车继续..."
read

# 2. 初始化 Git
echo ""
echo "📦 第二步：初始化 Git 仓库..."
git init
git config user.name "matchaeryin"
git config user.email "matchaeryin@users.noreply.github.com"

# 3. 添加文件
echo ""
echo "📦 第三步：添加文件..."
git add -A

# 4. 提交
echo ""
echo "📦 第四步：提交更改..."
git commit -m "feat: VitaSleep V2 - 对接 Google Health + 算法服务

- Google Health API 集成
- 数据转换服务（MetricPayload 格式）
- 算法服务对接（/home/admin/Desktop/Vita/算法）
- 指标计算：电量/疲劳/睡眠/心血管
- 数据库持久化
- 前端界面

算法服务启动：
cd /home/admin/Desktop/Vita/算法
uvicorn main:app --reload --port 8001

后端启动：
cd backend
npm install && npm run dev

前端启动：
cd frontend
npm install && npm run dev"

# 5. 重命名分支
echo ""
echo "📦 第五步：设置主分支..."
git branch -M main

# 6. 添加远程仓库
echo ""
echo "📦 第六步：添加远程仓库..."
echo "请输入你的 GitHub 仓库 URL："
echo "例如：https://github.com/matchaeryin/VitaSleep.git"
read REPO_URL
git remote add origin $REPO_URL

# 7. 推送
echo ""
echo "📦 第七步：推送到 GitHub..."
echo "请输入 GitHub 用户名和密码（或 Personal Access Token）："
git push -u origin main

echo ""
echo "╔════════════════════════════════════════════════╗"
echo "║  ✅ 上传完成！                                  ║"
echo "╚════════════════════════════════════════════════╝"
echo ""
echo "仓库地址：$REPO_URL"
echo ""
