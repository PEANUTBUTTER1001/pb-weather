# ARCHITECTURE.md

## 1. 문서 정보

| 항목 | 값 |
| --- | --- |
| 문서 역할 | 구조·데이터·인터페이스 기술 계약 SSOT |
| 대상 | 안드로이드 날씨 앱 v1 (단일 `app` 모듈) |
| 작성일 | 2026-11-16 |
| 상태 | 구현 진행 — 기능·검증 상태는 IMPLEMENTATION_PLAN 참조 |
| 상위 문서 | [REQUIREMENTS.md](REQUIREMENTS.md), [SECURITY_PRIVACY.md](SECURITY_PRIVACY.md) |

## 2. 설계 목표

1. **캐시 우선(offline-first)**: 화면은 항상 Room 캐시를 구독, 네트워크는 캐시 갱신 수단 (FR-010, NFR-PERF-001).
2. **서버 없는 구조**: 기기에서 공공 API 직접 호출, 운영 부담 최소화(1인 운영 제약).
3. **API 호출 절감**: 발표 시각 단위 캐시 키, 과거 자료 불변 저장으로 일일 트래픽 한도 보호.
4. **위치 최소 처리**: 원본 위경도는 메모리 내 변환에만 사용 (SEC-001).
5. **섹션 독립 실패**: 한 API 실패가 다른 섹션 표시를 막지 않음.
6. **단순 구조**: 1인 개발 규모에 맞춘 단일 모듈 + 패키지 계층(ui / domain / data).

## 3. 관련 요구사항과 ASR

| ASR | 아키텍처 영향 요구 | 관련 요구사항 |
| --- | --- | --- |
| ASR-01 | 캐시 우선 데이터 흐름, 섹션별 Resource 상태 | FR-010, NFR-PERF-001, DATA-001 |
| ASR-02 | 발표 시각 계산 규칙과 NO_DATA 재시도 | FR-001, FR-002, FR-003 |
| ASR-03 | 위치 → 격자·ASOS·측정소·중기 구역 4종 변환 | FR-003, FR-004, FR-005, FR-007 |
| ASR-04 | 위치정보 비저장·비로깅 | SEC-001 |
| ASR-05 | 서비스 키 빌드 주입·비커밋 | SEC-002 |
| ASR-06 | 백그라운드 갱신 제약(WorkManager) | FR-009, NFR-PERF-002 |
| ASR-07 | 접근성 토큰·contentDescription | NFR-A11Y-001 |

## 4. 현재 구조

- 저장소 `weather/`에 초기 안드로이드 골격·빌드 파일 존재, 기능 구현·검증 진행 중. 디자인 기준 HTML은 고도화 완성형 `outputs/pb-weather-prototype.html`(원본 참고 `android-weather-prototype-iv3.html`), 기존 안드로이드 구조 제약 없음.
- 생성 당시 `.mvpmcp/` 하위 문서 패키지를 루트 `docs/`로 통합(2026-11-24). 프로젝트 안내·작업 규칙은 루트, 다른 디자인 지침 버전·생성 메타데이터는 `docs/archive/`에 보관.

## 5. 변경 후 구조

```text
[UI: Compose 화면·Glance 위젯]
        │  StateFlow<UiState>
[ViewModel]
        │  UseCase 호출
[domain: UseCase · 모델 · 규칙(발표시각, 비교, 등급)]
        │  Repository 인터페이스
[data: Repository 구현]
   ├─ local: Room(WeatherSnapshot, DailyObservation, RegionGrid, AsosStation) · DataStore(설정·즐겨찾기)
   ├─ remote: Retrofit(KMA 단기·중기·ASOS, AirKorea)
   └─ location: FusedLocationProviderClient(COARSE)
[work: WorkManager 주기 갱신 → 위젯 업데이트]
```

## 6. 기술 스택

