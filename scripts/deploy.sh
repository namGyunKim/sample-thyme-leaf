#!/bin/bash

# =================================================================================
# 🍚 자동 배포 스크립트 (deploy.sh)
# 기능: Git Pull -> Frontend Build -> (Sleep) -> Gradle Build -> Backup -> Copy -> Restart
# 변경사항:
# 1. 배포 시 백업 경로를 /app/backup/libs/manual 로 변경 (디렉토리 분리)
# 2. 실행 스크립트(app.sh)를 프로젝트 내 scripts 폴더에서 참조
# =================================================================================

PROJECT_ROOT="/home/ec2-user/src/secret"
DEPLOY_PATH="/app/libs"
# [수정] 배포 시 백업(수동/배포 성격)은 manual 디렉토리에 저장하여 auto와 분리
BACKUP_PATH="/app/backup/libs/manual"

# [수정] 기존 /app/bin/app.sh 대신 Git 프로젝트 내부의 스크립트를 바라보게 설정
APP_SCRIPT="$PROJECT_ROOT/scripts/app.sh"

JAR_NAME="app.jar"
CURRENT_TIME=$(date +%Y%m%d-%H%M%S)

echo "========================================================"
echo "🚀 배포 시작 : $CURRENT_TIME"
echo "========================================================"

# 0. 프로젝트 루트로 이동
cd $PROJECT_ROOT

# 1. Git Pull
echo "> 🐙 Git Pull 수행"
git pull

# 2. Frontend Build (경로: ./scripts/build_frontend.sh)
echo "> 🎨 Frontend Build 시작"

# 스크립트 파일 존재 확인
if [ -f "./scripts/build_frontend.sh" ]; then
    # 실행 권한 부여
    chmod +x scripts/build_frontend.sh

    # 스크립트 실행
    ./scripts/build_frontend.sh

    # 실행 결과 확인
    if [ $? -ne 0 ]; then
        echo "❌ [ERROR] 프론트엔드 빌드 실패. 배포를 중단합니다."
        exit 1
    fi
else
    echo "❌ [ERROR] 'scripts/build_frontend.sh' 파일을 찾을 수 없습니다."
    exit 1
fi

# =================================================================================
# 💤 [메모리 안정화 - 중요]
# t3.medium 환경에서 npm install 직후 바로 Gradle 빌드 시 OOM 발생 가능성 있음.
# =================================================================================
echo "💤 [메모리 안정화] 프론트엔드 빌드 후 15초간 대기합니다..."
sleep 15
echo "✨ 대기 완료. Gradle 빌드를 시작합니다."


# 3. Gradle Build
echo "> 🐘 Gradle Clean Build 시작 (Test Skip)"
chmod +x gradlew
./gradlew clean build -x test

# 빌드 실패 시 즉시 중단
if [ $? -ne 0 ]; then
    echo "❌ [ERROR] Gradle 빌드에 실패했습니다. 배포를 중단합니다."
    exit 1
fi
echo "✅ Gradle 빌드 성공!"

# 4. 기존 파일 백업 (배포 전)
echo "> 📦 백업 진행 (기존 파일이 있을 경우)"
if [ -f "$DEPLOY_PATH/$JAR_NAME" ]; then
    # [수정] 백업 디렉토리 생성 (/app/backup/libs/manual)
    mkdir -p "$BACKUP_PATH"

    # 파일명에 deploy 접두어 추가하여 구분
    cp $DEPLOY_PATH/$JAR_NAME $BACKUP_PATH/app_deploy_$CURRENT_TIME.jar
    echo "> 💾 [Deploy Backup] 백업 완료: $BACKUP_PATH/app_deploy_$CURRENT_TIME.jar"
fi

# 5. 새 파일 복사
echo "> 🚚 빌드된 JAR 파일 이동"

# 배포 경로(/app/libs)가 없는 경우 에러 방지를 위해 생성
if [ ! -d "$DEPLOY_PATH" ]; then
    echo "⚠️ 배포 디렉토리($DEPLOY_PATH)가 없어 새로 생성합니다."
    mkdir -p "$DEPLOY_PATH"
fi

cp $PROJECT_ROOT/build/libs/$JAR_NAME $DEPLOY_PATH/

# 6. 무조건 재시작
echo "> 🔄 애플리케이션 재시작 요청 (Restart)"

# [추가] 프로젝트 스크립트에 실행 권한 부여
if [ -f "$APP_SCRIPT" ]; then
    chmod +x $APP_SCRIPT
    $APP_SCRIPT restart
else
    echo "❌ [ERROR] 실행 스크립트를 찾을 수 없습니다: $APP_SCRIPT"
    echo "Git Repository의 scripts 폴더에 app.sh가 존재하는지 확인해주세요."
    exit 1
fi

echo "========================================================"
echo "🎉 배포가 완료되었습니다!"
echo "========================================================"