# PB Weather · 땅버날씨 디자인 토큰 명세

최종 확정 프로토타입([pb-weather-prototype.html](../outputs/pb-weather-prototype.html))의 색상·간격·곡률·타이포그래피·모션·파티클 수치를 Jetpack Compose(`ui/theme/`)로 **100% 동일하게 이식**하기 위한 단일 진실 원천(SSOT) 토큰 문서.

- 문서 ID: `DOC-DESIGN-TOKENS-001` · 버전 `1.0` · 작성일 `2026-11-24`
- 기준 프로토타입: [pb-weather-prototype.html](../outputs/pb-weather-prototype.html)
- 연계 문서: [디자인·레이아웃 구현 가이드](DESIGN_IMPLEMENTATION_GUIDE.md) · [고도화 지침](ENHANCEMENT_GUIDELINE.md) · [아키텍처](ARCHITECTURE.md)
- 코드 규칙: 화면 코드(`ui/`)에 Hex·Dp·Sp 리터럴 하드코딩 금지. 반드시 `WeatherTheme.colors`, `WeatherTheme.spacing`, `WeatherTheme.shapes`, `WeatherTheme.typography` 경유(AGENTS.md 6절).

---

## 1. 설계 전 확정 결정 항목 (본 절이 현재 계약)

| 항목 | 확정 기준 |
| --- | --- |
| **말투** | 앱 전체 평서문 **합쇼체(`~합니다`)**, 요청형 **`~해 주세요`**. 의문문 제목·기능 소개형 장문 금지. |
| **메인색** | 사용자가 `설정 › 화면 › 색상`에서 선택하는 **4종 팔레트**(`인디고` 기본 · `앰버` · `틸` · `슬레이트`). |
| **바탕색** | 팔레트별 연동 바탕(`#F2F5FA` / `#F7F5F1` / `#F2F6F7` / `#F4F5F7` 라이트, `#0C1322` / `#161310` / `#0B1618` / `#111317` 다크). |
| **테마** | `라이트` · `다크` · `고대비` 3종. 라이트 일반 화면 상태 표시줄 글자 어둡게, 모달/시트 막 위 상태 표시줄 아이콘 밝게 고정. |
| **글씨 크기** | 앱 내부 3단계(`기본 1.0×` · `크게 1.18×` · `아주 크게 1.42×`) + 시스템 `200%` 확대 대응. 모든 시트·문서 창·확인 창 상속. |
| **아이콘** | 2px 스트로크 둥근 끝선 단색 벡터 기본. 의미 고정 아이콘(테마 해/달, 날씨 심볼)만 고유색, 나머지 UI 아이콘은 글자색 상속. |
| **이름 표기** | 프로젝트 `pb-weather` · 영문 `PB Weather` · 한국어 `땅버날씨`. |
| **사용자 용어** | 고객 언어 100%. 내부 ID(`FR-*`, `ENH-*`)·권한 상수·DB/라이브러리 용어는 본 화면 미노출(단, `오픈소스 라이선스` 문서 창은 법적 고지용 명칭 유지). |
| **도움말 진입점** | 홈 상단 바 `?` 아이콘 + `설정 › 앱 정보 › 도움말` (동일한 도움말 창 열기). |

---

## 2. 공통 시맨틱 색상 토큰 (팔레트 독립 고정 역할)

모든 팔레트(`indigo` · `amber` · `teal` · `slate`)에서 공통으로 유지하는 텍스트·상태·미세먼지 등급·모달 막 토큰.