| 분류 | 기술·버전 | 역할 | 근거 상태 | 결정·근거 |
| --- | --- | --- | --- | --- |
| 언어 | Kotlin 2.x (구현 시 최신 안정판 고정) | 앱 전체 | CONFIRMED | 사용자 지정 |
| UI | Jetpack Compose + Material 3 (Compose BOM 최신 안정판) | 화면 | CONFIRMED | 사용자 지정 |
| 빌드 | Gradle Kotlin DSL + Version Catalog, AGP 최신 안정판 | 빌드·의존성 | RECOMMENDED | 표준 안드로이드 구성 |
| SDK | minSdk 26 / targetSdk·compileSdk 최신 안정 API | 플랫폼 | RECOMMENDED | 국내 기기 대부분 커버, Glance·java.time 사용 용이 |
| DI | Hilt | 의존성 주입 | RECOMMENDED | 표준·Worker 주입 지원 |
| 네트워크 | Retrofit + OkHttp + kotlinx.serialization | 공공 API 호출 | RECOMMENDED | JSON 응답 파싱, 인터셉터로 키 주입 |
| 로컬 DB | Room | 캐시·과거 일자료·내장 자산 | RECOMMENDED | 쿼리·Flow 구독 |
| 설정 저장 | DataStore(Preferences) | 즐겨찾기·대표 위치·설정 | RECOMMENDED | SharedPreferences 대체 |
| 비동기 | Kotlin Coroutines + Flow | 데이터 흐름 | RECOMMENDED | Compose·Room 연동 |
| 백그라운드 | WorkManager | 위젯 주기 갱신 | RECOMMENDED | 배터리 제약 준수(NFR-PERF-002) |
| 위젯 | Jetpack Glance | 홈 위젯 | RECOMMENDED | Compose 스타일 위젯 |
| 위치 | Play services location (FusedLocationProviderClient) | 현재 위치 | RECOMMENDED | COARSE 단발 조회 |
| 그래프 | Vico (Compose 차트) | 기간 그래프 | RECOMMENDED | 선·막대 결합 지원, 대안: Compose Canvas 직접 구현 |
| 테스트 | JUnit4, kotlinx-coroutines-test, Turbine, MockWebServer, Robolectric, Compose UI Test | 자동 검증 | RECOMMENDED | 표준 조합 |
| 정적 검사 | Android Lint + ktlint(Gradle 플러그인) | 품질 게이트 | RECOMMENDED | 1인 개발 최소 게이트 |
| 데이터 | 기상청 단기예보·중기예보·ASOS 일자료 API | 날씨 데이터 | CONFIRMED | 사용자 지정 |
| 데이터 | 에어코리아 대기오염정보·측정소정보 API | 미세먼지 | RECOMMENDED | 기상청 API 미제공 항목 |

## 7. 디렉터리 구조

2026-11-26 디자인 재구현 승인: `res/drawable/design_icon_*.xml`, `res/drawable/design_weather_*.xml`, `res/values/design_colors.xml`은 확정 HTML의 SVG를 변환한 역할별 자산. `ui/components/PrototypeComponents.kt`는 조밀한 컨트롤·비교 배지·KV 격자·메타 행·부유 시트·확인 창, `androidTest/.../ui/design/PrototypeFixtures.kt`, `PrototypeParityTest.kt`, `DesignFlowCaptureTest.kt`는 운영 자료와 분리한 고정 UI 상태·상세/문서 흐름·시각 증거. 증거 경로 `docs/evidence/DESIGN_REWORK/`. 기존 API·DB·권한·원본 HTML 변경 없음.

2026-11-26 통합 QA: `androidTest/.../ui/home/IntegratedFlowTest.kt`, `HeroAnimationTest.kt`, `androidTest/.../ui/home/RefreshMetricsTest.kt`, `docs/evidence/TASK-009-013-integration.json`, `docs/evidence/INTEGRATION/`. 설정 로드 완료 전 지역 자동 전환 보류, 캐시 크기 로딩과 실제 0바이트 구분.

