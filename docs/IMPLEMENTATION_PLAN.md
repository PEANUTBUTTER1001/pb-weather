# IMPLEMENTATION_PLAN.md

## 1. 문서 정보

| 항목 | 값 |
| --- | --- |
| 문서 역할 | TASK·순서·상태·인수인계 관리(살아 있는 문서) |
| 작성일 | 2026-11-16 |
| 현재 단계 | TASK-004 검증 완료, TASK-008 위치·최소 지역 검색 착수 |
| 근거 | [ENHANCEMENT_GUIDELINE.md](ENHANCEMENT_GUIDELINE.md), [REQUIREMENTS.md](REQUIREMENTS.md), [ARCHITECTURE.md](ARCHITECTURE.md), [TEST_PLAN.md](TEST_PLAN.md) |

## 2. 구현 목표

- v1 범위 FR-001~FR-010 전부 구현(사용자 결정: 10개 전부 v1).
- 구현 순서는 위험·의존성 기준 5단계로 분할, 각 단계 종료 시 실기기 본인 사용 가능 상태 유지.
- 완료 조건: 개발 대상 수용 기준·ENH-001~006 검증 및 S25 Ultra 실 API 확인. TASK-014·TEST-015 출시는 별도.

## 3. 작업 목록

| TASK | 제목 | 요구사항 | 의존 | 상태 |
| --- | --- | --- | --- | --- |
| TASK-001 | 프로젝트 골격·테마·빌드 설정 | BIZ-001, SEC-002, NFR-A11Y-001 | — | DONE |
| TASK-002 | 내장 자산과 좌표 변환 유틸 | FR-007, FR-008, FR-005 | TASK-001 | PARTIAL (ASSET-001) |
| TASK-003 | Room·DataStore 로컬 저장소와 캐시 수명 | DATA-001, FR-010 | TASK-001 | DONE |
| TASK-004 | 기상청 단기예보 클라이언트·현재/시간별 Repository | FR-001, FR-002, NFR-PERF-001 | TASK-002, TASK-003 | DONE |
| TASK-005 | 중기예보 클라이언트와 주간 예보 병합 | FR-003 | TASK-004 | DONE |
| TASK-006 | 에어코리아 미세먼지 클라이언트 | FR-004, DATA-002 | TASK-002, TASK-003 | DONE |
| TASK-007 | ASOS 일자료 클라이언트와 과거 날씨 Repository | FR-005, FR-006, NFR-PERF-001 | TASK-002, TASK-003 | TODO |
| TASK-008 | 위치 권한·현재 위치 제공자 | FR-007, SEC-001 | TASK-002 | TODO |
| TASK-009 | 홈 화면 UI | BIZ-001, FR-001~005, FR-010, NFR-A11Y-001 | TASK-004~008 | TODO |
| TASK-010 | 과거 날씨 화면·그래프 | FR-005, FR-006, NFR-A11Y-001 | TASK-007 | TODO |
| TASK-011 | 지역 검색·즐겨찾기 화면 | FR-008, FR-007 | TASK-003, TASK-008 | TODO |
| TASK-012 | 홈 화면 위젯·백그라운드 갱신 | FR-009, NFR-PERF-002 | TASK-003, TASK-004 | TODO |
| TASK-013 | 정보·설정 화면 | DATA-002, SEC-001, DATA-001 | TASK-003 | TODO |
| TASK-014 | 릴리스 준비(서명·AAB·내부 테스트) | BIZ-002, SEC-002 | TASK-009~013 | TODO |

### 완료 조건(Done When) 상세