| 토큰명 (`WeatherColors`) | CSS 변수 | 라이트 | 다크 | 고대비 (`HighContrast`) | 용도 |
| --- | --- | --- | --- | --- | --- |
| `surface` | `--bg-surface` | `#FFFFFF` | 팔레트별 지정 | `#0A0A0A` | 카드·하단 바·시트·위젯 표면 |
| `textPrimary` | `--text-primary` | `#0F172A` | `#F1F5F9` | `#FFFFFF` | 제목·기온·주요 수치 |
| `textSecondary` | `--text-secondary` | `#3F4F66` | `#CBD5E1` | `#F5F5F5` | 보조 라벨·본문 |
| `textTertiary` | `--text-tertiary` | `#52627A` | `#9FB0C7` | `#E0E0E0` | 발표/측정 시각·메타 |
| `textOnAccent` | `--text-on-accent` | `#FFFFFF` | 팔레트별 지정 | `#000000` | 채운 주 버튼 글자 |
| `tempWarm` | `--temp-warm` | `#B9380B` *(앰버 `#C2410C`)* | `#FB923C` | `#FF9966` | 최고기온·상승 델타 |
| `tempCool` | `--temp-cool` | `#0369A1` | `#60A5FA` | `#7AD7FF` | 최저기온·하강 델타 |
| `pm25Line` | `--pm25-color` | `#7C3AED` | `#C4B5FD` | `#F0ABFC` | 추이 차트 PM2.5 선·점 |
| `statusOk` | `--status-ok` | `#15803D` | `#4ADE80` | `#7DFF9B` | 위치 허용됨·안심 방패 |
| `gradeGoodBg` / `Fg` | `--grade-good-bg/fg` | `#DCFCE7` / `#14532D` | `#123324` / `#86EFAC` | `#000000` / `#7DFF9B` | 미세먼지 좋음 (`●`) |
| `gradeModBg` / `Fg` | `--grade-mod-bg/fg` | `#DBEAFE` / `#1E3A8A` | `#172E54` / `#93C5FD` | `#000000` / `#7AD7FF` | 미세먼지 보통 (`◐`) |
| `gradeBadBg` / `Fg` | `--grade-bad-bg/fg` | `#FFEDD5` / `#9A3412` | `#43230F` / `#FDBA74` | `#000000` / `#FFBE5C` | 미세먼지 나쁨 (`▲`) |
| `gradeVBadBg` / `Fg` | `--grade-vbad-bg/fg` | `#FEE2E2` / `#991B1B` | `#45161B` / `#FCA5A5` | `#000000` / `#FF7A7A` | 미세먼지 매우나쁨 (`◆`) |
| `statusWarnBg` / `Fg` / `Border` | `--status-warn-*` | `#FEF3C7` / `#78350F` / `#D97706` | `#3B2A0E` / `#FDE68A` / `#F59E0B` | `#000000` / `#FFE600` / `#FFE600` | 오프라인·오래됨·측정소 대체 배너 |
| `statusErrBg` / `Fg` / `Border` | `--status-err-*` | `#FEE2E2` / `#991B1B` / `#DC2626` | `#421519` / `#FECACA` / `#F87171` | `#000000` / `#FF7A7A` / `#FF7A7A` | 한도 초과·섹션 오류·입력 오류 |
| `destructive` / `Hover` | `--destructive` | `#B91C1C` / `#991B1B` | `#F87171` / `#FCA5A5` | `#FF6B6B` / `#FF9494` | 삭제 외곽선·확인 창 채움 |
| `iconSun` / `iconMoon` | `--icon-sun` / `--icon-moon` | `#D97706` / `#4F46E5` | `#FBBF24` / `#A5B4FC` | `#FFE600` / `#7AD7FF` | 설정 테마 세그먼트 고유색 |
| `scrimModal` | `--scrim-modal` | `rgba(15,23,42,0.56)` | `rgba(15,23,42,0.56)` | `rgba(0,0,0,0.76)` | 시트·문서 창 어두운 배경 막(P14) |

- **고대비 모드 전용 오버라이드** (`theme == HighContrast` 시 팔레트 선택 무시, 색상 행 비활성):
  - `ground = #000000`, `surface = #0A0A0A`, `tonal = #141414`, `tonalStrong = #1F1F1F`
  - `borderSubtle = #FFFFFF`, `borderStrong = #FFFFFF`, `cardBorderWidth = 2.dp`
  - `accentPrimary = #FFE600`, `accentSoft = #1A1A00`, `accentOnSoft = #FFE600`, `focusRing = #FFE600`
  - `rainColor = #7AD7FF`, `shadow = 0.dp`