TASK-010 구현 경로: `ui/history/HistoryScreen.kt`, `androidTest/.../ui/history/HistoryScreenTest.kt`. 과거 화면은 전체 최근 10일 Room 관측을 공유하고 선택 기간만 필터링, 어제 비교 자료는 기간 선택과 독립 유지.
TASK-011 경로: `ui/locations/LocationsScreen.kt`, `androidTest/.../ui/locations/LocationsScreenTest.kt`. 검색·즐겨찾기의 보조 날씨는 Room Flow만 구독하며 목록 표시를 위한 네트워크 요청 없음.
TASK-012 추가: `data/repository/WidgetRepository.kt`, `widget/WeatherWidget.kt`, `work/WeatherRefreshWorker.kt`, `work/WidgetSync.kt`, `androidTest/.../widget/WidgetIntegrationTest.kt`, `res/drawable/widget_preview.xml`, `res/drawable/widget_symbol_*.xml`, `res/values/widget_colors.xml`, `androidTest/.../widget/WidgetHostTest.kt`. 기기 위치 조회 없이 대표 지역·기존 Room 캐시 공유.
TASK-013 경로: `ui/settings/SettingsScreen.kt`, `ui/settings/LegalDocSheets.kt`, `assets/legal/apache-2.0.txt`, `androidTest/.../ui/settings/SettingsScreenTest.kt`. 캐시 삭제 전 요청 세대와 완료 저장 세대를 비교해 삭제 이전 응답의 재저장 방지.

2026-11-24 구현 승인 추가 경로: `gradlew`, `gradlew.bat`, `gradle.properties`,
`gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`,
`.editorconfig`, `docs/IMPLEMENTATION_SPEC.md`, `docs/API_CONTRACT.md`,
`docs/ASSET_SOURCES.md`, `docs/evidence/`, `tools/`(자산 변환·보안 검사·디자인 비교용 원본 프레임 생성 `design_reference.py`),
`app/schemas/`(Room 자동 생성), `app/src/test/resources/fixtures/`,
`app/src/main/res/values/`, `app/src/main/res/drawable/`,
`app/src/main/res/xml/`, 패키지 `di/`, `ui/components/`,
`data/remote/ServiceKeyInterceptor.kt`, `data/local/AirStationEntity`(공식 공개 측정소 목록).
자산 CSV는 공식 출처로부터 생성하며 수기 관측값·지역 좌표 작성 금지.

```text
weather/                                         # [KEEP] 저장소 루트
├── README.md                                    # [KEEP] 프로젝트 안내
├── AGENTS.md                                    # [KEEP] AI Agent 작업 규칙
├── .gitignore                                   # [NEW] local.properties·keystore 제외
├── settings.gradle.kts                          # [NEW] 모듈 설정
├── build.gradle.kts                             # [NEW] 루트 빌드
├── gradle/libs.versions.toml                    # [NEW] 버전 카탈로그
├── docs/                                        # [KEEP] 통합 프로젝트 문서
│   ├── README.md                                # [NEW] 문서 목록·통합 출처
│   ├── REQUIREMENTS.md                          # [KEEP] 요구사항
│   ├── ARCHITECTURE.md                          # [KEEP] 구조·기술 계약
│   ├── SECURITY_PRIVACY.md                      # [KEEP] 보안·개인정보
│   ├── IMPLEMENTATION_PLAN.md                   # [KEEP] 구현 계획
│   ├── TEST_PLAN.md                             # [KEEP] 검증 계획
│   ├── RELEASE_RUNBOOK.md                       # [KEEP] 배포·롤백
│   ├── MAIN_DESIGN_GUIDELINE.md                  # [KEEP] 원본 폴더의 디자인 지침
│   ├── ENHANCEMENT_GUIDELINE.md                  # [NEW] 사용자 확정 변경·벤치마크 고도화 6항목·수용 기준
│   ├── DESIGN_TOKENS.md                          # [NEW] 최종 확정 디자인 토큰 SSOT (4종 팔레트·3종 테마·타이포·모션)
│   ├── DESIGN_IMPLEMENTATION_GUIDE.md            # [NEW] 프로토타입 1:1 Compose 화면·레이아웃 구현 가이드
│   └── archive/                                # [NEW] 다른 버전·생성 이력 보관
│       ├── MAIN_DESIGN_GUIDELINE_COPY.md         # [KEEP] 복사본 폴더의 디자인 지침
│       └── mvpmcp.manifest.json                 # [KEEP] 생성 당시 메타데이터
├── android-weather-prototype-iv3.html            # [KEEP] 원본 참고 디자인 프로토타입
├── outputs/                                     # [KEEP] 산출물 폴더
│   └── pb-weather-prototype.html                # [KEEP] 고도화 6항목·히어로 C안·미세먼지 1안 반영 완성형 프로토타입
├── app/build.gradle.kts                         # [NEW] 앱 빌드·BuildConfig 키 주입·R8
├── app/proguard-rules.pro                       # [NEW] 직렬화 모델 keep 규칙
├── app/src/main/AndroidManifest.xml             # [NEW] COARSE 위치·INTERNET 권한, 위젯 receiver
├── app/src/main/assets/region_grid.csv          # [NEW] 기상청 행정구역별 격자표(+중기 regId)
├── app/src/main/assets/asos_stations.csv        # [NEW] ASOS 지점 목록·좌표
├── app/src/main/java/com/pbweather/weather/       # [NEW] 패키지 루트(사용자 정정: com.pbweather.weather)
│   ├── WeatherApp.kt                            # [NEW] Hilt Application
│   ├── MainActivity.kt                          # [NEW] Compose 진입점
│   ├── core/geo/                                # [NEW] 격자 변환·하버사인 최근접 탐색(공개 측정소 로컬 매핑)
│   ├── core/time/                               # [NEW] 발표 시각 계산(base_time, tmFc)
│   ├── domain/                                  # [NEW] 모델·UseCase·비교 규칙
│   ├── data/remote/kma/                         # [NEW] 단기·중기·ASOS Retrofit API
│   ├── data/remote/airkorea/                    # [NEW] 측정소·실시간 측정 API
│   ├── data/local/                              # [NEW] Room DB·DAO·DataStore
│   ├── data/location/                           # [NEW] 위치 제공자
│   ├── data/repository/                         # [NEW] Forecast·AirQuality·History·Location Repository
│   ├── ui/theme/                                # [NEW] 앱 테마(색·간격·글꼴)
│   ├── ui/navigation/                           # [NEW] 하단 탭 내비게이션
│   ├── ui/home/                                 # [NEW] 홈 화면
│   ├── ui/history/                              # [NEW] 과거 날씨 화면
│   ├── ui/locations/                            # [NEW] 지역 검색·즐겨찾기
│   ├── ui/settings/                             # [NEW] 정보·설정
│   ├── ui/permission/                           # [NEW] 위치 권한 안내
│   ├── widget/                                  # [NEW] Glance 위젯
│   └── work/                                    # [NEW] WorkManager 갱신 작업
├── app/src/main/res/xml/weather_widget_info.xml # [NEW] 위젯 메타데이터
├── app/src/test/                                # [NEW] 단위 테스트(JVM·Robolectric)
└── app/src/androidTest/                         # [NEW] Compose UI 테스트
```

