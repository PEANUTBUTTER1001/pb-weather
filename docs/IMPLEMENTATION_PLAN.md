# IMPLEMENTATION_PLAN.md

## 1. 문서 정보

| 항목 | 값 |
| --- | --- |
| 문서 역할 | TASK·순서·상태·인수인계 관리(살아 있는 문서) |
| 작성일 | 2026-11-16 |
| 현재 단계 | TASK-009~013 구현·통합 자동 게이트 검증, 성능·미검증 예외 정리 |
| 근거 | [ENHANCEMENT_GUIDELINE.md](ENHANCEMENT_GUIDELINE.md), [REQUIREMENTS.md](REQUIREMENTS.md), [ARCHITECTURE.md](ARCHITECTURE.md), [TEST_PLAN.md](TEST_PLAN.md) |

## 2. 구현 목표

- v1 범위 FR-001~FR-010 전부 구현(사용자 결정: 10개 전부 v1).
- 구현 순서는 위험·의존성 기준 5단계로 분할, 각 단계 종료 시 실기기 본인 사용 가능 상태 유지.
- 완료 조건: 개발 대상 수용 기준·ENH-001~006 및 기본 에뮬레이터 검증. S25 Ultra 수동 테스트는 사용자 담당, 위치 계측은 사용자 지시로 보류 유지. TASK-014·TEST-015 출시는 별도.

## 3. 작업 목록

| TASK | 제목 | 요구사항 | 의존 | 상태 |
| --- | --- | --- | --- | --- |
| TASK-001 | 프로젝트 골격·테마·빌드 설정 | BIZ-001, SEC-002, NFR-A11Y-001 | — | DONE |
| TASK-002 | 내장 자산과 좌표 변환 유틸 | FR-007, FR-008, FR-005 | TASK-001 | PARTIAL (ASSET-001) |
| TASK-003 | Room·DataStore 로컬 저장소와 캐시 수명 | DATA-001, FR-010 | TASK-001 | DONE |
| TASK-004 | 기상청 단기예보 클라이언트·현재/시간별 Repository | FR-001, FR-002, NFR-PERF-001 | TASK-002, TASK-003 | DONE |
| TASK-005 | 중기예보 클라이언트와 주간 예보 병합 | FR-003 | TASK-004 | DONE |
| TASK-006 | 에어코리아 미세먼지 클라이언트 | FR-004, DATA-002 | TASK-002, TASK-003 | DONE |
| TASK-007 | ASOS 일자료 클라이언트와 과거 날씨 Repository | FR-005, FR-006, NFR-PERF-001 | TASK-002, TASK-003 | DONE |
| TASK-008 | 위치 권한·현재 위치 제공자 | FR-007, SEC-001 | TASK-002 | PARTIAL (사용자 위치 계측 보류) |
| TASK-009 | 홈 화면 UI | BIZ-001, FR-001~005, FR-010, NFR-A11Y-001 | TASK-004~008 | PARTIAL (화면 구현·자동 검증, 겨울 입력 규격 미확인) |
| TASK-010 | 과거 날씨 화면·그래프 | FR-005, FR-006, NFR-A11Y-001 | TASK-007 | IMPLEMENTED (에뮬레이터 검증, 실기기 사용자 담당) |
| TASK-011 | 지역 검색·즐겨찾기 화면 | FR-008, FR-007 | TASK-003, TASK-008 | IMPLEMENTED (에뮬레이터 검증, 위치 호출 보류) |
| TASK-012 | 홈 화면 위젯·백그라운드 갱신 | FR-009, NFR-PERF-002 | TASK-003, TASK-004 | IMPLEMENTED (호스트·통합 검증 PASS, 실기기 사용자 담당) |
| TASK-013 | 정보·설정 화면 | DATA-002, SEC-001, DATA-001 | TASK-003 | IMPLEMENTED (설정·삭제·복원 통합 검증, 실기기 사용자 담당) |
| TASK-014 | 릴리스 준비(서명·AAB·내부 테스트) | BIZ-002, SEC-002 | TASK-009~013 | TODO |

### 완료 조건(Done When) 상세

