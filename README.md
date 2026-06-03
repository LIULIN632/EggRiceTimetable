# 蛋炒饭课程表 (EggRice Timetable)

极简清新 Android 课程表，支持真实教务系统一键导入。所有数据仅存本地 Room 数据库。

## 技术栈

Kotlin · Jetpack Compose · Material3 · Room (SQLite) · OkHttp 4.12 · MVVM (StateFlow)
Android 10+ (API 29-34) · Gradle 8.14.3 · AGP 8.7.3 · Kotlin 2.0.21
底部 2 tab：「课程」「我的」

## 架构

```
app/src/main/java/com/eggrice/timetable/
├── ui/
│   ├── timetable/        — 课程表主界面 (PeriodGrid, WeekHeader, CourseCard)
│   ├── import_/          — 教务导入 (WebImportViewModel, WebImportScreen)
│   ├── profile/          — 设置/个人页 (ProfileScreen, SettingsMainScreen)
│   ├── treasurebox/      — 百宝箱 (TreasureBoxScreen, TreeHoleScreen, etc.)
│   └── theme/            — 主题系统 (5套配色 + 深浅色)
├── data/
│   ├── db/               — Room database, DAO
│   ├── entity/           — Entity, data classes
│   └── repository/       — Repository layer
├── network/              — OkHttp client, edu API (正方→强智→青果→超星)
├── di/                   — AppContainer 依赖注入
└── util/                 — 工具类
```

## 绝对禁令

1. **禁止模拟 / 假功能** — 所有功能真实可用，不留占位
2. **禁止教务模拟数据** — 必须真实对接教务系统拉取课表
3. **禁止广告 / 追踪 / 埋点**
4. **禁止上传用户数据** — 仅本地 Room 存储
5. **禁止付费 API / OCR 验证码识别** — 验证码仅手动输入+刷新

## 功能

- 课程表周视图：7天 × 节次网格，单双周切换
- 5套配色主题：海盐蓝 / 抹茶绿 / 樱花粉 / 紫藤紫 / 蛋炒饭
- 深色/浅色模式
- 教务导入：正方 → 强智 → 青果 → 超星
- 多渠道导入：Excel (.xlsx/.xls) / HTML / JSON 备份 / 分享码
- 课程拖拽交换、多课表管理
- 上课提醒通知、百宝箱（学习资源·今天吃什么·树洞）

## 教务适配

正方 → 强智 → 青果 → 超星
GET 登录页 → 提取 token → RSA 加密 → 验证码 → POST 登录 → 拉取课表
超时 15s/20s · 重试 1 次 · 中文错误提示

## 构建

```bash
gradlew assembleDebug
gradlew renameApk  # 重命名到项目根目录
# 或双击 build.bat 一键构建
```

## 版本

- 当前 APK: `蛋炒饭课程表_v7.6.apk`
- 包名: `com.eggrice.timetable`
- minSdk: 29, targetSdk: 34