## 8. C4 모델

- **Context**: 사용자 ↔ 날씨 앱(안드로이드) → 기상청 API(공공데이터포털), 에어코리아 API, Google Play services(위치). 자체 서버 없음.
- **Container**: 단일 Android 앱 프로세스 — Compose UI, Glance 위젯, WorkManager Worker, Room DB, DataStore.
- **Component**: ViewModel → UseCase → Repository → (Remote API / Room / LocationProvider). 9절 책임표 참조.

## 9. 컴포넌트 책임

| 컴포넌트 | 책임 | 요구사항 |
| --- | --- | --- |
| `core/time/ReleaseTimeCalculator` | 초단기실황·초단기예보·단기예보 base_date/base_time, 중기 tmFc 계산 | FR-001~003 |
| `core/geo/KmaGridConverter` | 위경도 → 격자(nx, ny), Lambert 정각원추도법 | FR-007 |
| `core/geo/NearestFinder` · `data/repository/AirQualityRepository` | 원계획 TM 좌표 외부 조회 대신 공식 공개 측정소 목록 로컬 최근접 탐색(API_CONTRACT) | FR-004 |
| `core/geo/NearestFinder` | 하버사인 거리 기반 최근접 ASOS 지점 | FR-005 |
| `ForecastRepository` | 실황·시간별·주간 조회·병합·캐시 | FR-001~003 |
| `AirQualityRepository` | 근접 측정소 → 실시간 측정, 결측 대체 | FR-004 |
| `HistoryRepository` | ASOS 일자료 누락 구간만 조회·저장, 비교 계산 | FR-005, FR-006 |
| `LocationRepository` | 현재 위치·검색·즐겨찾기·대표 위치 | FR-007, FR-008 |
| `CachePolicy` | 종류별 만료·50MB LRU | DATA-001 |
| `WeatherRefreshWorker` | 대표 위치 갱신 후 위젯 update | FR-009 |
| `WeatherWidget` | Glance UI | FR-009 |

