🤖 GEMINI 개발 규칙 (샘플 타임리프 프로젝트)

이 문서는 AI(Gemini)가 '샘플 타임리프' 프로젝트의 코드를 생성하거나 수정할 때 반드시 따라야 할 절대적인 규칙입니다.
본 프로젝트는 “새로운 프로젝트에서 재사용 가능한 범용 베이스 프로젝트”를 지향합니다.

---

0️⃣ 최우선 규칙 (CRITICAL)

💻 코드 제공 방식 (절대 규칙)

- ❌ 생략 금지: “기존 로직 유지”, “...”, “일부만 예시” 등으로 코드를 생략하지 않는다.
- ✅ 전체 코드 제공: 파일에 변경이 1줄이라도 있다면 **해당 파일의 전체 코드**를 다시 작성하여 바로 복사/붙여넣기 가능하도록 제공한다.
- ✅ 변경 파일이 5개 이상이면: 압축(zip)으로 전체 파일을 제공한다. (미루지 않는다)
- ✅ 변경 파일이 5개 미만이면: 웹에 전체 코드를 파일 단위로 제공한다.

📂 파일 경로 표기 규칙

- `src/main/resources/**` 하위 파일(templates/static/yml 등)은 **항상 전체 상대경로를 함께 명시**한다.
    - 예: `src/main/resources/templates/home.html`
    - 예: `src/main/resources/application.yml`
- Java 파일은 패키지 선언으로 위치 확인이 가능하므로 파일명만 명시해도 무방하다.

🧷 스크립트 보호 규칙

- ❌ `scripts/` 패키지 및 모든 `.sh` 파일은 **절대 수정하지 않는다.**
    - 배포/백업 등 운영 자동화 의존성이 있으므로, 변경 제안 자체를 금지한다.

⚙️ 설정파일 관련 의도사항

- 설정파일에 평문(하드코딩)이 존재하거나 prod 활성화가 되어 있어도 **의도된 사항**으로 간주한다.
- 보안/권장사항을 이유로 임의로 dev 전환, 키 제거, 구조 변경을 하지 않는다. (사용자가 명시적으로 요청한 경우만 예외)

---

1️⃣ 기본 원칙 (Core Principles)

🇰🇷 언어 및 소통

- 모든 답변, 주석, 커밋 메시지는 **한국어(Korean)** 로 작성한다.
- 서버 설정 관련 질문이 없는 한, 인프라(도메인/DNS 등)보다 **프로젝트 코드(Java/Spring/HTML)** 에 집중한다.

☕ 기술 스택 및 환경

- Java: **Java 21 (LTS)** 문법 적극 활용 (Record, Pattern Matching, Switch Expression 등).
- Frontend: **Thymeleaf + Tailwind CSS** 조합 유지 (React/Vue 도입 금지).
- Server Spec(유지): Amazon Linux 2023 / t3a.medium(or t3a.medium 계열) 전제
    - 단, 도메인/서버 식별자(도메인, 이메일 등)는 베이스 프로젝트 특성상 없을 수 있으며 정상이다.

📝 문서/가이드 스타일

- Markdown 테이블은 스타일 깨짐 방지를 위해 이모지(✅/⚠️/🚨 등)를 적절히 활용한다.
- 중요한 경고/주의사항은 🚨, ⚠️ 로 강조한다.

---

2️⃣ 코딩 컨벤션 (Coding Convention)

🧱 2.1. 객체 생성 및 변경 (Object Creation & Mutation)

- ❌ No Builder Pattern: Lombok의 `@Builder` 사용을 지양(사실상 금지)한다.
- ✅ Constructor / Static Factory: 생성자 또는 정적 팩토리 메서드(`of`, `from`, `create`) 사용을 우선한다.
- ✅ 의미 있는 변경 메서드: Setter 대신 의도를 드러내는 메서드를 만든다.
    - 예: `changePassword(...)`, `activate()`, `deactivate()`

예시:

```java
public class Member {
    private String password;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
```

🚨 2.2. DTO 전략 (Record + Static Factory Method) - CRITICAL
목표: “DTO 필드가 바뀌어도 Controller/Service 수정이 0이 되도록” 구조를 강제한다.

✅ 규칙

- DTO는 무조건 `record` 로 작성한다. (불변성)
- DTO 내부에는 반드시 정적 팩토리 메서드(`from`)를 만든다.
- 🚨 DTO를 생성하는 외부 코드(Controller/Service 등)에서 `new DTO(...)` 직접 호출을 금지한다.
    - 외부에서는 **오직 `DTO.from(...)`만 호출**한다.

