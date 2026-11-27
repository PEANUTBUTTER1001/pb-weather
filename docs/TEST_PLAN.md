# TEST_PLAN.md

## 1. 문서 정보

| 항목 | 값 |
| --- | --- |
| 문서 역할 | 요구사항 검증 절차·기대 결과·실제 증거 SSOT |
| 작성일 | 2026-11-16 |
| 상태 | 2026-11-26 자동·에뮬레이터 검증 기록 반영, 전체·부분·미실행 구분 |
| 근거 | [ENHANCEMENT_GUIDELINE.md](ENHANCEMENT_GUIDELINE.md) 11절, [REQUIREMENTS.md](REQUIREMENTS.md) 16절 추적표, [ARCHITECTURE.md](ARCHITECTURE.md) 14절 오류 처리 |

## 2. 테스트 목적과 범위

- 목적: P0·P1 요구사항 19개의 수용 기준(AC) 충족 여부를 실제 실행 증거로 판정.
- 범위: 도메인 규칙(발표 시각·병합·비교), API 파싱·오류 분류(MockWebServer), 로컬 저장·캐시 수명, Compose UI 상태, 위젯, 권한, 접근성, 보안 정적 검사, 성능 측정.
- 총 22개 TEST: 자동 15개, 수동 7개.

## 3. 제외 범위

- 실제 공공 API 가용성·정확도(외부 기관 책임) — 계약 픽스처로 대체, 실 API는 수동 스모크만.
- 기상특보 알림(비범위), iOS·웹.
- 부하 테스트(서버 없음).
- 위치정보법 법률 판단(OPEN-002, 테스트 대상 아님).

## 4. 테스트 환경

| 구분 | 환경 |
| --- | --- |
| JVM 단위 | JDK 17+, JUnit4, kotlinx-coroutines-test, Turbine, MockWebServer, Robolectric(Room·DataStore) |
| UI 계측 | 에뮬레이터 API 26·최신 API 각 1, Compose UI Test |
| 실기기 | 본인 안드로이드 기기 1대(Android 8.0+), Wi-Fi·LTE·비행기 모드 |
| 데이터 | `app/src/test/resources/fixtures/` 기상청·에어코리아 JSON/XML 오류 응답 픽스처 |
| 시간 | 주입 가능한 `Clock`(Asia/Seoul)로 경계 시각 고정 |

## 5. 테스트 지역 추적표

> 요구사항 ↔ TEST 매핑(추적표). 격자 검증 샘플 지역은 TEST-008 참조.

| TEST | 요구사항 | 유형 | 케이스 | 상태 |
| --- | --- | --- | --- | --- |
| TEST-001 | FR-001, FR-002 | 자동 | boundary | PASS |
| TEST-002 | FR-001 | 자동 | normal | PASS |
| TEST-003 | FR-002 | 자동 | normal | PASS |
| TEST-004 | FR-003 | 자동 | boundary | PASS |
| TEST-005 | FR-004, DATA-002 | 자동 | failure | PASS |
| TEST-006 | FR-005 | 자동 | boundary | PARTIAL (윤년 경계 별도 미검증) |
| TEST-007 | FR-006 | 자동 | boundary | PASS |
| TEST-008 | FR-007 | 자동 | normal | PARTIAL (ASSET-001) |
| TEST-009 | FR-007, SEC-001 | 수동 | failure | PARTIAL (위치 호출 보류) |
| TEST-010 | FR-008 | 자동 | boundary | PASS |
| TEST-011 | FR-009, NFR-PERF-002 | 수동 | normal | PARTIAL (호스트·작업 검증, S25 미실행) |
| TEST-012 | FR-010, DATA-001 | 자동 | failure | PARTIAL (캐시 자동 검증, 오프라인 화면 미검증) |
| TEST-013 | NFR-A11Y-001 | 수동 | normal | PARTIAL (TalkBack·실기기 미실행) |
| TEST-014 | BIZ-001 | 자동 | normal | PARTIAL (의존성 정적 확인·탭 이동, 전 정보 탭 수 미계측) |
| TEST-015 | BIZ-002 | 수동 | normal | NOT_RUN (출시 제외) |
| TEST-016 | FR-001, NFR-PERF-001 | 자동 | failure | PARTIAL (오류 분류·공유 검증, 모든 재시도 시나리오 미검증) |
| TEST-PERF-001 | NFR-PERF-001 | 수동 | normal | FAIL (debug 콜드 p90 4867ms > 1000ms) |
| TEST-PERF-002 | NFR-PERF-002 | 수동 | boundary | PASS (UID 최대 376225B) |
| TEST-DATA-001 | DATA-001 | 자동 | boundary | PASS |
| TEST-DATA-002 | DATA-002 | 수동 | normal | PARTIAL (화면 증거, S25 미실행) |
| TEST-SEC-001 | SEC-001 | 자동 | normal | PARTIAL (COARSE 확인, 실사용 로그·DB 미검증) |
| TEST-SEC-002 | SEC-002 | 자동 | normal | PARTIAL (소스 패턴 검사·R8, 실제 키 대조 미실행) |

