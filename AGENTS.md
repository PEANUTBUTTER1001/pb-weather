# AGENTS.md

## 1. 적용 범위

- 대상: 안드로이드 날씨 앱 저장소 전체(`app/`, `docs/`, Gradle 설정).
- 대상 Agent: 구현·리뷰·문서 갱신을 수행하는 모든 AI Agent.
- 하위 디렉터리에 별도 AGENTS.md가 생기면 해당 경로에서 우선(14절).

## 2. 문서 우선순위

1. 사용자의 채팅 지시
2. 본 AGENTS.md
3. [docs/ENHANCEMENT_GUIDELINE.md](docs/ENHANCEMENT_GUIDELINE.md) — 사용자 확정 변경·고도화 6항목(해당 문서 2절·ENH-001~006 범위에 한정, 보안 규칙 완화 금지)
4. [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) — 무엇·왜
5. [docs/SECURITY_PRIVACY.md](docs/SECURITY_PRIVACY.md) — 위치·키 보호
6. [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — 구조·디렉터리 SSOT
7. [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) — TASK·상태
8. [docs/TEST_PLAN.md](docs/TEST_PLAN.md) — 검증·증거
9. [docs/RELEASE_RUNBOOK.md](docs/RELEASE_RUNBOOK.md) — 출시

- 문서 간 충돌 시 상위 문서 기준, 충돌 사실을 완료 보고에 기록.

## 3. 프로젝트 구조

- 단일 `app` 모듈, 패키지 루트 `com.pbweather.weather`(임시, OPEN-003).
- 계층: `ui/` → `domain/` → `data/`(`remote/`, `local/`, `location/`, `repository/`), 공통 `core/geo`, `core/time`, 백그라운드 `work/`, 위젯 `widget/`.
- 경로 SSOT: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 7절 디렉터리 구조. 목록에 없는 경로 추가 시 해당 절 먼저 갱신.

## 4. 설치·실행 방법

1. Android Studio 최신 안정판, JDK 17+.
2. 루트 `local.properties`에 사용자가 직접 입력(Agent 입력·생성 금지):
   ```properties
   KMA_SERVICE_KEY=<공공데이터포털 Decoding 키>
   AIRKOREA_SERVICE_KEY=<공공데이터포털 Decoding 키>
   ```
3. `./gradlew assembleDebug` → `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## 5. 테스트·린트·빌드 명령

```bash
./gradlew ktlintCheck lintDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
./gradlew bundleRelease
```

## 6. 코딩 규칙

- Kotlin 공식 코드 스타일 + ktlint. 공개 함수에 KDoc은 도메인 규칙(발표 시각·비교·등급)에 한정 필수.
- UI 상태: ViewModel이 `StateFlow<UiState>` 노출, Composable은 상태·이벤트만 받음(state hoisting).
- 데이터: Repository는 Room Flow를 단일 진실 원천으로 사용, ViewModel에서 Retrofit 직접 호출 금지.
- 시간: `java.time` + 주입 가능한 `Clock`(Asia/Seoul). `System.currentTimeMillis()` 직접 사용 금지(테스트 불가).
- 색·간격·글꼴: 테마 역할 토큰만 사용, Hex 하드코딩 금지.
- 문자열: `strings.xml` 사용, 버튼은 동사형.
- 접근성: 의미 있는 아이콘·그래프에 `contentDescription`, 등급은 텍스트 병기.

## 7. 재사용할 구성요소

| 구성요소 | 위치 | 용도 |
| --- | --- | --- |
| `ReleaseTimeCalculator` | `core/time/` | 모든 기상청 발표 시각 계산 |
| `KmaGridConverter`, `TmConverter`, `NearestFinder` | `core/geo/` | 위치 변환 |
| `Resource<T>` | `domain/` | 섹션 상태(data·isStale·isOffline·error) |
| `CachePolicy` | `data/local/` | 만료·LRU |
| `ServiceKeyInterceptor` | `data/remote/` | 키 주입 단일 지점 |
| 테마·토큰 | `ui/theme/` | 색·간격·글꼴 |

- 동일 기능 중복 구현 금지, 위 구성요소 확장 우선.

## 8. 수정 금지 영역

- `app/src/main/assets/*.csv` — 출처 자료 갱신 시에만 교체, 수기 편집 금지.
- `local.properties`, `keystore.properties`, `*.jks` — 읽기·출력·커밋 금지.
- `app/schemas/` — Room 자동 생성물, 수기 편집 금지.

## 9. 데이터·보안 규칙

- 위치 권한은 `ACCESS_COARSE_LOCATION`만. FINE·BACKGROUND 추가 금지(SEC-001).
- 원본 위경도 로그·저장·외부 전송 금지. 격자·지점 ID만 사용.
- 서비스 키 하드코딩·로그 출력 금지, 커밋 전 키 문자열 검색(TEST-SEC-002).
- 광고·분석·크래시 수집 SDK 추가 금지(BIZ-001, 사용자 승인 시 예외).
- HTTPS 엔드포인트만 사용.

## 10. 승인 필요 작업

- 새 외부 라이브러리·SDK 추가.
- 위치 권한 범위 변경, 새 권한 추가.
- 범위 밖 기능 추가(예: 기상특보 알림) 또는 FR 삭제.
- Room 스키마 파괴적 변경, 마이그레이션 없는 버전 변경.
- 릴리스 빌드 서명·Play 업로드·공개 배포.
- 서버·프록시 도입(OPEN-001).

## 11. 코드 리뷰 규칙

- TASK-ID 단위 변경, 커밋 메시지 Conventional Commits + TASK-ID.
- 리뷰 체크: 요구사항 AC 충족, 9절 보안 규칙, 토큰 사용, 오류 상태 처리, 테스트 추가 여부.
- 범위 밖 수정은 별도 커밋·사전 고지.

## 12. 품질 게이트

- `ktlintCheck`, `lintDebug` error 0.
- `testDebugUnitTest` 실패 0, 변경 TASK의 연결 TEST 포함.
- UI 변경 시 375dp 폭·글자 200%·다크·고대비 확인.
- 문서 ↔ 코드 경로 일치(ARCHITECTURE 7절).

## 13. 완료 보고 형식

```text
TASK: TASK-0XX <제목>
요구사항: FR-0XX, ...
변경 파일: <경로 목록>
실행한 검증: <명령> → <결과 요약>
TEST 결과: TEST-0XX PASS|FAIL|NOT_RUN (증거 경로)
미해결·위험: <없으면 '없음'>
문서 갱신: IMPLEMENTATION_PLAN 3·13절 등
```

- 실행하지 않은 테스트를 PASS로 보고 금지.

## 14. 하위 지침·오버라이드

- 현재 하위 AGENTS.md 없음.
- 추가 시 해당 디렉터리 범위에 한해 본 문서를 보완, 9절 보안 규칙은 하위 지침으로 완화 불가.

## 15. 상세 문서 링크

- 사용자 확정 변경·고도화 6항목: [docs/ENHANCEMENT_GUIDELINE.md](docs/ENHANCEMENT_GUIDELINE.md)
- 디자인 토큰 SSOT: [docs/DESIGN_TOKENS.md](docs/DESIGN_TOKENS.md)
- 화면 디자인·레이아웃 구현 가이드: [docs/DESIGN_IMPLEMENTATION_GUIDE.md](docs/DESIGN_IMPLEMENTATION_GUIDE.md)
- 요구사항: [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md)
- 아키텍처: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- 보안·개인정보: [docs/SECURITY_PRIVACY.md](docs/SECURITY_PRIVACY.md)
- 구현 계획: [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md)
- 테스트 계획: [docs/TEST_PLAN.md](docs/TEST_PLAN.md)
- 릴리스: [docs/RELEASE_RUNBOOK.md](docs/RELEASE_RUNBOOK.md)