---

## 3. 시그니처 컬러 팔레트 4종 토큰 (DataStore 저장 · 기본 `indigo`)

### 3.1 라이트 테마 (`Light`)

| 토큰 | 1. `indigo` (기본) | 2. `amber` (땅버) | 3. `teal` (청록) | 4. `slate` (차콜) |
| --- | --- | --- | --- | --- |
| `ground` | `#F2F5FA` | `#F7F5F1` | `#F2F6F7` | `#F4F5F7` |
| `surface` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| `tonal` | `#E7EEF9` | `#EFEAE1` | `#E3EFEF` | `#E8EBF0` |
| `tonalStrong` | `#DAE4F5` | `#E4DCCD` | `#D4E6E6` | `#DCE1E8` |
| `borderSubtle` | `#D6E0EE` | `#DFD7CA` | `#CFE0E2` | `#D6DCE5` |
| `borderStrong` | `#94A3B8` | `#9F9483` | `#87A3A7` | `#8A96A8` |
| `accentPrimary` | `#2B4ACB` *(7.13:1)* | `#9A5610` *(5.66:1)* | `#0F766E` *(5.47:1)* | `#273449` *(12.55:1)* |
| `accentPrimaryHover` | `#203BB0` | `#7D440B` | `#115E59` | `#1A2434` |
| `accentSoft` | `#E0E7FF` | `#FAEACC` | `#CCFBF1` | `#E2E8F0` |
| `accentOnSoft` | `#1E3A8A` *(8.41:1)* | `#6B3907` *(8.00:1)* | `#115E59` *(6.73:1)* | `#1E293B` *(11.87:1)* |
| `rainColor` | `#0284C7` | `#0369A1` | `#1D4ED8` | `#1D4ED8` |
| `focusRing` | `#2B4ACB` | `#9A5610` | `#0F766E` | `#273449` |
| `heroClearStart` | `#D8EBFF` | `#F4E6CC` | `#D6F0EE` | `#E6ECF4` |
| `heroPartlyStart` | `#DCE9FB` | `#ECE8E0` | `#DFEEEF` | `#E8ECF2` |
| `heroCloudyStart` | `#DDE5F0` | `#E6E3DD` | `#DDE6E8` | `#E2E6EC` |
| `heroRainStart` | `#CFDCED` | `#DBE1E8` | `#D0DFE5` | `#D7DFE8` |
| `heroSleetStart` | `#D3E1EF` | `#DDE3EA` | `#D4E3E8` | `#DBE3EC` |
| `heroScrimTop` | `#F2F5FA` α8% | `#F7F5F1` α8% | `#F2F6F7` α8% | `#F4F5F7` α8% |
| `heroScrimBottom` | `#F2F5FA` α74% | `#F7F5F1` α78% | `#F2F6F7` α76% | `#F4F5F7` α78% |

### 3.2 다크 테마 (`Dark`)