## 6. 정상 시나리오

| TEST | 절차 | 기대 결과(PASS 기준) |
| --- | --- | --- |
| TEST-002 | MockWebServer에 초단기실황 정상 응답 → 홈 진입 | 기온·습도·풍속·강수형태·발표 시각 표시 |
| TEST-003 | 단기예보·초단기예보 픽스처 병합 | 현재 이후 48시간 이상 연속, 6시간 이내 초단기 값 |
| TEST-008 | 기상청 격자표 샘플 10개(서울·부산·대구·광주·대전·강릉·제주·울릉·백령·서귀포) 위경도 변환 | nx, ny 전부 격자표 값과 일치 |
| TEST-014 | `./gradlew :app:dependencies` 출력에서 광고·결제 아티팩트 검색, UI 테스트로 홈→각 정보 탭 수 확인 | `play-services-ads`·`billing` 0건, 모든 핵심 정보 탭 2회 이내 |

## 7. 오류·예외 시나리오

| TEST | 절차 | 기대 결과 |
| --- | --- | --- |
| TEST-005 | 1순위 측정소 `pm10Value="-"` 응답 | 2순위 측정소 값·측정소명·측정 시각·미확정 자료 안내, 등급 텍스트+아이콘 |
| TEST-012 | ① 캐시 있음 + 네트워크 차단 ② 캐시 없음 + 네트워크 차단 | ① 데이터 + 'N분 전 자료 · 오프라인' 배너 ② 빈 상태 + 다시 시도 |
| TEST-016 | resultCode 22, 30, XML 오류 본문, 10초 타임아웃 응답 | 크래시 없음, 사유별 문구, 22·30은 재시도 없음, 타임아웃 최대 2회 백오프 |
| TEST-009 | 권한 거부·'다시 묻지 않음'·위치 서비스 꺼짐 상태로 실행(수동) | 앱 유지, 사유 안내, 지역 검색 이동 가능 |

## 8. 경계값

| TEST | 입력 | 기대 결과 |
| --- | --- | --- |
| TEST-001 | 00:39, 00:41, 02:09, 02:11, 23:59, 자정 날짜 변경 | 실황·단기예보 모두 가용 최신 발표 시각·올바른 base_date |
| TEST-004 | tmFc 05:59/06:01/17:59/18:01, regId 매핑 없음 | 오늘~+10일 빈칸 없음, 매핑 없음은 구간 오류 상태 |
| TEST-006 | 오늘 = 3월 1일, 2월 29일(윤년) | 어제 관측과 오늘 예보의 차이 ±℃ 소수 1자리, KST 날짜·관측소명·거리 |
| TEST-007 | 최근 10일 창의 시작일 전/당일, 종료일 어제·오늘, 역전 범위 | 오늘-10일~어제 양 끝 포함 허용, 창 밖·오늘·역전은 입력 근처 오류 |
| TEST-010 | '강'(1글자), '강남', 즐겨찾기 11번째, 삭제 후 되돌리기 | 1글자 검색 안 함, 0.3초 이내 결과, 11번째 저장 차단·사유, 복구 |
| TEST-DATA-001 | 가짜 시계로 실황 29/31분, 시간별 2:59/3:01, 캐시 50MB 초과 | 만료 판정 정확, LRU 순 삭제, 즐겨찾기 보존 |
| TEST-PERF-002 | 전체 갱신 1회 전송량 | 합계 500KB 이하 |

## 9. 권한별 테스트