## 10. 데이터 흐름

1. ViewModel이 `observeHome(location)` 구독 → Room Flow 즉시 방출(캐시).
2. 캐시 만료 판정 → 만료 섹션만 병렬 네트워크 요청(supervisorScope + async, 요청별 오류 처리 및 취소 예외 재전파).
3. 응답 정규화 → Room upsert → Flow 재방출 → UI 갱신.
4. 실패 시 섹션 오류 상태 방출, 기존 캐시 유지(`isStale=true`).
5. 과거 조회: 요청 기간 중 Room에 없는 날짜만 연속 구간으로 묶어 ASOS 요청(numOfRows ≤ 999 페이지 처리).
6. 위젯: Worker가 대표 위치 실황·단기예보 갱신 → `GlanceAppWidget.updateAll`.

## 11. API·이벤트 계약

| 인터페이스 | 엔드포인트(Base: `https://apis.data.go.kr`) | 주요 입력 | 주요 출력 | 오류 |
| --- | --- | --- | --- | --- |
| 초단기실황 | `/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst` | base_date, base_time, nx, ny | T1H, RN1, REH, PTY, WSD, VEC | 03 NO_DATA, 22 트래픽 초과, 30 키 미등록 |
| 초단기예보 | `.../getUltraSrtFcst` | 동일 | T1H, SKY, PTY, RN1 (6시간) | 동일 |
| 단기예보 | `.../getVilageFcst` | 동일, numOfRows 대량 | TMP, SKY, PTY, POP, PCP, TMN, TMX | 동일 |
| 중기육상예보 | `/1360000/MidFcstInfoService/getMidLandFcst` | regId, tmFc | 일자별 wfAm/Pm, rnStAm/Pm | NO_DATA, 매핑 없음 |
| 중기기온 | `.../getMidTa` | regId, tmFc | taMin/taMax | 동일 |
| ASOS 일자료 | `/1360000/AsosDalyInfoService/getWthrDataList` | dataCd=ASOS, dateCd=DAY, startDt, endDt, stnIds | avgTa, maxTa, minTa, sumRn, avgWs, avgRhm | endDt 오늘 이후, 결측 |
| 근접 측정소 | `/B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList` | tmX, tmY | stationName, tm(거리) | 변환 오류 |
| 실시간 측정 | `/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty` | stationName, dataTerm=DAILY | dataTime, pm10Value, pm25Value, pm10Grade1h, pm25Grade1h, pm10Flag, pm25Flag | '-' 결측 |

- 경로·파라미터는 구현 착수 시 공공데이터포털 최신 명세로 재확인(OPEN-004). 모든 요청 `dataType/returnType=JSON`.
- 공통 오류 처리: HTTP 200이라도 `response.header.resultCode != "00"` 또는 XML 본문(키 오류) 감지 시 도메인 오류로 변환.
- 내부 계약: `Flow<Resource<T>>` — `data: T?`, `isStale`, `isOffline`, `error: DomainError?`.

## 12. 데이터 모델

| 엔티티 | 필드 | 제약 |
| --- | --- | --- |
| Location(DataStore 직렬화) | id, name, nx, ny, asosStationId, midLandRegId, midTaRegId, isFavorite, isPrimary | 즐겨찾기 ≤10, isPrimary 최대 1개/미지정 허용, 현재 위치 원본 좌표 비저장 |
| WeatherSnapshot(Room) | locationKey(nx_ny), kind(NOWCAST/HOURLY/DAILY/AIR), baseDateTime, payloadJson, fetchedAt, expiresAt | PK(locationKey, kind) |
| DailyObservation(Room) | stationId, date, avgTa?, maxTa?, minTa?, sumRn?, avgWs?, avgRhm?, lastAccessedAt | PK(stationId, date), date < 오늘, 결측 null |
| RegionGrid(Room, 내장) | code, sido, sigungu, dong, nx, ny, lat, lon, midLandRegId, midTaRegId | 최초 실행 시 assets 적재 |
| AsosStation(Room, 내장) | stationId, name, lat, lon | 최초 실행 시 assets 적재 |

- AIR 스냅샷 locationKey는 측정소명 기반(`air_<stationName>`).

