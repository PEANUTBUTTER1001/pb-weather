# RELEASE_RUNBOOK.md

## 1. 문서 정보

| 항목 | 값 |
| --- | --- |
| 문서 역할 | 검증된 빌드의 출시·관찰·롤백 절차 SSOT |
| 작성일 | 2026-11-16 |
| 상태 | 계획 — 릴리스 이력 없음 |
| 근거 | [REQUIREMENTS.md](REQUIREMENTS.md) BIZ-002, [TEST_PLAN.md](TEST_PLAN.md), [SECURITY_PRIVACY.md](SECURITY_PRIVACY.md) |

## 2. 릴리스 범위

| 단계 | REL ID(예정) | 대상 | 범위 |
| --- | --- | --- | --- |
| R1 개인 사용 | REL-20XX-001 | 개발자 본인 기기(사이드로드 debug/release APK) | FR-001~FR-010 |
| R2 내부 테스트 | REL-20XX-002 | Play Console 내부 테스트 트랙(본인·지인 최대 100명) | R1 + 서명 AAB |
| R3 공개 배포 | REL-20XX-003 | Play 프로덕션(단계적 출시) | OPEN-001·OPEN-002 해소 후에만 |

- REL ID 형식: `REL-YYYYMMDD-NN`(실제 릴리스일 기준 부여).

## 3. Definition of Done

- [ ] P0 요구사항 연결 TEST 전부 PASS(실제 증거 기록).
- [ ] lint error 0, 자동 테스트 실패 0.
- [ ] TEST-SEC-001·TEST-SEC-002 PASS(키 비커밋, COARSE만).
- [ ] versionCode 증가, versionName 갱신.
- [ ] R3 한정: 개인정보처리방침 URL 게시, Play 데이터 보안 양식 제출, OPEN-001·OPEN-002 결정 기록.

## 4. 버전과 대상 커밋

- 버전 규칙: `versionName = MAJOR.MINOR.PATCH`, `versionCode` 단조 증가 정수.
- v1 최초: `1.0.0` / versionCode 1.
- 대상 커밋: 릴리스 태그 `v1.0.0` 커밋 해시를 9절 결과에 기록.

## 5. 배포 artifact

| 단계 | artifact | 경로 |
| --- | --- | --- |
| R1 | `app-release.apk`(로컬 서명) | `app/build/outputs/apk/release/` |
| R2·R3 | `app-release.aab` | `app/build/outputs/bundle/release/` |
| 공통 | R8 `mapping.txt` | `app/build/outputs/mapping/release/` — 릴리스별 보관(크래시 역난독화용) |

## 6. 배포 전 조건

- 공공데이터포털 API 5종 활용 승인 상태 유효, 키 만료일 확인.
- R2 이상: 운영계정 전환 또는 한도 증량 필요성 검토(예상 사용자 수 × 일 호출 수 ≈ 8호출/갱신).
- `local.properties`·`keystore.properties` 존재, Git 미추적 확인(`git status --ignored`).
- 실기기 수동 스모크: 현재·시간별·주간·미세먼지·비교·그래프·검색·위젯·오프라인.

## 7. 환경별 설정

| 설정 | debug | release |
| --- | --- | --- |
| 서비스 키 | local.properties | local.properties(빌드 머신) |
| HTTP 로깅 | BODY(키 마스킹) | 없음 |
| R8 | off | on(`isMinifyEnabled`, `isShrinkResources`) |
| applicationId 접미사 | `.debug` | 없음(OPEN-003 확정값) |

## 8. 빌드·배포 명령

```bash
./gradlew clean ktlintCheck lintRelease testReleaseUnitTest
./gradlew assembleRelease
./gradlew bundleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

- R2·R3: Play Console → 해당 트랙 → 새 버전 만들기 → AAB 업로드 → 출시 노트 → 검토 후 출시(수동, 개발자 본인 수행).

## 9. 마이그레이션 순서

- v1 최초 출시: 해당 없음.
- 이후 Room 스키마 변경 시: 마이그레이션 테스트 PASS → 이전 버전 설치 기기에서 업데이트 설치 스모크 → 출시.

## 10. 기능 플래그

- v1 원격 플래그 없음(서버 없음).
- 빌드 타임 플래그: `BuildConfig.ENABLE_HTTP_LOG`(debug만 true).

## 11. 승인·권한자

| 행위 | 권한자 |
| --- | --- |
| 릴리스 결정·Play 업로드 | 개발자 본인 |
| 서명 키 보관 | 개발자 본인(오프라인 백업 1부) |
| OPEN-001·OPEN-002 결정 | 개발자 본인(필요 시 법률 자문) |

## 12. 제한 공개·카나리

- R2: 내부 테스트 트랙으로 제한 공개.
- R3: Play 단계적 출시 5% → 20% → 50% → 100%, 단계별 최소 48시간 관찰.

## 13. 모니터링 지표

| 지표 | 확인 수단 | 기준 |
| --- | --- | --- |
| 비정상 종료율 | Play Console Android vitals | 1.09% 미만(Play 불량 행동 기준선 이하 유지 목표) |
| ANR율 | Android vitals | 0.47% 미만 |
| API 일일 사용량 | 공공데이터포털 마이페이지 활용 현황 | 한도의 70% 미만 |
| 사용자 리뷰 | Play Console | 조회 실패·잘못된 지역 언급 모니터링 |

- 기준 수치는 Play 정책 변경 시 재확인.

## 14. 성공·실패 판단

- 성공: 단계별 관찰 기간 동안 13절 기준 충족, 치명 오류 리포트 0건.
- 실패: 크래시율·ANR율 기준 초과, API 한도 초과 2일 연속, 데이터 오표시(잘못된 지역·날짜) 확인.

## 15. 중단·롤백 조건

- 단계적 출시 중 실패 조건 발생 → 즉시 출시 일시중지(Play Console halt).
- 서비스 키 유출 의심 → 키 재발급 후 핫픽스 빌드.

## 16. 롤백 절차

1. Play Console에서 단계적 출시 중지.
2. 이전 정상 versionCode 기반 핫픽스 빌드(versionCode +1, 이전 코드 체크아웃 또는 revert 커밋).
3. 동일 트랙 업로드·출시. (Play는 이전 versionCode 재배포 불가 → 항상 새 versionCode.)
4. R1 개인 사용: 이전 APK 재설치(`adb install -r -d`).
5. 결과를 19절에 기록.

## 17. 장애 대응·에스컬레이션

| 장애 | 1차 대응 | 에스컬레이션 |
| --- | --- | --- |
| 공공 API 전면 장애 | 앱은 캐시 표시, 공지 불필요 | 공공데이터포털 고객센터·공지 확인 |
| 한도 초과 | 캐시 정책 강화 핫픽스 | 운영계정 증량 신청, OPEN-001 재검토 |
| 크래시 급증 | 출시 중지, mapping.txt로 원인 분석 | 롤백 |

## 18. 백업·복구

- 백업 대상: 서명 keystore·비밀번호(오프라인 저장소), `mapping.txt`, 서비스 키 발급 정보.
- 사용자 데이터 백업 없음(로컬 캐시·즐겨찾기만, 서버 없음).
- 복구: keystore 분실 시 Play 앱 서명의 업로드 키 재설정 절차 진행.

## 19. 릴리스 결과

| REL | 일시 | 버전 | 커밋 | 트랙 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| — | — | — | — | — | NOT_RUN | 릴리스 이력 없음 |
