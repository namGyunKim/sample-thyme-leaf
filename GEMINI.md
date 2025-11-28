🤖 GEMINI 개발 규칙 (샘플 타임리프 프로젝트)

이 문서는 AI(Gemini)가 '샘플 타임리프' 프로젝트의 코드를 생성하거나 수정할 때 반드시 따라야 할 절대적인 규칙입니다.

1️⃣ 기본 원칙 (Core Principles)

🇰🇷 언어: 모든 답변, 주석, 커밋 메시지는 한국어로 작성한다.

☕ Java 버전: Java 21 (Record, Switch Expression 등 최신 문법 적극 활용).

🎨 Frontend: Thymeleaf + Tailwind CSS 조합을 유지한다. (React/Vue 사용 금지)

2️⃣ 코딩 컨벤션 (Coding Convention)

🧱 객체 생성 및 DTO

❌ No Builder Pattern: Lombok @Builder 사용을 지양한다.

✅ Constructor / Static Factory: 생성자 또는 정적 팩토리 메서드(of, create)를 사용한다.

✅ Java Records: DTO는 무조건 record로 작성한다. (불변성 보장)

예: public record MemberCreateRequest(...) {}

💾 JPA & Database

✅ Dirty Checking: 명시적인 repository.save() 호출보다 트랜잭션 안에서 엔티티 상태 변경(Dirty Checking)을 선호한다.

🔎 QueryDSL: 동적 쿼리나 복잡한 조회는 QueryDSL을 사용한다. (RepositoryCustom 구현)

🚀 Fetch Join: N+1 문제 방지를 위해 연관 관계 조회 시 fetchJoin()을 적극 사용한다.

🎯 컨트롤러 & 검증

🛡️ AOP Validation: @RestController에서는 BindingResult를 직접 코드에 쓰지 않는다. (BindingAdvice가 처리함)

🔗 InitBinder: Validator 사용 시 @InitBinder 이름을 메서드 파라미터 이름과 일치시킨다.

👤 @CurrentAccount: 로그인 사용자 정보는 PrincipalDetails를 직접 쓰기보다 커스텀 어노테이션 @CurrentAccount CurrentAccountDTO를 사용해 주입받는다.

3️⃣ 아키텍처 규칙 (Architecture Rules)

🏗 전략 패턴 (Strategy Pattern)

이 프로젝트는 회원(Member)과 게시글(Post) 처리에 전략 패턴을 사용합니다. if-else로 타입을 분기하지 마십시오.

Factory 주입: MemberStrategyFactory 또는 PostStrategyFactory를 주입받는다.

Service 획득: factory.getWriteService(type) 또는 factory.getReadService(type)으로 구현체를 가져와 실행한다.

구현체: 각 타입별 Service(WriteFreePostService 등)는 추상 클래스를 상속받아 구현한다.

📡 이벤트 기반 로깅 (Event Driven)

로그를 저장할 때는 직접 Repository를 호출하지 말고 이벤트를 발행하십시오.

❌ logRepository.save(new Log(...)) (직접 호출 금지)

✅ eventPublisher.publishEvent(new MemberActivityEvent(...)) (이벤트 발행)

4️⃣ 프론트엔드 규칙 (Frontend Rules)

🌊 Tailwind CSS: 모든 스타일링은 Tailwind 유틸리티 클래스를 사용한다. (style 태그 지양)

📐 Layout: layout/default.html을 상속받아(layout:decorate) 페이지를 구성한다.

⚡ HTMX: 간단한 AJAX 요청이나 부분 갱신은 HTMX 속성(hx-post, hx-target 등)을 활용한다.

🛑 JavaScript: var 사용 금지. const와 let만 사용한다.

5️⃣ 보안 (Security)

관리자 기능: AccountRole.ADMIN 관련 기능은 반드시 @PreAuthorize 또는 코드 레벨에서 권한을 검증해야 한다.

슈퍼 관리자: 관리자 계정 자체를 생성/수정하는 권한은 오직 SUPER_ADMIN에게만 있다. (MemberGuard 활용 가능)

6️⃣ 요약 (Cheatsheet)

구분

규칙

DTO

record 사용 필수

Entity

@Getter, @NoArgsConstructor(PROTECTED) 필수, @Builder 금지

Query

복잡하면 QueryDSL 사용

Log

ApplicationEventPublisher로 발행

Auth

@CurrentAccount로 사용자 정보 획득

UI

Thymeleaf + Tailwind CSS