예시:

```java
public record UserResponse(Long id, String name, String profileUrl) {
    public static UserResponse from(User user, String profileUrl) {
        return new UserResponse(user.getId(), user.getName(), profileUrl);
    }
}
```

📌 적용 범위 가이드

- Response DTO: `from(entity, extra...)` 형태를 기본으로 한다.
- Request DTO: 폼/바인딩 목적이면 `record`로 유지하되, 생성/변환이 필요할 경우 `of(...)` 제공을 고려한다.

🖼️ 2.3. (선택) “화면/응답용 값”은 서비스에서 주입

- DB에 저장되지 않는 값(예: 이미지 URL, CDN URL, 프록시 URL 등)은
    - 서비스 계층에서 계산/생성 후
    - DTO의 `from(...)` 인자로 전달한다.
- 목적: 엔티티를 화면 로직으로 오염시키지 않고, DTO 변경을 한 곳에 모은다.

💾 2.4. JPA & Database

- ✅ Dirty Checking 우선: 트랜잭션 내 엔티티 상태 변경을 활용하고, 불필요한 `repository.save()` 호출을 지양한다.
- 🔎 QueryDSL: 동적/복잡 조회는 QueryDSL 사용(RepositoryCustom 구현 등).
- 🚀 Fetch Join: 연관 관계 조회 시 N+1 방지를 위해 `fetchJoin()`을 적극 활용한다.

🚨 2.5. Enum 변경 시 주의 (DB 제약조건 연동 가능성)

- Enum 값이 DB 제약조건(CHECK/ENUM 타입 등)과 연동될 수 있다.
- Enum을 추가/수정/삭제하는 경우, DB 제약조건도 동기화가 필요할 수 있다.
- 변경을 제안한다면, 반드시 DB 반영을 위한 SQL(ALTER 등)까지 함께 제공한다.
    - (프로젝트마다 DB 정책이 다를 수 있으므로, 실제 제약조건 존재 여부는 코드/DDL로 확인한다.)

📂 2.6. 패키지 구조 가이드 (Base Project Standard)
새 도메인/기능 추가 시 아래 구조를 우선한다.

src/main/java/gyun/sample
├── global
│ ├── config
│ ├── exception
│ ├── security
│ └── utils
└── domain
└── {domain}
├── api
├── entity
├── enums
├── payload
├── repository
├── service
│ ├── write
│ └── read
└── validator

---

3️⃣ 아키텍처 규칙 (Architecture Rules)

✅ 3.1. CQRS (Write / Read 분리) - CRITICAL

- CommandService(Write)
    - 생성/수정/삭제 담당
    - `@Transactional` 필수
    - 엔티티를 직접 로드하여 비즈니스 로직 수행
    - 반환: `void` 또는 `생성된 ID` (엔티티 반환 금지)
- QueryService(Read)
    - 조회 담당
    - `@Transactional(readOnly = true)` 필수
    - 조회는 가능하면 DTO로 즉시 변환(Projection)한다.

🏗 3.2. 전략 패턴 (Strategy Pattern)

- if-else/switch로 타입을 분기하지 않는다.
- Factory 주입: `MemberStrategyFactory` 또는 `PostStrategyFactory`를 주입받는다.
- Service 획득: `factory.getWriteService(type)` 또는 `factory.getReadService(type)`으로 구현체를 가져와 실행한다.
- 구현체는 타입별 Service(예: `WriteFreePostService`)로 분리하고, 필요 시 추상 클래스를 상속하여 공통 흐름을 재사용한다.

🧩 3.3. (권장) Template Method + Resolver로 “권한/타입” 분기 제거

- 흐름은 동일하고 일부 정책만 다른 경우(권한/타입/상태 조합)는 Service 내부 if/else 누적을 금지한다.
- 공통 흐름은 추상 클래스(템플릿)로 고정하고, 차이점만 Hook 메서드로 분리한다.
- 조합 선택은 Resolver/Factory에 집중한다.

⚠️ @Transactional(AOP) 주의

- 공통 흐름 메서드를 `final`로 만들지 않는다. (프록시/AOP 적용 저해 가능)
- 대신 “공통 흐름은 override 금지”를 규칙으로 강제하고 Hook만 오버라이드한다.

🔍 3.4. (권장) Specification Pattern (QueryDSL)

- 복잡한 검색 조건/재사용 가능한 조건은 서비스에서 Where 절을 나열하지 않는다.
- Repository 또는 별도 Spec 클래스에 `BooleanExpression` 반환 정적 메서드로 정의하고 조건을 조립(Chain)한다.
- 목적: 가독성/재사용/테스트 용이성 확보.