| 토큰 | 1. `indigo` (기본) | 2. `amber` (땅버) | 3. `teal` (청록) | 4. `slate` (차콜) |
| --- | --- | --- | --- | --- |
| `ground` | `#0C1322` | `#161310` | `#0B1618` | `#111317` |
| `surface` | `#152033` | `#211C17` | `#132326` | `#1A1D24` |
| `tonal` | `#1D2B44` | `#2D261E` | `#1B3135` | `#242832` |
| `tonalStrong` | `#253654` | `#393026` | `#233F44` | `#2E3340` |
| `borderSubtle` | `#283854` | `#3D3328` | `#243F44` | `#303644` |
| `borderStrong` | `#4B6186` | `#6E5F4E` | `#487077` | `#5A6376` |
| `accentPrimary` | `#84A9FF` *(8.09:1)* | `#F2B45C` *(10.06:1)* | `#5EEAD4` *(11.43:1)* | `#CBD5E1` *(12.02:1)* |
| `accentPrimaryHover` | `#A3BFFF` | `#F7C981` | `#99F6E4` | `#E2E8F0` |
| `textOnAccent` | `#081226` | `#1C1206` | `#04211E` | `#0F172A` |
| `accentSoft` | `#1F3263` | `#432F17` | `#134240` | `#2A313F` |
| `accentOnSoft` | `#E0E7FF` | `#FDE6C1` | `#CCFBF1` | `#F1F5F9` |
| `rainColor` | `#38BDF8` | `#60A5FA` | `#7CB3FF` | `#7CB3FF` |
| `focusRing` | `#93C5FD` | `#F2B45C` | `#5EEAD4` | `#CBD5E1` |
| `heroClearStart` | `#16305C` | `#352614` | `#12373A` | `#232936` |
| `heroPartlyStart` | `#182B4D` | `#282119` | `#132C30` | `#1F242E` |
| `heroCloudyStart` | `#1C2638` | `#23201C` | `#172528` | `#1C2028` |
| `heroRainStart` | `#142238` | `#1B222C` | `#12232C` | `#181F2A` |
| `heroSleetStart` | `#16263D` | `#1D242F` | `#142731` | `#1A222E` |
| `heroScrimTop` | `#0C1322` α12% | `#161310` α12% | `#0B1618` α12% | `#111317` α12% |
| `heroScrimBottom` | `#0C1322` α78% | `#161310` α80% | `#0B1618` α80% | `#111317` α80% |

- 괄호 안 명암비는 WCAG 2.1 AA 실측치(버튼 채움 위 `textOnAccent` / `accentSoft` 위 `accentOnSoft` 기준, 최저 5.47:1).

---

## 4. 간격 · 크기 · 곡률 · 정렬선 토큰 (`WeatherSpacing`, `WeatherShapes`)

| 토큰 | 값 (`Dp`) | 적용 위치 |
| --- | --- | --- |
| `pageGutter` | `16.dp` | 모든 화면 좌우 기본 여백·목록 구분선 정렬선 |
| `sectionGap` | `14.dp` | 홈 섹션 간격 (`16.dp` 과거/지역, `20.dp` 설정 섹션 간격) |
| `cardPaddingH` / `V` | `16.dp` / `15.dp` | 홈·과거 표면 카드 내부 여백 |
| `settingRowPaddingH` / `V` | `16.dp` / `12.dp` | 설정 카드 내부 모든 행 동일 여백 |
| `settingSectionTitleInset` | `12.dp` | 설정 묶음 제목 좌측 들여쓰기(카드 본문 `16.dp`보다 살짝 안쪽) |
| `minTouchTarget` | `44.dp` *(권장 `48.dp`)* | 모든 버튼·행·칩 최소 터치 높이 (`P9`) |
| `edgeIconOffset` | `-12.dp` | 44dp 아이콘 버튼 내 20dp 아이콘의 시각 끝을 구분선 끝에 일치 (`(44-20)/2`, `P8`) |
| `topbarMinHeight` | `58.dp` | 상단 앱바 최소 높이 |
| `bottomNavHeight` | `68.dp` | 하단 내비게이션 바 + `navigationBarsPadding()` |
| `bottomNavPill` | `52.dp × 28.dp` | 선택 탭 Material 3 알약 인디케이터 |
| `radiusSm` | `8.dp` | 입력창·요약 박스·인라인 버튼·선택 세그먼트 칸(`7.dp`) |
| `radiusMd` | `12.dp` | 배너·스낵바·세그먼트 트랙(`10.dp`) |
| `radiusLg` | `16.dp` | 섹션 카드·어제 비교 스트립 |
| `radiusSheet` | `20.dp` | 바텀시트·문서 창·확인 창 상단/전체 모서리 |
| `radiusWidget` | `22.dp` | Glance 홈 위젯 모서리 |
| `radiusPill` | `999.dp` | 주/보조 버튼·칩·등급 배지·온도 막대 |
| `dividerThin` / `dividerStrong` | `1.dp` | 항목 사이 `borderSubtle` / 평평한 목록 상하 `borderStrong` (`P6`) |
| `maxContentWidth` | `760.dp` | 폴더블 펼침(Medium) 단일 열 최대 폭(`ARCHITECTURE.md` 16절) |