| 권한 상태 | 확인 | TEST |
| --- | --- | --- |
| COARSE 허용 | 현재 위치 날씨 표시 | TEST-008(변환), TEST-002 |
| 거부 / 다시 묻지 않음 | 지역 검색 대체 동작 | TEST-009 |
| 위치 서비스 꺼짐 | 사유 안내 | TEST-009 |
| 매니페스트 권한 | COARSE 외 위치 권한 없음 | TEST-SEC-001 |

## 10. 데이터·마이그레이션 테스트

- TEST-DATA-001: 캐시 만료·LRU.
- TEST-012: 오프라인 캐시 표시.
- Room 스키마 v1 → 마이그레이션 해당 없음. 스키마 변경 발생 시 `MigrationTestHelper` 테스트 추가(회귀 항목).

## 11. 접근성 테스트

- TEST-013(수동): TalkBack으로 홈·과거·지역 화면 탐색, 기온·강수확률·미세먼지 등급·그래프 요약 낭독 확인 / 글자 크기 200%에서 잘림 없음(히어로 통합 미세먼지 1안 1열 리플로 포함) / 다크·고대비 테마에서 대비 4.5:1 이상 확인 / 등급이 색만으로 구분되지 않음 / 히어로 날씨 배경 C안의 고대비·시스템 애니메이션 배율 0 정적 폴백·스크롤/탭 이탈 정지·점멸 없음 확인.
- 자동 보조: Compose UI 테스트에서 주요 노드 `contentDescription` 존재 assert(TEST-009 외 UI 테스트에 포함).

## 12. 보안 테스트

| TEST | 절차 | 기대 결과 |
| --- | --- | --- |
| TEST-SEC-001 | 병합 매니페스트(`processDebugManifest` 산출물) 권한 검사, 디버그 로그·Room DB 덤프에서 위경도 정규식 검색 | COARSE 외 위치 권한 0건, 원본 위경도 0건 |
| TEST-SEC-002 | `git grep`으로 서비스 키 문자열 검색, `app/build.gradle.kts` release `isMinifyEnabled` 확인 | 키 0건, minify true |

## 13. 회귀 테스트

- 모든 자동 TEST는 PR·TASK 종료마다 전체 실행.
- API 명세 변경(OPEN-004) 시 픽스처 갱신 후 TEST-001~007, TEST-016 재실행.
- UI 변경 시 TEST-013 수동 재확인.

## 14. 자동 검증 명령

```bash
./gradlew ktlintCheck lintDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
git grep -n -I -e "<서비스키 일부 문자열>" -- . ':!local.properties'
```

## 15. 수동 확인 항목

| TEST | 확인 내용 | 증거 |
| --- | --- | --- |
| TEST-009 | 권한 거부 흐름 | 화면 녹화 |
| TEST-011 | 위젯 표시·60분 갱신·실패 유지·탭 | 스크린샷 2장(갱신 전후), WorkManager 상태(`adb shell dumpsys jobscheduler`) |
| TEST-013 | 접근성 · 히어로 C안 폴백 · 미세먼지 1안 200% 리플로 | 체크리스트 + 스크린샷 |
| TEST-015 | 내부 테스트 업로드·설치 | Play Console 화면 캡처 |
| TEST-PERF-001 | 콜드 스타트 10회 시간 측정 | 측정 표(ms) |
| TEST-PERF-002 | 전송량 측정 | HTTP 본문 합계 + UID RX/TX 차이(JSON 수치만 기록, URL·본문 로깅 금지) |
| TEST-DATA-002 | 출처·안내 문구(히어로 통합 미세먼지 띠의 측정소·측정 시각·확정 전 안내 포함) | 스크린샷 |

## 16. 테스트 품질 게이트

- P0 요구사항 연결 TEST 100% PASS 전 릴리스(REL) 기록 금지.
- 자동 TEST 실패 0건, lint error 0건.
- 수동 TEST는 증거 파일 경로 없으면 PASS 불가.
- 실 API 수동 스모크: 실황·단기·중기·ASOS·에어코리아 각 1회 정상 응답 확인(활용신청 승인 후).

## 17. 테스트 결과

