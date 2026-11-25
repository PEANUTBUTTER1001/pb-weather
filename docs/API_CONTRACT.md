# Public API contract

Checked against official downloaded guides on 2026-11-24. Source downloads and hashes: ASSET_SOURCES.md.
No key values or complete request URLs are recorded.

| Service | Production operation | Contract |
| --- | --- | --- |
| KMA short | getUltraSrtNcst / getUltraSrtFcst / getVilageFcst | HTTPS, dataType=JSON, general serviceKey: raw input or percent-encoded input normalized once by interceptor, all result pages |
| KMA mid | getMidLandFcst / getMidTa | Existing MidFcstInfoService guide: 06/18 releases; day 4 starts at 06, day 5 at 18; day 8~10 whole-day fields |
| ASOS | getWthrDataList | DAY/ASOS, end at yesterday; resultCode 0 or 00 accepted; missing dates not cached as complete |
| AirKorea | getMsrstnAcctoRltmMesureDnsty | DAILY, numOfRows=24, pageNo=1, ver=1.5; 1h grade fields only; independent flags preserved |
| AirKorea stations | getMsrstnList | ver=1.1: dmX longitude, dmY latitude; all pages stored as public local reference metadata |

## Time and merge rules

- Short guide 261019 describes nowcast in 10-minute units with availability after 10 minutes. Request latest 10-minute slot. One NO_DATA fallback uses the legacy hourly 40-minute envelope; actual returned base time is preserved. Live smoke evidence determines available behavior.
- Ultra forecast HH:30 after HH:45; short forecast 02/05/08/11/14/17/20/23 after 10 minutes.
- Full next 48 actual forecast hours are displayed. Extended short days may be three-hour values and qualitative PCP codes; never fabricate hourly points or convert codes to numeric rain amounts.
- Short morning/afternoon POP is explicitly a maximum of actually provided hourly probabilities, not a newly predicted period probability.
- Mid digital-service reform does not prove this selected legacy endpoint changed. Existing selected guide remains the contract until actual response evidence; no new external service added.
- Field-level ultra priority only within the next six hours; null preferred fields do not remove short values. Each selected field retains source metadata.

## Error and privacy rules

- 03: one previous-release fallback. 22/429: family quota gate until next KST date, persisted in DataStore. 30/auth/XML-key errors: no retry. Timeouts: at most two exponential retries.
- Concurrent same cache request shares one in-flight load; valid cache causes no HTTP request. Cancellation propagates; independent sections retain their results.
- No HTTP logging interceptor. Only aggregate body byte counts exposed to diagnostics. All production requests require the approved HTTPS host.
- Runtime station lookup sends no user coordinates. Station catalog is the public metadata API; local nearest selection and cached grid-to-station link.
- Winter apparent-temperature WSD mean interval remains UNCONFIRMED: do not mark ENH-006 fully complete without an authoritative matching definition.

## Live validation state

Initial Pixel_9_Pro live nowcast smoke returned an HTTP 401/403-category access/configuration failure. Exact key values and response bodies were not inspected or recorded. Live test stays a separately labelled LargeTest and is NOT treated as PASS. Local fixture/coordinator tests run independently while the user checks service approval/key configuration. After supporting raw and percent-encoded general key input, a separate real nowcast smoke passed on Pixel_9_Pro (no skip). Initial failure cause is not conclusively isolated; no claim that a separately issued Decoding key is required. Short/ultra and zero-additional-traffic cache checks are validated separately.

## Midnight mid horizon

The selected mid endpoint anchors day offsets to the actual 06/18 release date. Between midnight and the new 06 release, previous-day 18 forecasts end at today's +9. Today's +10 slot must therefore say unprovided until a successful new release supplies it. Eleven calendar slots do not justify fabricated extrema, AM/PM copies, or shifting the previous release by one day. This resolves IMPLEMENTATION_PLAN TASK-005's older "no empty values" wording in favor of ENH-003's missing-data and actual-resolution rules. Real land/temperature responses succeeded; the first live assertion incorrectly required values in all eleven slots and was corrected to the actual release horizon.
- TASK-006 live PASS: complete public station catalog ver=1.1 (dmX longitude/dmY latitude), hourly air ver=1.5, maximum 24 real entries, no additional body traffic on valid-cache refresh. Duplicate station names with different reference coordinates are excluded because selected realtime operation accepts names. Catalog replacement is transactional; weather cache clearing preserves metadata. Fallback replaces a whole station only when latest valid PM coverage improves; different stations are never blended.
- TASK-007 live PASS: ASOS daily nine published dates, zero extra traffic on unchanged cached range. Actual daily records carry validFrom/validTo (KST day) and receivedAt; no invented instantaneous observedAt. Unpublished dates have no completion marker, valid rows preserve null indicators, and retries request missing contiguous gaps only. Cache completion markers never bypass checks for evicted observation rows.

## 겨울 WSD 입력 규격 확인 (2026-11-25)

- 공식 초단기실황 설명: 대표 AWS 관측값(https://data.kma.go.kr/data/rmt/rmtList.do?code=400&pgmNo=570).
- 기상청 누리사랑방 설명의 10분 간격 수집은 평균시간과 별개(https://data.kma.go.kr/community/nuriLovePopup.do). API WSD가 체감온도식의 10분 평균풍속과 동일하다는 대응 근거는 미확보. `INPUT_SPEC_UNCONFIRMED` 유지, `windMeanConfirmed=true` 실운영 전달 금지. 새 API·권한 도입 없음.
