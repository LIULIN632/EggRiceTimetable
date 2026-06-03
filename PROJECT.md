# 蛋炒饭课程表 (EggRice Timetable)

> 极简 Android 课程表，Kotlin + Jetpack Compose。支持真实教务系统一键导入，所有数据仅存本地。

## 项目基础信息

| 属性 | 值 |
|------|-----|
| 项目名称 | 蛋炒饭课程表 |
| 包名 | `com.eggrice.timetable` |
| 项目类型 | Android 原生应用 |
| 语言 | Kotlin |
| 最低 SDK | 29 (Android 10) |
| 目标 SDK | 34 (Android 14) |
| Gradle | 8.14.3 |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 |

## 项目架构

```
┌─────────────────────────────────────────────┐
│              Jetpack Compose UI              │
│  课程表网格 / 个人页 / 设置 / 百宝箱         │
│  Material3 + 5套配色主题 + 深色/浅色         │
├─────────────────────────────────────────────┤
│               MVVM (StateFlow)               │
│  ViewModel → Repository → Room DAO          │
├─────────────────────────────────────────────┤
│             OkHttp 教务爬虫模块               │
│  正方 / 强智 / 青果 / 超星                   │
│  GET 登录页 → token → RSA → 验证码 → 拉取    │
├─────────────────────────────────────────────┤
│              Room (SQLite)                   │
│  CourseEntity / SchemeEntity / TimeSlotEntity│
└─────────────────────────────────────────────┘
```

## 目录结构

```
D:\AICAN\
├── app/
│   └── src/main/java/com/eggrice/timetable/
│       ├── ui/
│       │   ├── timetable/      — 课程表主页 + 组件
│       │   ├── import_/        — 教务导入页面
│       │   ├── profile/        — 个人页 + 设置
│       │   ├── treasurebox/    — 百宝箱
│       │   └── theme/          — Color.kt + Theme.kt
│       ├── data/
│       │   ├── db/             — Room 数据库 + DAO
│       │   ├── entity/         — 数据实体
│       │   └── repository/     — 数据仓库
│       ├── network/            — OkHttp 教务爬虫
│       ├── di/                 — AppContainer 依赖注入
│       └── util/               — 工具类
├── build.gradle.kts
├── build.bat                   — 一键构建脚本
├── README.md
├── CLAUDE.md
└── PROJECT.md
```

## 核心功能清单

### 课程表展示
- [x] 周视图：7天 × 节次网格，单双周切换
- [x] 课程卡片：课程名、教师、教室、颜色标记、边框样式
- [x] 课程详情弹窗：完整信息 + 编辑/删除
- [x] 手动添加课程：表单 + 验证
- [x] 多课表管理：创建/切换/重命名/删除方案

### 教务系统导入
- [x] 正方教务：RSA 加密登录 → 验证码识别 → JSON API → 课程解析
- [x] 强智教务：WebView 导航 → 页面注入 → HTML 解析
- [x] 青果教务：WebView 导航 → 页面注入 → HTML 解析
- [x] 超星教务：WebView 导航 → 页面注入 → HTML 解析

### 多渠道导入/导出
- [x] Excel 导入 (.xlsx/.xls)
- [x] HTML 导入
- [x] JSON 备份导入/导出
- [x] 分享码导入/导出
- [x] CSV 导出

### 个性化配置
- [x] 5套配色主题：海盐蓝 / 抹茶绿 / 樱花粉 / 紫藤紫 / 蛋炒饭
- [x] 深色/浅色/跟随系统
- [x] 格子高度/圆角/透明度/文字大小可调
- [x] 边框样式：无/实线/虚线
- [x] 显示开关：教师/教室/校区/节次时间/非本周/文字居中/单双周

### 辅助功能
- [x] 上课提醒通知
- [x] 时间段管理（快捷生成上午/下午/晚上）
- [x] 百宝箱：学习资源 / 今天吃什么 / 大学任务清单 / 好物清单 / 留声树洞
- [x] 更新日志
- [x] 崩溃日志导出

## 数据结构

### CourseEntity (Room)
```kotlin
@Entity
data class CourseEntity(
    val name: String,       // 课程名称
    val teacher: String,    // 教师
    val room: String,       // 教室
    val dayOfWeek: Int,     // 星期 (1-7)
    val startSlot: Int,     // 开始节次
    val endSlot: Int,       // 结束节次
    val weeks: String,      // 周次
    val weekType: String,   // all|odd|even
    val colorIndex: Int,    // 颜色索引 (0-14)
    val schemeId: Long,     // 所属课表方案
    val credits: Float,     // 学分
)
```

## 构建命令

```bash
# 编译
.\gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk

# 重命名到根目录
.\gradlew renameApk

# 或双击 build.bat 一键构建
```

## 版本

- 当前 APK: `蛋炒饭课程表_v7.6.apk`
- 包名: `com.eggrice.timetable`
- minSdk: 29, targetSdk: 34