- **TASK-001**: `./gradlew assembleDebug` 성공 / light·dark·high_contrast → Compose ColorScheme 매핑 / `KMA_SERVICE_KEY`·`AIRKOREA_SERVICE_KEY` BuildConfig 주입, local.properties `.gitignore` 포함 / release `isMinifyEnabled=true`.
- **TASK-002**: 격자 변환이 기상청 격자표 샘플 10개와 일치 / 최근접 ASOS·중기 regId 매핑 단위 테스트 통과 / TM 좌표 변환 구현(좌표계는 API 명세 확인).
- **TASK-003**: WeatherSnapshot·DailyObservation·RegionGrid·AsosStation 엔티티·DAO / 만료(30분·3시간·12시간·1시간)·50MB LRU 테스트 통과 / 즐겨찾기·대표 위치 DataStore.
- **TASK-004**: base_time 경계 테스트 통과 / 실황·초단기·단기 파싱·병합(6시간 이내 초단기 우선) / resultCode 03·22·30·XML 오류 → 도메인 오류 / 캐시 유효 시 호출 0회.
- **TASK-005**: tmFc 06/18시 계산 / 오늘~+10일 11개 날짜 병합 테스트 통과(실제 미제공은 null 유지, 새벽 +10일 포함).
- **TASK-006**: 근접 측정소 → 실시간 측정 2단계 / '-' 결측 시 2순위 대체 / 등급 1~4 매핑.
- **TASK-007**: 누락 날짜 구간만 조회 / 어제 비교·KST 최근 10일 창 계산 / 결측 null 보존.
- **TASK-008**: COARSE만 선언·요청 / 요청 전 목적 안내, 거부 시 지역 검색 / 원본 좌표 로깅·저장 0건.
- **TASK-009**: 현재(히어로 날씨 배경 C안 순수 Compose `Canvas` 2D 파티클·텍스트 보호 스크림·스크롤/탭/고대비/애니메이션 배율 0 정지·점멸 제외 + 히어로 하단 구분선 아래 미세먼지 1안 컴팩트 2열 띠 통합 및 독립 오류/대체)·어제 비교 요약·시간별·주간 섹션 / 섹션별 로딩·오류·오프라인·빈 상태 / 하단 탭(홈·과거·지역·설정) / contentDescription. (고도화 상세는 [ENHANCEMENT_GUIDELINE.md](ENHANCEMENT_GUIDELINE.md) TASK-ENH-001~006 참조)
- **TASK-010**: 비교 표·기간 선택·선/막대 그래프 / 입력 오류 문구 입력 연결 / 날짜 상세 시트 / 그래프 요약 낭독.
- **TASK-011**: 2글자 이상 검색 / 즐겨찾기 10개 제한·삭제·되돌리기·대표 지정 / 현재 위치 행 권한 상태.
- **TASK-012**: Glance 위젯·탭 시 앱 열기 / 네트워크 제약 60분 주기 Worker / 실패 시 마지막 값 유지.
- **TASK-013**: 출처·이용 조건·위치 처리 안내 / 캐시 삭제 확인·완료 메시지.
- **TASK-014**: keystore.properties(Git 제외) 서명 / `./gradlew bundleRelease` 성공 / 내부 테스트 트랙 업로드 기록.

## 4. 우선순위

1. **기반**(TASK-001~003): 이후 모든 작업의 전제.
2. **핵심 가치**(TASK-004, 008, 009 일부): 지금 날씨 확인 — 자주 하는 작업 1순위.
3. **차별 기능**(TASK-007, 010): 과거 날씨 — 기존 앱 불편 해소 핵심.
4. **보완 정보**(TASK-005, 006): 주간 예보·미세먼지.
5. **편의·출시**(TASK-011~014): 검색·위젯·설정·릴리스.

## 5. 선행 조건과 의존성

- 공공데이터포털 회원가입 및 활용신청 승인: 기상청 단기예보, 중기예보, 지상(종관, ASOS) 일자료, 에어코리아 대기오염정보, 측정소정보(총 5종). 승인 전 TASK-004~007 실 API 연동 불가(MockWebServer 픽스처로 선개발 가능).
- 기상청 격자 좌표표(행정구역별 nx, ny), ASOS 지점 목록, 중기예보 구역 코드표 확보 → CSV 변환(TASK-002).
- Android Studio 최신 안정판, JDK 17 이상, 실기기 1대(Android 8.0+).
- 의존 그래프: TASK-001 → {002, 003} → {004, 006, 007, 008} → {005, 009, 010, 011, 012, 013} → 014.

## 6. 담당자·담당 Agent

| 역할 | 담당 | 범위 |
| --- | --- | --- |
| 제품 결정·승인 | 개발자 본인 | 범위·OPEN 항목 결정, 릴리스 승인 |
| 구현 | 개발자 본인 또는 구현 AI Agent | TASK 단위, AGENTS.md 준수 |
| 검증 기록 | 구현 AI Agent | 실제 실행한 TEST만 `implementation_record_evidence`로 기록 |

## 7. 협업·변경 절차