---

## 5. 타이포그래피 · 글씨 크기 스케일 (`WeatherTypography`)

- 기본 폰트 패밀리: Android 기본 한글 산세리프(`Noto Sans KR` / 시스템 산세리프, 선택적 `Pretendard` 번들 여부 미정 시 시스템 기본 유지).
- 수치 스타일 필수 규칙: 모든 기온·시각·날짜·강수량·확률·농도·거리·용량에 `fontFeatureSettings = "tnum"`(`tabular-nums`) 적용.
- 한글 줄바꿈 규칙: Compose `LineBreak.Heading` / 단어 단위 줄바꿈(1글자 쪼개짐 방지).

| 역할 | 기본 크기 (`1.0×`) | 굵기 | 행간 | 자간 | 용도 |
| --- | --- | --- | --- | --- | --- |
| `heroTemp` | `65.6.sp` *(200% 시 `47.2.sp` 상한)* | Light (300) | `0.96×` | `-0.04em` | 현재 기온 (히어로 1차 초점) |
| `widgetTemp` | `36.8.sp` | Regular (400) | `1.0×` | `-0.02em` | 홈 위젯 현재 기온 |
| `airValue` | `17.2.sp` | Bold (700) | `1.2×` | `0` | 히어로 통합 미세먼지 수치 |
| `pageTitle` | `16.8.sp` | Bold (700) | `1.3×` | `0` | 상단 바 `설정` 제목 |
| `sheetTitle` | `15.6.sp` | Bold (700) | `1.3×` | `0` | 바텀시트·문서 창 제목 |
| `topbarLoc` | `15.3.sp` | Bold (700) | `1.25×` | `0` | 상단 바 지역명(1줄 말줄임) |
| `sectionTitle` | `14.3.sp` | Bold (700) | `1.35×` | `0` | 섹션 제목 |
| `settingLabel` | `13.8.sp` | SemiBold (600) | `1.35×` | `0` | 설정 행 제목 |
| `body` | `13.2.sp` | Medium/Regular | `1.5×` | `0` | 일반 본문·목록 행 |
| `caption` | `11.7.sp` | Medium/SemiBold | `1.4×` | `0` | 설정 설명·보조 라벨·세그먼트 |
| `meta` | `11.4.sp` | Medium (500) | `1.35×` | `0` | 발표/측정 시각·출처 메타·하단 탭 |
| `microTag` | `9.6.sp` | Bold (700) | `1.2×` | `0` | 시간별 `초단기` 태그 |

- **앱 내 글씨 크기 배율 (`DataStore: fontSizePref`)**:
  - `NORMAL = 1.00f` · `LARGE = 1.18f` · `EXTRA_LARGE = 1.42f`.
  - Compose 적용: 최상위 `WeatherTheme` 및 모든 `ModalBottomSheet`/`Dialog` 내부를 `CompositionLocalProvider(LocalDensity provides Density(density, fontScale = systemFontScale * appFontScale))`로 감싸 별도 창 배율 초기화 방지(`P13`).

---

## 6. 모션 · 히어로 C안 Canvas 파티클 파라미터

> **2026-11-27 사용자 최종 정정**: 애니메이션 구현 제외. 히어로는 날씨별 정적 그라데이션·심볼만 사용, 파티클·회전·맥동·프레임 루프·시스템 모션 관찰자 제거. 로딩 표시는 정적 스켈레톤, 시간 이동은 즉시 스크롤. 아래 C안 수치는 설계 이력이며 현재 구현·검증 대상 제외. 기존 원본 HTML은 보존, 검수 사본은 내장 A 경량 모드로 비교.