| TEST | 상태 | 실행일 | 실행자 | 증거 |
| --- | --- | --- | --- | --- |
| TEST-001 ~ TEST-016 | 5절 항목별 PASS/PARTIAL, TEST-015 NOT_RUN | 2026-11-27 | Agent(에뮬레이터) | TASK-004~013*.json, TASK-009-013-integration.json, DESIGN_REWORK/integration.json, AGSL_SHADER/verification.json |
| TEST-PERF-001, TEST-PERF-002 | 콜드 FAIL(4653ms, debug 에뮬레이터) / 갱신·전송 PASS | 2026-11-27 | Agent(에뮬레이터) | INTEGRATION/cold-start.json, refresh-metrics.json |
| TEST-DATA-001, TEST-DATA-002 | PASS / PARTIAL | 2026-11-26 | Agent(에뮬레이터) | TASK-003·006·013*.json, INTEGRATION/*.png |
| TEST-SEC-001, TEST-SEC-002 | PARTIAL (정적 검사 PASS, 실사용 로그/DB·키 직접 대조 미실행) | 2026-11-27 | Agent | TASK-009-013-integration.json, DESIGN_REWORK/integration.json |

- 결과는 실제 실행 증거 경로로 기록. 과거 생성 Run과 현재 계약이 다르면 현재 명세 기준으로 연결, MCP 기록 가능 여부는 별도 확인.

## 18. 알려진 제한

- 과거 자료는 관측소 단위 → 선택 위치와 수 km~수십 km 차이 가능(거리 표시로 고지).
- 실시간 미세먼지는 미확정 자료.
- 외부 API 장애·한도 초과는 앱에서 해결 불가, 캐시 표시로 완화.
- 성능 측정은 본인 기기 1대 기준, 기기 편차 미반영.

- TASK-004: TEST-001 발표 경계, TEST-002 실황, TEST-003 단기/초단기 실제 48시간, TEST-016 캐시 추가 전송 0·동시 실패 공유 하위 기준 PASS. 전체 성능 p90·UI·오프라인 화면은 미실행. 증거 docs/evidence/TASK-004.json.

- TEST-004 PASS(실제 제공 범위 기준): 중기육상/기온 실제 응답·11개 날짜·자정 기준 발표일 유지·종일 자료 복제 금지. docs/evidence/TASK-005.json. 새벽 +10일은 미제공 슬롯으로 명시.
- TASK-006: TEST-005 API/캐시/파싱 하위 기준 PASS, ENH-005 정규화·공백·PM별 플래그 단위 기준 PASS. 당시 추이 UI 검증 예정. 이후 통합 DetailNavigationTest에서 PM10/PM2.5 동시 표시·독립 결측 이유 검증, 확대 화면 증거 확보. 실기기 검증은 미실행. docs/evidence/TASK-006.json.
- TASK-007: TEST-006 비교 도메인·TEST-007 최근 창/검증/파싱/ASOS 실 API·TEST-016 누락 날짜 및 LRU 복구 하위 기준 PASS. 당시 화면 검증 예정. 이후 TASK-010 입력·기간 UI 검사와 확대 화면 확인. 윤년 전용 경계는 별도 미검증. docs/evidence/TASK-007.json.

- TASK-009 보완 검증: 상세 날짜→실제 시간 연결·이전/다음 경계 UI 검사 포함 전체 자동 게이트 PASS. 위치 호출 1건 SKIPPED 유지. docs/evidence/TASK-009-completion.json. TalkBack 실제 낭독·프레임 할당 프로파일 미실행.

- TASK-010: 전체 자동 검사 PASS, 기간 입력 오류·1주 7일 선택 UI 검사 PASS. 실제 검색→홈 실 API→과거 화면 확인, 375dp/200%·다크·고대비 캡처. S25 Ultra·TalkBack 실제 낭독 미실행. docs/evidence/TASK-010-completion.json 및 TASK-010/*.png.

- TASK-011: 전체 자동 게이트 PASS. 저장·저장됨 비활성·대표 지정·마지막 삭제 후 대표 null 및 일반 선택 유지 UI 검사 PASS. 검색→실 API 홈 기존 회귀 재실행 PASS. 375dp·200%·다크·고대비 캡처, S25 Ultra·TalkBack 실제 낭독 미실행. docs/evidence/TASK-011-completion.json 및 TASK-011/*.png.

- TASK-012: 첫 전체 자동 게이트 PASS(단위30, 계측15·위치1 보류), WidgetIntegrationTest의 대표 삭제/Undo·오류 시 수신 시각 보존·단일 주기 작업 PASS. 추가 호스트 검사 초기 바인딩 거부 1건 → 테스트 전용 BIND_APPWIDGET 권한 위임 방식으로 수정 후 WidgetHostTest 단독 PASS. 제품 Manifest 권한 추가 없음. 당시 최종 캡처는 기기 소실로 환경 차단. 2026-11-26 통합 검사에서 실제 RemoteViews 캡처 회수·확인(`INTEGRATION/widget-host.png`), S25 검증은 미실행. docs/evidence/TASK-012-completion.json.

- TASK-013: 고대비 팔레트 비활성·삭제 확인 UI, 캐시 삭제 도중 지연된 스냅샷/일자료 쓰기 차단 및 새 요청 허용 Room 검사 PASS. 장치 선택 --serial 옵션 경로의 UnsupportedOperationException을 내장 ANDROID_SERIAL=emulator-5554 필터로 우회, 실기기 테스트 미실행. 증거 docs/evidence/TASK-013-completion.json.
- 2026-11-26 통합 최종: `ktlintCheck lintDebug testDebugUnitTest assembleDebug connectedDebugAndroidTest` PASS. 최종 JUnit XML 기준 단위 31 PASS·계측 25 PASS·위치 1 SKIPPED. Gradle 진행 메시지의 Finished 27과 XML 집계가 달라 TEST 수는 XML testcase 기준 기록. 최초 5회 중 1~4회 실패 원인과 수정, 5차 PASS, 사용자 승인 후 설정 스크롤 1줄의 추가 검증 PASS는 `TASK-009-013-integration.json` 기록.
- 실 API 갱신 10회씩 2세트 p90 2592/2580ms, UID 송수신 최대 375240/376225B(전체 앱 UID 배경 트래픽 포함 가능). 갱신 3초·500KB 목표 PASS. HTTP 본문만의 합계는 모두 301858B이며 헤더/TLS 제외. 프로세스 콜드 스타트 10회 p90 4867ms는 1초 목표 FAIL. `INTEGRATION/refresh-metrics.json`, `refresh-metrics-final.json`, `cold-start.json` 참조, 전체 NFR-PERF-001 PASS 주장 없음.
- 히어로 자동 화면 비교: 활성 상태 프레임 변화·비활성/고대비 정적 픽셀 PASS. 앱 Canvas draw lambda의 직접 JVM new/배열 할당 명령 0개 정적 확인(`INTEGRATION/hero-bytecode.json`). 호출된 프레임워크·런타임 전체 할당량 프로파일 NOT_RUN, 시스템 애니메이션 배율 0·TalkBack 실제 낭독·물리 기기 절전은 별도 미검증.
- 2026-11-27 디자인 통합·기동 경로 최적화 후 최종 게이트: `ktlintCheck lintDebug testDebugUnitTest assembleDebug connectedDebugAndroidTest` PASS(JUnit XML 단위 31 PASS, 계측 34 중 33 PASS·위치 1 SKIPPED). 사용자 정정(애니메이션 제외)에 따라 기존 C안 프레임 루프·런타임 할당량 검증은 현 수용 대상에서 제외, 정적 배경 전환 검증은 `DESIGN_REWORK/static.json` 및 `DESIGN_REWORK/integration.json` 참조. `WeatherApp`/`WidgetSync` 지연 초기화 적용 후 debug 에뮬레이터 10회 콜드 스타트 `fullyDrawnMs` p90 4867ms→4653ms(`firstDisplayP90Ms` 3184ms)로 개선되었으나 1000ms 목표 `targetMet: false` 유지(`INTEGRATION/cold-start.json`).
- 2026-11-27 AGSL 기상 셰이더(`feat/hero-agsl-shader`): `ktlintFormat ktlintCheck lintDebug testDebugUnitTest assembleDebug connectedDebugAndroidTest` PASS(JUnit XML 단위 37 PASS, 계측 38 중 37 PASS·위치 1 SKIPPED). `HeroMotionTierTest`(티어 판정·프레임 캡·풍속/시간대 정규화·셰이더 실패 폴백·레거시 JSON 하위 호환), `HeroAnimationTest`(`HIGH`/`MEDIUM` 활성 프레임 변화·`STATIC`/고대비/설정 끄기/비가시 정지·7종×3테마·홈 가독성 캡처), `SettingsScreenTest`(`배경 움직임` 토글 및 고대비 비활성) PASS. 증거 `docs/evidence/AGSL_SHADER/`. 보급형 실기기 체감·S25 Ultra 검증은 사용자 담당.