## 13. 인증·권한·보안

- 사용자 인증 없음(로그인 기능 없음).
- 서비스 키: `local.properties` → `BuildConfig.KMA_SERVICE_KEY`, `BuildConfig.AIRKOREA_SERVICE_KEY` → OkHttp 인터셉터에서 쿼리 추가. 키는 URL 인코딩 이중 적용 주의(Decoding 키 사용 권장).
- 권한: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_COARSE_LOCATION`만 선언.
- 전체 요청 URL·BODY 로깅 비활성. debug에서도 API 종류·응답 상태·전송 바이트 수만 기록 가능.
- 상세: [SECURITY_PRIVACY.md](SECURITY_PRIVACY.md).

## 14. 오류 처리

| 오류 | 처리 | 사용자 표시 |
| --- | --- | --- |
| 네트워크 없음 | 캐시 유지, isOffline | 오프라인 배너 / 빈 상태 |
| resultCode 03 NO_DATA | 직전 발표 시각으로 1회 재시도 | 재시도 실패 시 섹션 오류 |
| resultCode 22 트래픽 초과 | 당일 재시도 중단 플래그 | "오늘 조회 한도 초과, 저장된 자료 표시" |
| resultCode 30 / XML 오류 본문 | 키 설정 오류로 분류, 재시도 없음 | "서비스 설정 오류" (debug에서 상세) |
| 타임아웃(10초) | 지수 백오프 최대 2회 | 섹션 오류 + 재시도 |
| 파싱 실패 | 섹션 오류, 크래시 금지 | 섹션 오류 |
| 미세먼지 '-' | 2순위 측정소 대체 | '측정 자료 없음' |

## 15. 마이그레이션

- 신규 앱, 기존 데이터 없음 → 마이그레이션 해당 없음.
- Room 스키마 버전 1 시작, `exportSchema=true`로 `app/schemas/` 보관. 이후 변경은 AutoMigration 우선.

## 16. 호환성

- Android 8.0(API 26) 이상. 위젯은 Glance 지원 범위 내.
- 다크 모드·고대비 지원.
- 화면 폭: compact 기준 설계, 폴더블 펼침(medium)에서 단일 열 최대 폭 760dp 유지.

## 17. 성능·확장성

- 홈 전체 갱신 = 실황 1 + 초단기예보 1 + 단기예보 1 + 중기 2 + 미세먼지 2 + ASOS 1(비교) ≈ 8호출, 캐시 유효 시 0호출.
- 1인 사용 기준 일 호출량: 수십~수백 건 → 개발계정 한도 대비 여유.
- **확장 한계**: 공개 배포 시 모든 사용자가 하나의 키 한도 공유 → 사용자 증가 시 한도 초과 위험(constraints `performance`). OPEN-001, ADR-001 참조.

## 18. 기술적 위험

| ID | 위험 | 영향 | 완화 |
| --- | --- | --- | --- |
| RISK-01 | 공유 키 트래픽 한도 초과 | 공개 후 전 사용자 조회 실패 | 캐시 강화, 운영계정 증량 신청, 프록시 전환(ADR-001 재검토) |
| RISK-02 | APK에서 서비스 키 추출 | 키 오남용·차단 | R8, 공개 전 프록시 검토, 키 재발급 절차 |
| RISK-03 | API 명세 변경(경로·범위) | 조회 실패 | 계약 테스트 픽스처 갱신, OPEN-004 |
| RISK-04 | 격자·regId·지점 매핑 오류 | 엉뚱한 지역 날씨 | TEST-008 샘플 검증, 자산 출처·갱신일 기록 |
| RISK-05 | 위치정보법 적용 | 공개 배포 법적 위험 | OPEN-002 해소 전 공개 금지 |

## 19. 주요 결정과 ADR

| ADR | 결정 | 상태 | 근거 |
| --- | --- | --- | --- |
| ADR-001 | v1은 기기에서 공공 API 직접 호출(서버 없음) | 채택(개인·내부 테스트 단계) / 공개 전 재검토 | 1인 운영, 비용 0, 개인 사용 트래픽 소량 |
| ADR-002 | 단일 `app` 모듈 + 패키지 계층 | 채택 | 1인 개발 규모, 빌드 단순화 |
| ADR-003 | 캐시 우선 Repository(Room Flow 단일 진실 원천) | 채택 | 오프라인·속도 요구 |
| ADR-004 | 과거 자료는 ASOS 관측소 일자료 사용 | 채택 | 격자 단위 과거 자료 미제공 |
| ADR-005 | 위치 권한 COARSE 한정 | 채택 | 격자 5km 해상도로 충분, 개인정보 최소화 |
| ADR-006 | 미세먼지는 에어코리아 API | 채택 | 기상청 단기예보 API 미제공 |

## 20. 설계 품질 게이트

- [ ] 모든 FR/NFR/DATA/SEC가 9절 컴포넌트에 매핑됨.
- [ ] 7절 디렉터리 구조와 실제 파일 경로 일치(구현 후 점검).
- [ ] UI는 Repository Flow만 구독, ViewModel에서 네트워크 직접 호출 없음.
- [ ] 원본 위경도 저장·로그 코드 경로 0건(TEST-SEC-001).
- [ ] 서비스 키 하드코딩 0건(TEST-SEC-002).
- [ ] 14절 오류 표 항목별 테스트 존재(TEST-005, TEST-012, TEST-016).

- 구현 기준 브랜치: origin/develop. 원본 위치는 기기 내 격자·지점 선택에만 사용, 대기 측정소는 공식 목록으로 로컬 최근접 탐색. TmConverter를 이유로 사용자 TM 좌표 외부 전송 금지.

- TASK-002 경로: `tools/build_assets.py`, `app/src/main/assets/mid_regions.csv`, `app/src/test/resources/fixtures/grid_samples.csv`. 공식 XLSX/DOCX/CSV 변환 결과만 자산에 기록.

- TASK-004 경로: `data/remote/{PublicDataApi,ApiClient,NetworkMonitor,ServiceKeyInterceptor}.kt`, `data/repository/RequestCoordinator.kt`, `di/{NetworkModule,DataEntryPoint}.kt`, `domain/{ForecastSource,ForecastMerger}.kt`, `docs/API_CONTRACT.md`.

- TASK-005 경로: `data/remote/kma/MidParser.kt`, `data/repository/MidForecastRepository.kt`, `domain/WeeklyForecast.kt`, `app/src/test/.../MidForecastTest.kt`, `app/src/androidTest/.../LiveMidContractTest.kt`. 중기는 regId별 공유 캐시, 화면은 오늘~+10일 11개 날짜에 실제 제공값만 병합.
- TASK-006 경로: `data/remote/airkorea/AirParser.kt`, `data/repository/{AirStationCatalog,AirQualityRepository}.kt`, `domain/AirQualitySource.kt`, `di/AirModule.kt`. 공개 측정소 좌표는 로컬 최근접 탐색 전용, AIR 원본은 측정소별 캐시.
- TASK-007 경로: `data/remote/kma/AsosParser.kt`, `data/repository/HistoryRepository.kt`, `domain/{HistorySource,HistoryRules}.kt`, `di/HistoryModule.kt`. 일자료는 observations SSOT, snapshots HISTORY는 완료 날짜 표식만 보관. 실제 행 유무를 검사하여 LRU 후 누락 날짜만 재조회.
- TASK-008 경로: `data/location/{RegionResolver,CoarseLocationProvider}.kt`, `data/repository/LocationRepository.kt`, `domain/LocationSource.kt`, `di/LocationModule.kt`, `ui/permission/LocationPermissionAction.kt`. 최소 오프라인 검색을 위치 대체 경로로 먼저 구현.

- TASK-009 경로: `ui/navigation/{WeatherViewModel,WeatherShell}.kt`, `ui/components/{WeatherComponents,WeatherIcon,WeatherDetails}.kt`, `ui/home/{HomeScreen,HeroWeather}.kt`, `domain/{ForecastSummary,ApparentTemperature}.kt`. 초기 지역 선택 경로는 TASK-008 오프라인 검색 재사용, 과거·지역·설정 상세는 후속 TASK에서 완성.

- Git 검토 경로: `.github/pull_request_template.md`(별도 `chore/add-pr-template` 브랜치), `docs/evidence/GIT_REVIEW.json`(안전 검사·제출 전 검증 결과). 기능 PR과 템플릿 PR 분리.
