# PB Weather · 땅버날씨 (안드로이드 날씨 앱)

현재·예보·과거 날씨와 미세먼지를 광고·비용 없이 앱 하나에서 확인하는 개인용 안드로이드 앱. 기상청·에어코리아 공공데이터 API 직접 호출, 서버 없음.

## 상태

- 단계: 문서화·고도화 프로토타입 완료, 앱 코드 구현 미착수(2026-11-24).
- 범위: v1 핵심 기능(현재 날씨·시간별 48시간·주간 11일·미세먼지·어제 비교·최근 10일 그래프·현재 위치·지역 검색/즐겨찾기·홈 위젯·오프라인 캐시) + 고도화 6항목(ENH-001~006) + 히어로 날씨 배경 C안(순수 Compose `Canvas` 2D 파티클) + 미세먼지 히어로 하단 통합 1안.
- 고도화 프로토타입: [outputs/pb-weather-prototype.html](outputs/pb-weather-prototype.html) (원본 참고: [android-weather-prototype-iv3.html](android-weather-prototype-iv3.html)).
- 기술: Kotlin + Jetpack Compose(Material 3), Hilt, Retrofit, Room, DataStore, WorkManager, Glance.

## 문서 지도

전체 문서 목록·디자인 지침·보관 이력: [docs/README.md](docs/README.md).

| 문서 | 내용 |
| --- | --- |
| [AGENTS.md](AGENTS.md) | AI Agent 작업 규칙·명령·금지 영역 |
| [docs/ENHANCEMENT_GUIDELINE.md](docs/ENHANCEMENT_GUIDELINE.md) | 사용자 확정 변경(히어로 C안·미세먼지 1안·최근 10일·어제 비교·설정 4색)·고도화 6항목(ENH-001~006)·수용 기준 |
| [docs/DESIGN_TOKENS.md](docs/DESIGN_TOKENS.md) | 4종 팔레트·3종 테마·간격·곡률·타이포·C안 파티클 디자인 토큰 SSOT |
| [docs/DESIGN_IMPLEMENTATION_GUIDE.md](docs/DESIGN_IMPLEMENTATION_GUIDE.md) | 4탭 화면·설정 시그니처·바텀시트·위젯 1:1 Compose 구현 가이드 |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | 문제·목표·범위·FR/NFR/DATA/SEC·수용 기준·추적표 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 기술 스택·디렉터리 구조·데이터 흐름·API 계약·ADR |
| [docs/SECURITY_PRIVACY.md](docs/SECURITY_PRIVACY.md) | 위치정보·서비스 키 위협 모델과 통제 |
| [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) | TASK-001~014, 단계별 계획, 차단 요소 |
| [docs/TEST_PLAN.md](docs/TEST_PLAN.md) | TEST 22개, 경계값, 자동 명령, 결과 기록 |
| [docs/RELEASE_RUNBOOK.md](docs/RELEASE_RUNBOOK.md) | 개인 → 내부 테스트 → 공개 배포 절차·롤백 |

## 시작 전 준비

1. 공공데이터포털에서 API 5종 활용신청: 기상청 단기예보, 기상청 중기예보, 기상청 지상(종관, ASOS) 일자료, 에어코리아 대기오염정보, 에어코리아 측정소정보.
2. 루트 `local.properties`에 `KMA_SERVICE_KEY`, `AIRKOREA_SERVICE_KEY` 직접 입력(Git 제외).
3. [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md)의 TASK-001부터 진행.

## 공개 배포 전 결정 필요

- OPEN-001: API 직접 호출 유지 vs 캐싱 프록시(공유 트래픽 한도·키 노출).
- OPEN-002: 위치정보법상 신고 필요 여부(법률 확인).
- OPEN-003: applicationId·앱 이름.

공공데이터포털에 일반 인증키만 표시되면 해당 값을 사용. Encoding/Decoding은 같은 키의 URL 표기 차이이며 별도 발급 키가 아님. 인터셉터에서 URL 인코딩 1회 보장. 키는 사용자만 local.properties에 직접 입력하며 채팅·Git·로그 전달 금지.