1. TASK 착수 전 `implementation_task_context`로 해당 TASK 명세 확인.
2. 1 TASK = 1 목적의 기능 브랜치(의존 TASK는 선행 브랜치 기반 순차 분기) = 1 커밋 묶음(Conventional Commits, 예: `feat(forecast): TASK-004 단기예보 클라이언트`).
3. 범위 변경 필요 시 REQUIREMENTS.md 변경 이력 → 본 문서 12절 기록 → 영향 TASK·TEST 갱신.
4. 설계 변경 시 ARCHITECTURE.md ADR 추가 후 구현.

## 8. 단계별 구현 계획

| 단계 | TASK | 산출 | 종료 기준 |
| --- | --- | --- | --- |
| S1 기반 | 001, 002, 003 | 빌드 가능 빈 앱, 변환 유틸, 로컬 DB | TEST-008, TEST-DATA-001, TEST-SEC-002 PASS |
| S2 현재·예보 | 004, 008, 009(현재·시간별) | 현재 위치 현재 날씨·시간별 예보 | TEST-001~003, TEST-009, TEST-016 PASS |
| S3 과거 날씨 | 007, 010, 009(비교 요약) | 비교·기간 그래프 | TEST-006, TEST-007 PASS |
| S4 보완 정보 | 005, 006, 009(주간·미세먼지) | 주간 예보·미세먼지 | TEST-004, TEST-005 PASS |
| S5 편의·출시 | 011, 012, 013, 014 | 검색·위젯·설정·AAB | 나머지 TEST 전부 PASS, TEST-015 |

## 9. 변경 파일

- 파일 경로·라벨의 SSOT는 [ARCHITECTURE.md](ARCHITECTURE.md)의 **7. 디렉터리 구조**. 본 절은 TASK별 범위만 요약.