| 항목 | 값 | 규칙 |
| --- | --- | --- |
| `motionFast` / `motionMed` | `150ms` / `220ms` | 버튼 hover/pressed, 세그먼트 전환, 상단 바 배경색 전환 |
| **점멸 금지** | 번개 플래시 `0회` | 과도한 점멸·시각 자극 배제(`MAIN_DESIGN_GUIDELINE.md` §2) |
| **자동 정지 조건** | 4종 모두 중단 | ① 앱 백그라운드(`ON_STOP`) ② 다른 탭 이동 ③ 히어로 스크롤 이탈(`scrollOffset > 320dp`) ④ 고대비 테마 또는 시스템 `ANIMATOR_DURATION_SCALE == 0f` |
| **메모리 규칙** | 프레임 할당 `0건` | 파티클 배열·`Brush`·`Path`는 `remember(sky, theme, palette, size)`에서 1회 생성 후 좌표만 갱신 |

| 날씨 (`Sky`) | 회전 태양광 빔 | 다층 운무/구름 | 사선 빗줄기 | 눈송이/보케/습기 입자 |
| --- | --- | --- | --- | --- |
| `clear` (맑음) | 8줄 + 맥동 코로나(`96±14dp`) | 0 | 0 | 보케 28개 (미세 반짝임) |
| `partly` (구름많음) | 5줄 + 맥동 코로나 | 5겹 | 0 | 보케 18개 |
| `cloudy` (흐림) | 0 | 7겹 | 0 | 습기 입자 26개 (수평 유동) |
| `rain` (비) | 0 | 5겹 (어두운 슬레이트) | 64가닥 (고속 사선 낙하) | 0 |
| `sleet` (비/눈) | 0 | 5겹 (어두운 슬레이트) | 32가닥 | 눈송이 46개 (사인파 좌우 흔들림 낙하) |

---

## 7. Compose 토큰 파일 매핑 (`app/src/main/java/com/pbweather/weather/ui/theme/`)

| 파일 | 책임 |
| --- | --- |
| `WeatherColorTokens.kt` | `AppThemeMode(LIGHT, DARK, HIGH_CONTRAST)`, `AppColorPalette(INDIGO, AMBER, TEAL, SLATE)`, `WeatherColors` 데이터 클래스 및 9종 조합 빌더(`4×2 + HighContrast`) |
| `WeatherSpacingShapes.kt` | `WeatherSpacing`, `WeatherShapes` 및 `LocalWeatherSpacing` |
| `WeatherTypography.kt` | `AppFontSizePref(NORMAL, LARGE, EXTRA_LARGE)`, `tabularNums` 텍스트 스타일, 배율 상속 `CompositionLocalProvider` |
| `WeatherTheme.kt` | Material 3 `MaterialTheme` 래핑 + `LocalWeatherColors` 제공 + 시스템 바 대비(`enableEdgeToEdge` / `WindowInsetsControllerCompat`, `P14`) |

- 현재 코드 매핑: 위 4개 설계 역할을 단일 `WeatherTheme.kt`에 통합. 색은 `ThemeMode`·`Palette`·`WeatherColors`·`weatherColors`, 간격·곡률은 `Dims`, 글씨는 `FontSize`·`WeatherTypography`·`weatherTypography`, 테마 래퍼는 `WeatherTheme` 사용. 별도 `WeatherColorTokens.kt`·`WeatherSpacingShapes.kt`·`WeatherTypography.kt` 파일 생성 없음.

- 접근성 정정(2026-11-24): 강수 차트 rainColor는 유지. 작은 강수 텍스트는 별도 rainText 토큰(라이트 #0369A1, 다크 #7CB3FF, 고대비 #7AD7FF) 사용. 흰 표면 대비 기존 #0284C7은 4.095:1로 본문 기준 미달.

- TASK-009 구현 매핑: 각 역할별 행간·자간과 200% heroTemp 47.2sp 역할을 WeatherTypography로 분리. 앱 배율은 TextStyle의 sp 토큰에 적용하고 시스템 Density를 유지하여 Android 14+ 비선형 글자 확대를 보존. 시트/대화상자는 동일 CompositionLocal 역할·배율을 상속. 위의 수동 Density 교체 예시는 현재 구현에서 사용하지 않음.
