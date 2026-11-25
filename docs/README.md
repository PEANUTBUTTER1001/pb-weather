# 프로젝트 문서

안드로이드 날씨 앱의 상세 문서 통합 경로. 프로젝트 안내는 [루트 README.md](../README.md), AI Agent 작업 규칙은 [루트 AGENTS.md](../AGENTS.md).

## 문서 목록

| 문서 | 내용 |
| --- | --- |
| [REQUIREMENTS.md](REQUIREMENTS.md) | 목표·범위·기능·비기능 요구사항·수용 기준·추적표 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 기술 스택·디렉터리 구조·데이터 흐름·API 계약·ADR |
| [SECURITY_PRIVACY.md](SECURITY_PRIVACY.md) | 위치정보·서비스 키 위협 모델과 통제 |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | TASK-001~014·단계별 구현 계획·차단 요소 |
| [TEST_PLAN.md](TEST_PLAN.md) | 테스트·경계값·검증 명령·결과 기록 |
| [RELEASE_RUNBOOK.md](RELEASE_RUNBOOK.md) | 개인·내부 테스트·공개 배포 절차와 롤백 |
| [MAIN_DESIGN_GUIDELINE.md](MAIN_DESIGN_GUIDELINE.md) | 원본 폴더의 UI/UX 디자인 지침 |
| [ENHANCEMENT_GUIDELINE.md](ENHANCEMENT_GUIDELINE.md) | 사용자 확정 변경·벤치마크 고도화 6항목·구현 계약·수용 기준·검증 |
| [DESIGN_TOKENS.md](DESIGN_TOKENS.md) | 최종 확정 디자인 토큰 SSOT (4종 팔레트·3종 테마·간격·곡률·타이포·C안 파티클) |
| [DESIGN_IMPLEMENTATION_GUIDE.md](DESIGN_IMPLEMENTATION_GUIDE.md) | 화면·컴포넌트·설정 시그니처·바텀시트·위젯 1:1 Compose 구현 가이드 |

- 구현 전 [ENHANCEMENT_GUIDELINE.md](ENHANCEMENT_GUIDELINE.md) 2절의 사용자 확정 변경 우선 확인. 디자인 기준은 고도화 완성형 [pb-weather-prototype.html](../outputs/pb-weather-prototype.html)(원본 참고 [android-weather-prototype-iv3.html](../android-weather-prototype-iv3.html)). 기존 요구사항과의 우선 범위는 AGENTS.md 2절 적용.

## 통합 기록

- 통합일: 2026-11-24.
- 출처: `adaptive-run-L3HljIi8CHprJeFwQ9iAnxoy/`, `adaptive-run-L3HljIi8CHprJeFwQ9iAnxoy - 복사본/`.
- 두 묶음의 README·AGENTS·상세 문서 6개·생성 메타데이터는 통합 전 SHA-256 일치 확인 후 중복 정리.
- 기존 문서의 본문·제목·번호·요구사항 보존. 루트 README에 문서 목록 링크 추가, ARCHITECTURE의 현재 위치·디렉터리 구조 갱신.
- `result/`의 HTML 프로토타입과 `outputs/` 산출물 폴더는 기존 경로 유지.

## 디자인 지침 버전·생성 이력

디자인 지침은 두 폴더에서 내용 차이 확인. 출처별 원문 보존, 아래 구분은 버전 선후관계 의미 없음.

| 파일 | 출처·내용 |
| --- | --- |
| [MAIN_DESIGN_GUIDELINE.md](MAIN_DESIGN_GUIDELINE.md) | 원본 폴더 버전. 조밀한 조판·컨트롤, 실제 값 비례 시각화, 조작에 따른 연관 데이터 연동 명시 |
| [archive/MAIN_DESIGN_GUIDELINE_COPY.md](archive/MAIN_DESIGN_GUIDELINE_COPY.md) | 복사본 폴더 버전. 명암비 `4.5:1`·`3:1`, 터치 `44×44px(pt)`·권장 `48×48dp`, 포인터 `24×24px`, 폭 `320px`·글꼴 `200%` 등 수치 기준 명시 |
| [archive/mvpmcp.manifest.json](archive/mvpmcp.manifest.json) | 두 폴더에 동일했던 생성 당시 메타데이터. 기존 관리 경로·체크섬의 이력으로 보관, 현재 통합 문서의 검증 기준에서 제외 |