| TASK | 주요 경로 |
| --- | --- |
| TASK-001 | `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `AndroidManifest.xml`, `WeatherApp.kt`, `MainActivity.kt`, `ui/theme/`, `.gitignore` |
| TASK-002 | `assets/region_grid.csv`, `assets/asos_stations.csv`, `core/geo/`, `test/.../core/geo/` |
| TASK-003 | `data/local/`, `test/.../data/local/` |
| TASK-004 | `data/remote/kma/`, `data/repository/ForecastRepository.kt`, `domain/`, `core/time/` |
| TASK-005 | `data/remote/kma/MidFcstApi.kt`, `ForecastRepository.kt` |
| TASK-006 | `data/remote/airkorea/`, `data/repository/AirQualityRepository.kt` |
| TASK-007 | `data/remote/kma/AsosDailyApi.kt`, `data/repository/HistoryRepository.kt` |
| TASK-008 | `data/location/`, `ui/permission/`, `AndroidManifest.xml` |
| TASK-009 | `ui/home/`, `ui/navigation/`, `androidTest/.../ui/home/` |
| TASK-010 | `ui/history/`, `androidTest/.../ui/history/` |
| TASK-011 | `ui/locations/`, `androidTest/.../ui/locations/` |
| TASK-012 | `widget/`, `work/`, `res/xml/weather_widget_info.xml`, `AndroidManifest.xml` |
| TASK-013 | `ui/settings/` |
| TASK-014 | `app/build.gradle.kts`(서명), `docs/RELEASE_RUNBOOK.md` 결과 기록, 개인정보처리방침 초안(외부 게시용) |

## 10. 검증 증거

- 증거 SSOT: [TEST_PLAN.md](TEST_PLAN.md) 17절 테스트 결과.
- 현재 기록된 증거: 없음(구현 전). 모든 TEST `NOT_RUN`.
- PASS 판정은 실제 실행 로그·스크린샷 경로가 있을 때만 기록.

## 11. 차단 요소와 미해결 질문

| ID | 내용 | 차단 대상 | 상태 |
| --- | --- | --- | --- |
| BLK-01 | 공공데이터포털 API 5종 활용신청 승인 | TASK-004~007 실연동 | 미착수 |
| OPEN-001 | 공개 배포 시 직접 호출 vs 프록시 | 공개 배포 | UNRESOLVED |
| OPEN-002 | 위치정보법 신고 필요 여부 | 공개 배포 | UNRESOLVED |
| OPEN-003 | applicationId·앱 이름 | TASK-014 | UNRESOLVED |
| OPEN-004 | 단기예보 제공 범위 최신 명세 | TASK-004·005 | UNRESOLVED |

## 12. 변경·영향 이력

| 날짜 | CHG | 내용 | 영향 |
| --- | --- | --- | --- |
| 2026-11-16 | CHG-001 | 최초 계획 수립, 분리안 제안 → 사용자 '10개 전부 v1' 결정 | 전 TASK |
| 2026-11-24 | CHG-002 | [ENHANCEMENT_GUIDELINE.md](ENHANCEMENT_GUIDELINE.md) 확정 변경 연결: 최근 10일·어제 비교(작년 이연), 고도화 6항목(TASK-ENH-001~006), 히어로 날씨 배경 C안(순수 Compose Canvas), 미세먼지 히어로 하단 통합 1안 | TASK-007, 009, 010, 011, 012, TASK-ENH-001~006 |

## 13. 현재 상태

- 단계: TASK-001·003·004 완료, TASK-002 공식 원본 불일치만 PARTIAL.
- 다음 작업: TASK-008 위치·오프라인 검색 → 나머지 데이터·화면·위젯.
- 완료 TASK: 4 / 14 (TASK-001 기반 검증, TASK-014 출시 별도).

## 14. 인수인계

- 구현 Agent 시작 순서: [AGENTS.md](../AGENTS.md) → [ENHANCEMENT_GUIDELINE.md](ENHANCEMENT_GUIDELINE.md) → [REQUIREMENTS.md](REQUIREMENTS.md) → [ARCHITECTURE.md](ARCHITECTURE.md) → 본 문서 TASK → [TEST_PLAN.md](TEST_PLAN.md).
- 주의: 서비스 키는 사용자가 직접 `local.properties`에 입력(Agent 입력 금지).
- 각 TASK 종료 시 본 문서 3절 상태·13절 갱신.

- 2026-11-24 실행 기준: 원격 기본 `origin/develop`, 작업 `feat/task-001-foundation`. 사용자 /execute 승인, namespace/applicationId `com.pbweather.weather`. 기존 초기 파일 보존.

- TASK-001 검증: ktlintCheck·lintDebug·testDebugUnitTest·assembleDebug PASS. 증거 `docs/evidence/TASK-001.json`. 기본 에뮬레이터 Pixel_9_Pro 검증 진행, S25 Ultra 수동 검증은 사용자 담당(후속 지시).

- TASK-002: 공식 격자 3,839·활성 지원 ASOS 95·중기 코드 221개 생성. 자동 검사 PASS, TEST-008은 백령면 원본 불일치로 10/10 PASS 아님. 증거 docs/evidence/TASK-002.json, ASSET_SOURCES.md. 유틸은 후속 개발에 사용 가능.

- TASK-003 PASS: Room v1 스키마 자동 생성, 캐시·LRU·설정·연속 Undo 단위 검증 및 Pixel_9_Pro Android 15 Room 보존/삭제·DataStore 재시작 복원 계측 2개 PASS. docs/evidence/TASK-003.json.

- TASK-004 PASS: ktlintCheck·lintDebug·JVM 14개·에뮬레이터 계측 5개·assembleDebug 통과. 실황/단기/초단기 실제 응답 및 미래 48개 이상·캐시 재사용 추가 전송 0 확인. 성공·실패 동시 요청 공유와 실패 시 자료/시각 보존 확인. docs/evidence/TASK-004.json.

- TASK-005 PASS: 17 JVM·6 에뮬레이터 계측·ktlintCheck·lintDebug·assembleDebug 통과. 중기육상/기온 실응답, 오늘~+10일 11개 슬롯, 실제 발표일 기준 범위, 종일 해상도 유지 확인. 새벽 +10일 미제공은 오류/누락 위조 없이 보존. docs/evidence/TASK-005.json.
- TASK-008은 별도 feat/task-008-location의 Git stash 보존 상태. 2026-11-25 사용자 지시: 위치 계측 보류 유지. 나머지 기능 작업 계속 진행.
- TASK-006 PASS: JVM 18개·에뮬레이터 계측 6개·ktlintCheck·lintDebug·assembleDebug 통과. ver1.1 공개 측정소 목록과 ver1.5 최근 측정 실 API·재조회 전송 0 확인. PM별 결측/플래그/1h 등급, 24:00 정규화·중복·공백 분리 검증. docs/evidence/TASK-006.json. 독립 브랜치 기반 TASK-004이며 TASK-005·008은 별도 브랜치.