📡 3.5. 이벤트 기반 로깅 (Event Driven)

- 로그 저장 시 Repository 직접 호출 금지, 이벤트 발행 방식 우선.
- ❌ `logRepository.save(new Log(...))`
- ✅ `eventPublisher.publishEvent(new MemberActivityEvent(...))`

---

4️⃣ 컨트롤러 & 검증 (Controller & Validation)

🛡️ 4.1. AOP Validation / BindingAdvice

- `@RestController`에서 `BindingResult`를 직접 if 처리하지 않는다.
- `BindingAdvice`(AOP)가 검증 에러를 감지해 예외를 발생시키고, 전역 예외 처리에서 표준 응답을 만든다.
- 컨트롤러 메서드 시그니처에 `BindingResult`가 필요하다면 **“존재만”** 시키고 직접 처리하지 않는다.

🚨 4.2. InitBinder & ModelAttribute 규칙 (공용 이름 금지)

- ❌ `@InitBinder("form")`, `@ModelAttribute("form")` 같은 공용 이름(form/dto 등) 사용 금지
- 이유: 서로 다른 Request DTO가 같은 이름을 공유하면
    - 특정 Validator가 의도치 않은 DTO에 적용될 수 있고
    - `Invalid target for Validator` 런타임 예외가 발생할 수 있다.

✅ 올바른 방식

- `@InitBinder` 와 `@ModelAttribute` 를 **Request DTO 단위로 1:1 매칭**
- ModelAttribute 이름은 DTO 의미가 드러나는 “고유 이름” 사용

예시:

```java

@InitBinder("restaurantCreateRequest")
public void initCreateBinder(WebDataBinder binder) {
    binder.addValidators(restaurantCreateRequestValidator);
}

@PostMapping("/create")
public String create(
        @Valid @ModelAttribute("restaurantCreateRequest") RestaurantCreateRequest request,
        BindingResult bindingResult
) {
    // bindingResult 직접 처리 금지 (AOP가 처리)
    return "redirect:/...";
}
```

🧾 4.3. (권장) JSON API 표준

- JSON API는 `/api/**` 경로 사용을 권장한다.
- 인증 필요 API는 `@PreAuthorize("isAuthenticated()")` 등을 기본으로 한다.
- `@RequestBody`의 검증 실패는 `MethodArgumentNotValidException`으로 즉시 예외가 발생할 수 있으므로,
  전역 예외 처리에서 일관된 400 응답 정책을 유지한다.

🔍 4.4. (권장) 민감정보 로깅/직렬화 마스킹

- 컨트롤러 로깅이 존재할 수 있으므로, Request DTO에 민감정보(password/token 등)가 있다면 마스킹이 필수다.
- 권장 방식: Jackson 직렬화 제외

```java
import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(
        String loginId,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password
) {
}
```

---

5️⃣ 프론트엔드 규칙 (Frontend Rules)

🌊 5.1. Tailwind CSS

- 스타일링은 Tailwind 유틸리티 클래스를 사용한다. (style 태그 지양)

📐 5.2. Layout

- `layout/default.html`을 상속(`layout:decorate`)하여 페이지를 구성한다.

⚡ 5.3. HTMX

- 간단한 AJAX/부분 갱신은 HTMX 속성(`hx-post`, `hx-target` 등)을 활용한다.

🛑 5.4. JavaScript

- `var` 사용 금지. `const`, `let`만 사용한다.

🚨 5.5. Thymeleaf 템플릿 표현식(SpEL) 제한 - CRITICAL
Spring 6 + Thymeleaf 환경에서는 평가 컨텍스트가 제한(Restricted)될 수 있어,
템플릿에서 객체 생성(new) 또는 정적 접근(T(...))이 차단될 수 있다.

❌ 금지

- `${T(패키지.클래스).CONST}` 형태의 정적 접근
- `${new ...}` 형태의 객체 생성
- `${T(java.time.LocalDateTime).now()}` 등 정적 메서드 호출

✅ 권장

- Enum 비교는 `name()` 기반으로 처리
    - 예: `th:if="${role != null and role.name() == 'USER'}"`
- 화면 조건이 복잡하면 DTO/도메인에 화면용 helper 메서드를 추가하고 템플릿은 단순 호출만 수행
    - 예: `isActive()`, `roleLabel()` 등
- 공통 상수/선택지 목록이 필요하면 Controller 또는 `@ControllerAdvice`의 `@ModelAttribute`로 내려준다.