- **TASK-001**: `./gradlew assembleDebug` 성공 / light·dark·high_contrast → Compose ColorScheme 매핑 / `KMA_SERVICE_KEY`·`AIRKOREA_SERVICE_KEY` BuildConfig 주입, local.properties `.gitignore` 포함 / release `isMinifyEnabled=true`.
- **TASK-002**: 격자 변환이 기상청 격자표 샘플 10개와 일치 / 최근접 ASOS·중기 regId 매핑 단위 테스트 통과 / 원계획 TM 좌표 변환은 공식 공개 측정소 목록의 로컬 최근접 탐색으로 대체(API_CONTRACT). 사용자 좌표 외부 전송 금지 유지.
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
- 현재 증거: `docs/evidence/TASK-001~013*.json`, `docs/evidence/TASK-009-013-integration.json`, `docs/evidence/INTEGRATION/`. 각 TEST의 전체·부분·미실행 판정은 TEST_PLAN 5·17절 기준.
- PASS 판정은 실제 실행 로그·스크린샷 경로가 있을 때만 기록.

## 11. 차단 요소와 미해결 질문

| ID | 내용 | 차단 대상 | 상태 |
| --- | --- | --- | --- |
| BLK-01 | 공공데이터포털 API 5종 활용신청 승인 | TASK-004~007 실연동 | 실 API 5종 PASS(증거 TASK-004~007) |
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

- 단계: TASK-001·003~007 완료, DR-001~012 디자인 통합 완료(`develop` 병합 `a6474ba`), `feat/hero-agsl-shader`에서 보급형 대응 `AGSL RuntimeShader` 히어로 배경·3단계 자동 티어링(`HIGH`/`MEDIUM`/`STATIC`)·설정 `배경 움직임` 토글 구현 및 전체 게이트 PASS. TASK-002 공식 원본 불일치, TASK-008 사용자 위치 계측 보류, TASK-ENH-006 겨울 WSD 입력 규격 미확인(`API_CONTRACT.md` 근거 유지)으로 PARTIAL.
- 다음 작업: `feat/hero-agsl-shader` `/git` PR 제출·`develop` 통합. 잔여 과제는 릴리스 빌드 기반 기동 프로파일링(debug p90 4653ms로 1초 목표 미달)·S25 Ultra 및 보급형 실기기 체감·TalkBack·절전/리사이즈 사용자 수동 검증. /execute 경계는 작업 브랜치 푸시 후 PR 생성 전.
- 완료 TASK: DONE 6 / 14, IMPLEMENTED 4 / 14(TASK-010~013), PARTIAL 3 / 14(TASK-002·008·009), 출시 TODO 1 / 14(TASK-014). 구현과 전체 수용 기준 완료를 구분.

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
- TASK-008은 사용자 계측 보류 확정 후 PARTIAL 브랜치로 보존. 마지막 실패 증거 유지, 위치 호출 테스트 @Ignore. 나머지 구현·통합 계속 진행.
- TASK-006 PASS: JVM 18개·에뮬레이터 계측 6개·ktlintCheck·lintDebug·assembleDebug 통과. ver1.1 공개 측정소 목록과 ver1.5 최근 측정 실 API·재조회 전송 0 확인. PM별 결측/플래그/1h 등급, 24:00 정규화·중복·공백 분리 검증. docs/evidence/TASK-006.json. 독립 브랜치 기반 TASK-004이며 TASK-005·008은 별도 브랜치.
- TASK-007 PASS: JVM 17개·에뮬레이터 계측 7개·ktlintCheck·lintDebug·assembleDebug 통과. ASOS 실 API 게시 완료 9일·재조회 전송 0 확인. KST 10일 창·어제 비교·결측 강수 null, 미게시 후 누락 날짜만 재조회·LRU 삭제 날짜 복구를 Room 계측으로 확인. docs/evidence/TASK-007.json. 독립 기반 TASK-004.
- TASK-008 보류: 로컬 매핑·검색·거부/사전 안내 PASS. 전경/공개 서울 모의 좌표에서도 COARSE 결과 OUTSIDE_COVERAGE. 5회 검증 상한 도달, 커밋·푸시 보류. docs/evidence/TASK-008.json. 원본 좌표는 출력·저장 없음. 이후 사용자 계측 보류 확정에 따라 수용 기준 검증 예외로 관리.

- 2026-11-25 사용자 후속 결정: 위치 계측 보류 유지. 해당 수용 기준을 미검증 예외로 명시하고 위치 호출 테스트 @Ignore 처리(마지막 실패 기록 보존, 추가 진단 없음). 검색·매핑·거부 안내와 구현 코드는 화면 통합에 사용, 전체 TASK-008 DONE 판정은 보류.

- TASK-009 의존 통합: TASK-005 기반에 TASK-006·007·008 병합. TASK-008 위치 호출 계측은 사용자 지시로 재실행 없음. 알려진 실패를 PASS로 치환하지 않으며, 나머지 자동 검사·화면 검증은 계속 진행.

