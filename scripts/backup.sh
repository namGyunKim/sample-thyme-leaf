#!/bin/bash

# =================================================================================
# 📦 통합 백업 스크립트 (backup.sh)
# 기능:
#   1. JAR 파일 일일 백업 (auto 폴더)
#   2. 로그 파일(app.log, error.log) 일일 백업 및 초기화 (Log Rotation)
#   3. 7일 지난 오래된 백업 파일 자동 삭제 (auto 및 manual 폴더 정리)
# 실행: Crontab 매일 00:00 (0 0 * * *)
# =================================================================================

# --- 1. 설정 변수 ---
APP_ROOT="/app"
JAR_SOURCE="$APP_ROOT/libs/app.jar"

# 디렉토리 설정
JAR_BACKUP_DIR="$APP_ROOT/backup/libs/auto"
LOG_SOURCE_DIR="$APP_ROOT/logs"
LOG_BACKUP_DIR="$APP_ROOT/backup/logs"

# 날짜 포맷 (YYYYMMDD)
TODAY=$(date +%Y%m%d)
# 파일명 충돌 방지를 위한 타임스탬프 (JAR용)
TIMESTAMP=$(date +%H%M%S)

# --- 2. 디렉토리 준비 ---
mkdir -p "$JAR_BACKUP_DIR"
mkdir -p "$LOG_BACKUP_DIR"

echo "========================================================"
echo "📦 [Backup Job] 작업 시작 : $(date)"
echo "========================================================"

# --- 3. JAR 파일 백업 (자동 백업 -> auto) ---
echo "> 🐘 JAR 파일 자동 백업 진행"
if [ -f "$JAR_SOURCE" ]; then
    cp "$JAR_SOURCE" "$JAR_BACKUP_DIR/app_daily_${TODAY}_${TIMESTAMP}.jar"
    echo "   ✅ JAR 백업 완료: $JAR_BACKUP_DIR"
else
    echo "   ⚠️ 원본 JAR 파일이 없어 건너뜁니다."
fi

# --- 4. 로그 파일 백업 및 비우기 (핵심 요청사항) ---
echo "> 📜 로그 파일 백업 및 초기화 (Rotation)"

# 로그 백업 함수
rotate_log() {
    local filename=$1
    local source_path="$LOG_SOURCE_DIR/$filename"
    local backup_path="$LOG_BACKUP_DIR/${filename}.${TODAY}"

    if [ -f "$source_path" ]; then
        # (1) 파일 복사 (백업)
        cp "$source_path" "$backup_path"

        # (2) 원본 파일 비우기 (truncate)
        truncate -s 0 "$source_path"

        echo "   ✅ $filename -> 백업 완료 & 내용 비움"
    else
        echo "   ⚠️ $filename 파일이 없어 건너뜁니다."
    fi
}

# app.log (표준출력) 백업
rotate_log "app.log"

# error.log (에러로그) 백업
rotate_log "error.log"


# --- 5. 오래된 파일 정리 (Retention: 7일) ---
echo "> 🧹 오래된 백업 정리 (7일 경과)"

# 1) 자동 백업(auto) 삭제
find "$JAR_BACKUP_DIR" -name "app_daily_*.jar" -type f -mtime +7 -delete
echo "   - 오래된 자동 백업(auto) 삭제 완료"

# 2) 로그 백업 삭제
find "$LOG_BACKUP_DIR" -type f -mtime +7 -delete
echo "   - 오래된 로그 백업 삭제 완료"

# 3) 배포 백업(manual) 삭제 (deploy.sh에서 생성된 것들)
# [수정] 경로를 manual로 변경
DEPLOY_BACKUP_DIR="$APP_ROOT/backup/libs/manual"
if [ -d "$DEPLOY_BACKUP_DIR" ]; then
    find "$DEPLOY_BACKUP_DIR" -maxdepth 1 -name "app_deploy_*.jar" -type f -mtime +7 -delete
    echo "   - 오래된 배포 백업(manual) 삭제 완료"
fi

echo "🎉 모든 작업이 완료되었습니다!"
echo "========================================================"