🚨 5.6. Spring Boot 3.x + Thymeleaf 3.1+ Expression Object 주의 - CRITICAL
기본 설정에서 아래 Expression Object가 더 이상 자동 제공되지 않을 수 있다.

- `#request`, `#session`, `#servletContext`, `#response`

따라서 `${#request.contextPath}` 같은 표현식은 템플릿 파싱 단계에서 즉시 예외를 유발할 수 있다.

✅ 해결/권장

- 링크/정적 리소스/라우팅은 Thymeleaf 링크 표현식 `@{...}`으로 통일한다.
    - 컨텍스트 루트: `@{/}`
    - 정적 리소스: `@{/css/app.css}`
    - 라우팅: `@{/posts/{id}(id=${post.id})}`
    - 쿼리 파라미터: `@{/search(q=${q}, page=${page})}`

✅ JS에서 컨텍스트 루트가 필요하면:

```html

<script th:inline="javascript">
    const CONTEXT_ROOT = /*[[@{/}]]*/ "/";
    // CONTEXT_ROOT는 끝에 '/' 포함 가능 → 상대 경로 이어붙이기 방식 권장
</script>
```

---

6️⃣ 보안 (Security)

- 관리자 기능: `AccountRole.ADMIN` 관련 기능은 반드시 `@PreAuthorize` 또는 코드 레벨에서 권한을 검증한다.
- 슈퍼 관리자: 관리자 계정 자체 생성/수정 권한은 오직 `SUPER_ADMIN`만 가능하다. (MemberGuard 활용 가능)

---

7️⃣ 운영/배포 관련 규칙 (Base Project Policy)

- 서버 사양/OS 전제는 유지하되, 도메인/DNS/이메일 등 인프라 식별자는 베이스 프로젝트에서는 없을 수 있다.
- 배포/백업 자동화 스크립트(.sh)는 절대 수정하지 않는다.
- 설정파일의 prod 활성화/평문 값은 의도사항으로 간주하며 임의 변경 금지.

---

8️⃣ 구현/수정 시 품질 점검 체크리스트 (매번 수행)

✅ 화면/템플릿

- 템플릿에서 `T(...)`, `new`, `#request` 등을 사용하지 않았는가?
- 모든 링크/정적 리소스 경로가 `@{...}`로 작성되어 있는가?
- layout 포함 시 특정 라인 파싱 예외가 전체 화면 장애로 번지지 않는가?

✅ 컨트롤러/검증

- `@InitBinder`와 `@ModelAttribute` 이름이 DTO 단위로 고유하게 분리되어 있는가? (form/dto 금지)
- `BindingResult`를 컨트롤러에서 직접 if 처리하지 않았는가? (AOP 처리)

✅ DTO/응답

- DTO는 record + from()이며, 외부에서 new 호출을 하고 있지 않은가?
- 필드 변경 시 수정 포인트가 DTO.from(...)로 한정되는가?

✅ JPA/조회

- 불필요한 save 호출을 하고 있지 않은가?
- N+1 가능성이 있는 연관조회에 fetch join/적절한 조회 전략이 적용되어 있는가?

✅ 보안/로그

- password/token 등 민감정보가 응답/로그에 노출될 수 있는 구조가 아닌가?

---

9️⃣ 요약 (Cheatsheet)

| 🧩 구분         | ✅ 규칙                                                         |
|---------------|--------------------------------------------------------------|
| ✅ DTO         | record 고정 + `from()` 정적 팩토리, 외부 `new` 금지                     |
| ✅ Entity      | `@Getter`, `@NoArgsConstructor(PROTECTED)` 권장, `@Builder` 금지 |
| ✅ Write/Read  | CQRS로 물리적 분리, Write는 `@Transactional`, Read는 `readOnly=true` |
| ✅ Query       | 복잡하면 QueryDSL + (권장) Specification으로 조건 조립                   |
| ✅ Validation  | BindingAdvice(AOP) 기반, 컨트롤러에서 BindingResult 직접 처리 금지         |
| 🚨 InitBinder | 공용 이름(form/dto) 금지, DTO 단위로 1:1 매칭                           |
| 🚨 Thymeleaf  | `T(...)`, `new`, `#request` 금지 / `@{...}` 링크 표현식 사용          |
| ✅ Log         | Repository 직접 저장 대신 이벤트 발행 우선                                |
| ✅ Auth        | `@CurrentAccount`로 사용자 정보 주입                                 |
| 🚫 Scripts    | `scripts/*.sh` 절대 수정 금지                                      |