- TASK-009 진행 기록(2026-11-25): 실제 검색→지역 선택→홈/자료 상세→뒤로 복귀 통과. 통합 JVM 30개 PASS, 에뮬레이터 계측 실패 0·위치 호출 1개 사용자 보류. 최신 타이포그래피·메타 정렬·시스템 애니메이션 설정 감지 반영. 375dp/200%/다크/고대비 화면 확인 중 시간 칸 폭 부족 발견, 글자 배율에 따른 폭 보정 적용. 최종 화면 검증·커밋·푸시 아직 미완료. TASK-010~013 상세/위젯 구현 계속 필요. docs/evidence/TASK-009.json.

- TASK-009 화면 후속 확인: 200% 시간 칸 폭 보정 후 assembleDebug PASS 및 실제 화면 확인. 증거 docs/evidence/TASK-009/*.png. 에뮬레이터 font_scale=1.0·wm size 기본값 복원. 홈 최종 QA·커밋/푸시와 TASK-010~013는 미완료 상태 유지.

- 2026-11-25 /git 체크포인트: 홈 200% 폭 보정 포함 빌드·단위 30개·린트 PASS. 저장 지역명 중복으로 발생한 UI 테스트 선택자 실패를 고유 접근성 설명으로 수정, 해당 검사 재실행 PASS. 위치 호출 1개 보류 유지. 사용자 지시로 현재 작업 Draft PR 제출, TASK-009 IN_PROGRESS·TASK-010~013 미완료 유지. 증거 docs/evidence/GIT_REVIEW.json.

- 2026-11-25 TASK-009 보완: 상세 이전/다음 실제 시간 탐색, 48시간 내 실제 원자료가 있는 날짜의 시간별 연결, 가로 스크롤 보조 버튼, 홈 focusRestorer 추가. S25 Ultra·TalkBack 실제 낭독·프레임 할당 프로파일은 별도 미검증. 겨울 WSD 입력 규격 미확인 유지. PR 생성 전 작업 브랜치 푸시 경계 적용.
- TASK-010: 최근 10일·1주·직접 입력, 입력별 오류, 오늘 예보/어제 관측 비교, 결측 단절 복합 그래프·유효일수/극값/누적강수, 날짜 6지표 상세 구현. 전체 최근 10일 Room 관측 공유로 기간 전환 후 홈 어제 비교 보존. 실기기 검증은 사용자 담당.
- TASK-011: 검색 결과 저장/저장됨, 대표·표시 중·최대 10개, 삭제/연속 Undo, COARSE 안내와 오류 표시 연결. 목록 보조 날씨는 캐시 Flow만 구독. 복원 불가 시 성공 메시지 방지·Undo 항목 보존. 실제 위치 호출은 재검증하지 않음.
- TASK-012: Glance 위젯·60분 네트워크 제약 단일 주기 작업·대표 변경 즉시 반영·캐시 공유·대표 홈/미지정 지역 탭 진입 구현. 기본 전체 게이트 PASS. 실제 AppWidgetHost 바인딩은 테스트 전용 권한 위임으로 검증 PASS(제품 권한 추가 없음), 대표 변경·마지막 삭제 실제 RemoteViews 확인. 당시 PNG 수집은 에뮬레이터 소실로 보류. 2026-11-26 통합 검사에서 호스트 이미지 회수·확인 완료(`INTEGRATION/widget-host.png`). S25 Ultra 런처/리사이즈/절전 실측은 사용자 담당.
- TASK-013: 화면/홈 위젯/위치/정리/앱 정보 시그니처, 실측 기반 1줄·2줄 배치·4색 2x2, DataStore 설정·캐시 용량/확인 삭제·안내 문서·오픈소스 이용 조건 연결. 캐시 삭제 세대 이후에만 쓰기 허용하여 삭제 이전 요청의 지연 응답(스냅샷·일자료) 재저장 차단. 서비스 키·위치 권한·스키마 버전 변경 없음. RELEASE_RUNBOOK의 debug BODY 허용 문구를 보안 상위 계약으로 정정.
- 2026-11-26 최종 통합: 홈·과거·지역·위젯·설정의 공유 셸/상태 연결, 초기 설정 로드와 사용자 탭 선택 경쟁, Activity 재생성 탭 복원, 위젯 SINGLE_TOP 진입, PM10/PM2.5 동시 추이·독립 결측, HALF_UP 반올림·음수 0 방지, 위치 설정 바로가기 스크롤 연결 보완. 5차 자동 게이트 PASS 후 스크롤 상태 1줄 누락 발견·수정, 사용자 명시 승인으로 추가 1회 전체 게이트 PASS. JUnit XML: 단위 31 PASS, 계측 25 PASS·위치 1 SKIPPED. lint 오류 0·경고 36(전체 무경고 판정 아님).
- 통합 성능: 전체 캐시 삭제 후 실 API 갱신 10회씩 2세트 p90 2592/2580ms, UID 송수신 최대 375240/376225B로 갱신 3초·500KB 충족. 프로세스 콜드 스타트 10회 p90 4867ms로 캐시 1초 목표 미달. debug 에뮬레이터 측정이며 최적화·실기기/릴리스 성능 확인 전 완료 판정 제외. 증거 `docs/evidence/INTEGRATION/*.json`.
- 검토 경계: 기능 5개는 각각 origin/develop@8001da1 독립 기반, 통합 브랜치는 모든 기능 커밋과 공통 호출부 충돌 해결을 포함. PR 생성·Ready·Merge는 /git 후속 영역. TASK-002 원본 격자 불일치·TASK-008 보류·겨울 WSD 규격·캐시 시작 성능·실기기/TalkBack·런타임 할당 프로파일·TASK-014는 잔여 항목으로 유지.
- 2026-11-27 디자인 통합·기동 최적화 마감: `fix/design-integration`에서 2026-11-27 사용자 정정(애니메이션 제외·정적 배경 전환) 반영 및 320/375/412dp·앱 글씨 3종·시스템 200%·180dp 위젯 보정 후 전체 자동 게이트(`ktlintCheck lintDebug testDebugUnitTest assembleDebug connectedDebugAndroidTest`) 재검증 PASS(단위 31 PASS, 계측 34 중 33 PASS·위치 1 SKIPPED, 시크릿 스캔 0건, 증거 `docs/evidence/DESIGN_REWORK/integration.json`). `WeatherApp` `Lazy<WidgetSync>`·`WidgetSync` 백그라운드 IO 초기화 분리 후 전체 게이트 재통과(단위 31 PASS, 계측 33 PASS·위치 1 SKIPPED). debug 에뮬레이터 10회 콜드 스타트 재측정 `fullyDrawnMs` p90 4867ms→4653ms(`firstDisplayP90Ms` 3184ms)로 개선되었으나 1000ms 목표 미달(`targetMet: false`, 증거 `docs/evidence/INTEGRATION/cold-start.json`). ENH-006 겨울 WSD는 `API_CONTRACT.md` 2026-11-25 확인 근거(10분 간격 수집 ≠ 10분 평균풍속)에 따라 `INPUT_SPEC_UNCONFIRMED` 유지.
- 2026-11-27 AGSL 기상 셰이더 고도화(`feat/hero-agsl-shader`): Android 13+(API 33+) 네이티브 `RuntimeShader` + `Choreographer` 기반 `HeroShaderView` 구현(`HIGH` 60fps / `MEDIUM` 0.5x·30fps / `STATIC` 폴백, 실황 풍속·KST 시간대 연동, 설정 `배경 움직임` 토글 `AppPreferences.weatherMotion`). 전체 자동 게이트 PASS(단위 37 PASS, 계측 38 중 37 PASS·위치 1 SKIPPED, 7종×3테마·홈 가독성 캡처 `docs/evidence/AGSL_SHADER/`). 보급형/S25 Ultra 실기기 체감 확인은 사용자 담당.
- 2026-11-28 UI 오와열·PBTube 패밀리룩 정렬(`fix/ui-alignment-polish`): 4개 탭 정렬 기준선 보정 및 서피스 곡률(`14.dp`)·카드 엘리베이션(`1.dp`)·세그먼트 트랙(`segmentPad 2.dp`, `segmentRadius 8.dp`)·주 버튼 곡률(`10.dp`)·디자인 토큰/아키텍처 문서 동기화 반영, 전체 자동 게이트·에뮬레이터 계측 검증 수행.
- 2026-11-28 감사 후속 안정화·릴리즈 준비(`refactor/audit-stability-lint`, `chore/release-readiness`): `WeatherViewModel` 기간 전환 중복 새로고침 분리 및 회귀 단위 테스트(`WeatherViewModelTest`) 추가, `ForecastRepository` 단일 공유 타이머(`SharedMinuteTicker`), 미사용 리소스·Deprecated API·`!!` 정리, 적응형 런처 아이콘(`mipmap-anydpi-v26`)·백업 차단 규칙(`data_extraction_rules.xml`, `backup_rules.xml`)·미서명 `assembleRelease` R8 검증 완료(서명·기동 실측·AAB·Play 업로드는 `TASK-014` 보류 유지).
