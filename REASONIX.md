# 蛋炒饭课程表 (EggRice Timetable)

Kotlin · Jetpack Compose · Material3 · Android 10+ (API 29-34)
Project root: D:\AICAN\ · APK: 蛋炒饭课程表_vX.X.apk

## Tech stack
Kotlin 2.0.21 · Compose + Material3 · Room (SQLite) · OkHttp 4.12.0 · MVVM (StateFlow)
Gradle 8.14.3 · AGP 8.7.3 · minSdk 29 · targetSdk 34

## Directory map
```
app/src/main/java/com/eggrice/timetable/
├── ui/
│   ├── timetable/    — 课程表主界面 (PeriodGrid, WeekHeader)
│   ├── import_/      — 教务导入 (WebImportViewModel, WebImportScreen)
│   └── profile/      — 设置/个人页
├── data/
│   ├── db/           — Room database, DAO
│   ├── model/        — Entity, data classes
│   └── repository/   — Repository layer
├── network/          — OkHttp client, edu API (正方→强智→青果→超星)
├── di/               — AppContainer dependency injection
└── util/             — Utils
```

## Key rules
- OkHttp first for edu system import; WebView is fallback only
- No mock/fake features or placeholder data
- No ads, analytics, tracking
- Local Room storage only — no data upload
- Captcha is manual input + refresh — no paid OCR
- Chinese error messages for all import failures
- UI: minimal, clean, 6-12dp radius, low-saturation pastel, #8B95A8 accent
- Bottom 2 tabs: 课程 (Timetable) · 我的 (Profile)

## Edu system adapters (try in order)
正方 → 强智 → 青果 → 超星
Flow: GET login page → extract token → RSA encrypt → captcha → POST login → fetch timetable
Timeout: 15s/20s · Retry: 1x

## Build
```
gradlew assembleDebug
gradlew renameApk  # renames APK to project root
```
Also: double-click build.bat for manual build.

## References
- Local: D:\AICAN\参考\ (Dawn-Course, WakeupSchedule, shiguangschedule)
- GitHub: dairoot/school-api · openschoolcn/zfn_api · XingHeYuZhuan/shiguang_warehouse
- Competitors: Wake Up, 时光课表
