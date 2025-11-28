#!/bin/bash

echo "========================================================"
echo "🎨 프론트엔드(Tailwind CSS) 빌드 시작"
echo "========================================================"

# Node.js 설치 여부 확인 (없으면 에러 메시지)
if ! command -v npm &> /dev/null; then
    echo "❌ [ERROR] npm이 설치되어 있지 않습니다."
    echo "서버에서 'sudo dnf install nodejs -y'를 실행하여 Node.js를 설치해주세요."
    exit 1
fi

# 1. 의존성 설치 (package.json 기반)
echo "> 📦 npm install 실행"
npm install
if [ $? -ne 0 ]; then
    echo "❌ [ERROR] npm install 실패"
    exit 1
fi

# 2. CSS 빌드 (tailwind.config.js 기반)
echo "> 🔨 Tailwind CSS 빌드 실행"
npm run build:css
if [ $? -ne 0 ]; then
    echo "❌ [ERROR] CSS 빌드 실패"
    exit 1
fi

echo "✅ 프론트엔드 빌드 완료!"