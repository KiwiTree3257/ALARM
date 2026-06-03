# alarm_app — 프로젝트 컨텍스트

## 프로젝트 개요

Flutter 기반 Android 알람 앱. 단순한 알람이 아닌 **무조건 깨우는** 알람 앱.

- 주요 플랫폼: **Android** (iOS는 부차적)
- 현재 상태: **1단계 완료** — 패키지 설치, Android 권한/서비스 설정, Kotlin 서비스 스켈레톤 완성

---

## 핵심 기능 요구사항

### 1. 기본 알람 기능
- 알람 소리 지정 (커스텀 사운드)
- 볼륨 조절
- 반복 설정 등 일반 알람 앱 기본 기능

### 2. 알람 등급 시스템 (A, B, C, D...)
- 사용자가 알람마다 등급을 사전 설정
- 앱 기본값은 강력한 방향으로 설정
- 등급별 차이:

| 등급 | 소리 | 진동 | 재취침 감지 허용 시간 | 특이사항 |
|------|------|------|----------------------|---------|
| A (약) | 낮은 볼륨 | 없음 | 넓은 허용 | 일반 알람 수준 |
| B | 중간 | 약 | 보통 | |
| C | 높음 | 강 | 좁은 허용 | 볼륨 버튼 차단 |
| D (강) | 최대 | 최강 | 매우 좁음 | 화면 끄기 차단 |

### 3. 반칙 방지 (Android)
- 알람 정지 버튼은 존재하지만, **정지 후 재취침 감지 시 자동 재울림**
- 볼륨 버튼으로 소리 줄이기 차단 (등급 C 이상)
- 잠금화면 위에 알람 화면 표시 (Full-Screen Intent)
- 앱이 꺼져도 알람 작동 (Foreground Service)

### 4. 기상 확인 시스템 (재취침 방지)
```
알람 울림 → 사용자가 정지 → [기상 모니터링 시작 (30분)] → 재취침 판단 → 재울림
```

**감지 방식:**
- 가속도계(Accelerometer): 폰이 N초 이상 정지 → 재취침 판단
- 주기적 확인 알림: 5분마다 "아직 깨어 있나요?" → M초 내 응답 없으면 재울림

### 5. 미션 시스템
- 정지 후 확인을 위한 기상 미션
- **미션 종류는 추후 사용자가 직접 구현 예정** (뼈대만 먼저 만들어 둘 것)
- 확장 가능한 추상 구조로 설계:

```dart
abstract class AlarmMission {
  String get name;
  int get difficulty; // 1~5
  Widget buildMissionUI();
  Future<bool> isSolved();
}
```

---

## 기술 스택

### Flutter 패키지
- `alarm` — Android AlarmManager 기반 정확한 알람 스케줄링
- `flutter_foreground_task` — 백그라운드 포그라운드 서비스
- `sensors_plus` — 가속도계 (재취침 감지)
- `audioplayers` — 커스텀 알람 소리 재생
- `flutter_local_notifications` — 주기적 기상 확인 알림

### Android 네이티브 구성
- `AlarmManager` (ExactAlarm) — 정확한 시간 보장
- `AudioManager` — 볼륨 버튼 잠금
- `WakeLock` — 화면/CPU 강제 유지
- `Full-Screen Intent` — 잠금화면 침투
- `AlarmForegroundService` (Kotlin) — 앱 종료 후에도 생존
- `WakeMonitorService` (Kotlin) — 정지 후 재취침 감지

### Android 권한 (AndroidManifest.xml)
- `SCHEDULE_EXACT_ALARM`
- `USE_EXACT_ALARM`
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`
- `RECEIVE_BOOT_COMPLETED` (재부팅 후 알람 복구)
- `USE_FULL_SCREEN_INTENT`

---

## 아키텍처

```
┌─────────────────────────────────────────┐
│          Flutter (UI / 설정)            │
├─────────────────────────────────────────┤
│     AlarmForegroundService (Kotlin)     │  ← 앱 꺼져도 생존
│     WakeMonitorService (Kotlin)         │  ← 정지 후 재취침 감지
├─────────────────────────────────────────┤
│  AlarmManager (ExactAlarm)              │
│  AudioManager (볼륨 버튼 잠금)          │
│  Accelerometer (움직임 감지)            │
│  WakeLock (화면/CPU 강제 유지)          │
│  Full-Screen Intent (잠금화면 침투)     │
└─────────────────────────────────────────┘
```

---

## 개발 순서

1. ✅ **기반 설정** — 패키지 추가, Android 권한 설정, 포그라운드 서비스
2. ⬜ **알람 기본 기능** — 생성/삭제/목록, 소리/볼륨 설정
3. ⬜ **알람 등급 시스템** — A~D 등급 설정 UI
4. ⬜ **잠금화면 침투 + 볼륨 차단** — 반칙 방지 핵심
5. ⬜ **기상 모니터링** — 가속도계 + 재확인 알림
6. ⬜ **미션 시스템 뼈대** — 추후 미션 추가를 위한 추상 구조
7. ⬜ **UI/UX 완성**

---

## 1단계 완료 상세 (2026-06-03)

### 설치된 패키지 (pubspec.yaml)
- `alarm ^5.4.1` — AlarmManager 기반 정확한 알람 스케줄링
- `flutter_foreground_task ^8.13.0` — 포그라운드 서비스 관리
- `sensors_plus ^6.1.0` — 가속도계 (재취침 감지)
- `audioplayers ^6.4.0` — 커스텀 알람 소리 재생
- `flutter_local_notifications ^18.0.0` — 주기적 기상 확인 알림
- `permission_handler ^11.4.0` — 런타임 권한 요청

### Android 설정
- `minSdk = 23` (build.gradle.kts) — SCHEDULE_EXACT_ALARM 요구사항
- AndroidManifest.xml에 권한 10개 선언
- `showWhenLocked`, `turnScreenOn` — 잠금화면 침투 속성 추가

### 생성된 Kotlin 파일
| 파일 | 역할 | 상태 |
|------|------|------|
| `AlarmForegroundService.kt` | WakeLock + 등급별 볼륨 정책 | 스켈레톤 완성 |
| `WakeMonitorService.kt` | 가속도계 정지 감지 + 주기 확인 | 스켈레톤 완성 |
| `BootReceiver.kt` | 재부팅 후 알람 복구 수신 | 스켈레톤 완성 |

### main.dart
- `Alarm.init()` 초기화
- `FlutterForegroundTask.initCommunicationPort()` 초기화
- 런타임 권한 요청 화면 (scheduleExactAlarm, notification, ignoreBatteryOptimizations)

### 미연결 항목 (이후 단계에서 처리)
- Flutter ↔ Kotlin 간 MethodChannel 연결 (2단계)
- `WakeMonitorService.triggerReAlarm()` → Flutter 콜백 (5단계)
- 볼륨 버튼 차단 `MainActivity.onKeyDown` (4단계)
- `WakeMonitorService.sendWakeCheckNotification()` 구현 (5단계)
- 등급별 `stillThresholdSec` 파라미터 연결 (3단계)
