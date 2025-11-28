🍚 샘플 타임리프 프로젝트

1인 가구 및 혼밥족을 위한 식당/메뉴 정보 제공 O2O 통합 플랫폼 > 관리자와 사용자가 공존하는 단일 애플리케이션 구조입니다.

❗ 시작 전 필독 (Prerequisites)

이 프로젝트는 Tailwind CSS를 사용하므로, 스타일이 깨지지 않으려면 아래 과정을 반드시 수행해야 합니다.

1️⃣ 필수 소프트웨어

☕ Java 21 (LTS): 필수

🟢 Node.js (LTS): Tailwind CSS 빌드용

🐘 PostgreSQL: DB 연결 (설정 파일 확인)

2️⃣ 프론트엔드(CSS) 빌드 (필수 🚨)

화면 디자인이 깨진다면 99% 확률로 CSS가 빌드되지 않은 것입니다.

패키지 설치 (최초 1회)

npm install



CSS 실시간 감시 및 빌드 (개발 중 터미널 하나 켜두기)

npm run watch:css



🛠 기술 스택 (Tech Stack)

🖥 Backend

☕ Java 21 (Record 문법 적극 활용)

🍃 Spring Boot 3.2.5

🔐 Spring Security 6 (Form Login + OAuth2 Google)

💾 JPA (Hibernate) & QueryDSL 5.0

🏗 Gradle

🎨 Frontend

🌿 Thymeleaf (+ Layout Dialect)

🌊 Tailwind CSS (Utility-first)

✨ HTMX (SPA 느낌의 인터랙션)

📦 NProgress (로딩 바)

☁️ Infra & Tools

🗄 PostgreSQL

☁️ AWS S3 (이미지 저장소)

📡 Feign Client (외부 API 통신)

🪵 P6Spy (쿼리 로그 시각화)

🏛 핵심 아키텍처 & 패턴

이 프로젝트는 단순한 MVC를 넘어 확장성을 고려한 패턴이 적용되어 있습니다.

1️⃣ 전략 패턴 (Strategy Pattern)

회원(Member)과 게시글(Post)의 타입에 따라 로직을 분리했습니다.

Factory: MemberStrategyFactory, PostStrategyFactory

Service: WriteAdminService, WriteUserService, ReadFreePostService 등

장점: if-else 도배를 방지하고, 새로운 회원 유형이나 게시판 추가 시 확장이 용이함.

2️⃣ 이벤트 기반 로깅 (Event Driven Logging)

핵심 비즈니스 로직과 로깅 관심사를 분리했습니다.

Event: MemberActivityEvent, PostActivityEvent, ExceptionEvent

Listener: @EventListener + @Async를 통해 비동기로 로그 DB 적재

효과: 로그 저장이 메인 트랜잭션 성능에 영향을 주지 않음.

3️⃣ AOP 기반 유효성 검사

BindingAdvice: 컨트롤러에서 지저분한 BindingResult 검사 코드를 제거하고 AOP로 공통 처리.

ControllerLoggingAspect: 요청/응답 파라미터 및 수행 시간 자동 로깅.

📂 디렉토리 구조 (Key Directories)

src/main/java/gyun/sample
├── domain
│   ├── account     # 로그인/인증 관련
│   ├── admin       # 관리자 전용 기능
│   ├── aws         # S3 업로드
│   ├── board       # 게시판 (전략 패턴 적용)
│   ├── log         # 활동 로그 (이벤트 리스너)
│   ├── member      # 회원 관리 (전략 패턴 적용)
│   └── social      # 구글 로그인 등
└── global
├── advice      # 전역 예외 처리
├── aop         # 로깅, 유효성 검사 AOP
├── config      # Security, Web, Async 설정
└── security    # PrincipalDetails 등 인증 객체



🚀 배포 및 운영 (Deployment)

서버 환경

🐧 OS: Amazon Linux 2023

📂 경로: /app (libs, logs, bin, backup)

스크립트

deploy_start.sh: 배포 진입점 (Git Pull -> Build -> Deploy)

app.sh: Java 프로세스 제어 (start/stop/status)

backup.sh: 매일 자정 DB/Log 백업

📝 라이선스 & 정보

Developed by: NamGyun Kim

Sample Project for Thymeleaf & Spring Boot Best Practices.