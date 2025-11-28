#!/bin/bash

# =================================================================================
# 🎮 애플리케이션 제어 스크립트 (app.sh)
# 기능: 애플리케이션 시작, 중지, 재시작, 상태 확인
# 작성일: 2025-11-28
# =================================================================================

APP_NAME="secret"
JAR_PATH="/app/libs/app.jar"
# [요청사항] 실행 로그가 쌓이는 경로
LOG_FILE="/app/logs/app.log"
PID_FILE="/app/bin/app.pid"

# ☕ 메모리 및 타임존 설정 (t3.medium - 4GB RAM 기준)
# Heap 메모리를 2GB로 설정하여 안정성 확보
JAVA_OPTS="-Xmx2048m -Duser.timezone=Asia/Seoul"

# 📂 필수 디렉토리 생성 (없으면 에러나니까 미리 생성)
mkdir -p /app/libs /app/logs /app/bin

start() {
    echo "🚀 [$APP_NAME] 시작을 시도합니다..."

    if [ -f "$PID_FILE" ]; then
        TARGET_PID=$(cat $PID_FILE)
        if kill -0 $TARGET_PID > /dev/null 2>&1; then
            echo "🚫 이미 실행 중입니다. (PID: $TARGET_PID)"
            return
        fi
    fi

    # 로그 파일 생성 (권한 문제 방지)
    if [ ! -f "$LOG_FILE" ]; then
        touch "$LOG_FILE"
    fi

    echo "📝 로그 파일 경로: $LOG_FILE"

    # [핵심] nohup으로 백그라운드 실행하며 로그 리다이렉션 (>> $LOG_FILE 2>&1)
    # 표준 출력(1)과 표준 에러(2)를 모두 app.log에 추가(append)합니다.
    nohup java $JAVA_OPTS -jar $JAR_PATH >> $LOG_FILE 2>&1 &

    NEW_PID=$!
    echo $NEW_PID > $PID_FILE
    echo "✅ 애플리케이션이 시작되었습니다! (PID: $NEW_PID)"
    echo "📜 실시간 로그 확인: tail -f $LOG_FILE"
}

stop() {
    echo "🛑 [$APP_NAME] 종료를 시도합니다..."

    if [ ! -f "$PID_FILE" ]; then
        echo "🚫 PID 파일 없음. 프로세스 검색 중..."
        EXIST_PID=$(pgrep -f "app.jar")
        if [ -n "$EXIST_PID" ]; then
            kill -15 $EXIST_PID
            echo "🔪 실행 중인 프로세스($EXIST_PID)를 종료했습니다."
            rm -f $PID_FILE
            return
        fi
        echo "🤔 실행 중인 프로세스가 없습니다."
        return
    fi

    TARGET_PID=$(cat $PID_FILE)
    kill -15 $TARGET_PID

    # 종료 대기 (5초)
    for i in {1..5}; do
        if ! kill -0 $TARGET_PID > /dev/null 2>&1; then
            break
        fi
        echo -n "."
        sleep 1
    done
    echo ""

    # 강제 종료 체크
    if kill -0 $TARGET_PID > /dev/null 2>&1; then
        echo "⚠️ 종료 지연 -> 강제 종료(kill -9)"
        kill -9 $TARGET_PID
    fi

    rm -f $PID_FILE
    echo "👋 애플리케이션이 완전히 종료되었습니다."
}

case "$1" in
    start) start ;;
    stop) stop ;;
    restart) stop; sleep 2; start ;;
    status)
        if [ -f "$PID_FILE" ] && kill -0 $(cat $PID_FILE) > /dev/null 2>&1; then
            echo "🟢 실행 중 (PID: $(cat $PID_FILE))"
        else
            echo "🔴 중지됨"
        fi
        ;;
    *) echo "사용법: $0 {start|stop|restart|status}"; exit 1 ;;
esac