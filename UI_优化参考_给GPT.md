# 蛋炒饭课程表 — 前端 UI 优化参考文档

> 将本文档完整复制给 GPT，让 GPT 理解项目后给出 UI 优化方案代码，再交回给我实施。

---

## 1. 项目概览

- **名称**: 蛋炒饭课程表 (Android 课表 App)
- **技术栈**: Kotlin · Jetpack Compose · Material3 · Room (SQLite) · MVVM
- **设计风格**: 极简清新 · 圆角 6-12dp · 低饱和马卡龙配色 · 支持深色/浅色模式 · 对标 Wake Up/时光课表
- **底部导航**: 2 tab —「课程」「我的」
- **主要色系**: 海盐蓝 `#6B95CF` / 抹茶绿 `#7CB342` / 樱花粉 `#F48FB1` / 紫藤紫 `#9575CD` / 蛋炒饭黄 `#F6C84C` + 薄荷绿 `#73D9A5` (5套主题可切换)

## 2. 主题色彩系统 (Color.kt)

```kotlin
// ═══ 海盐蓝主色 ═══
val Accent = Color(0xFF6B95CF)
val AccentLight = Color(0xFF8AB4F8)
val AccentSoft = Color(0xFFE3F2FD)

// ═══ 抹茶绿 ═══
val AccentGreen = Color(0xFF7CB342)
val AccentGreenLight = Color(0xFFAED581)
val AccentGreenSoft = Color(0xFFF1F8E9)

// ═══ 樱花粉 ═══
val PinkAccent = Color(0xFFF48FB1)
val PinkAccentLight = Color(0xFFF8BBD0)
val PinkSoft = Color(0xFFFCE4EC)

// ═══ 紫藤紫 ═══
val PurpleAccent = Color(0xFF9575CD)
val PurpleAccentLight = Color(0xFFB39DDB)
val PurpleSoft = Color(0xFFEDE7F6)

// ═══ 炒饭黄 ═══
val FriedAccent = Color(0xFFF6C84C)
val FriedAccentLight = Color(0xFFFFD95A)
val FriedAccentSoft = Color(0xFFFFF5D6)
val DarkFriedAccentSoft = Color(0xFF3A3028)

// ═══ 辅助暖色 ═══
val OrangeAccent = Color(0xFFF7B787)
val OrangeSoft = Color(0xFFFFF4EB)

// ═══ Light theme neutral ═══
val Surface = Color(0xFFFAFAFA)
val SurfaceCard = Color(0xFFFFFFFF)
val SurfaceAlt = Color(0xFFF5F5F5)
val CardBorder = Color(0xFFE0E0E0)
val Divider = Color(0xFFEEEEEE)
val TodayBg = Color(0xFFE8F0FE)
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF424242)
val TextTertiary = Color(0xFF757575)

// ═══ Dark theme neutral ═══
val DarkSurface = Color(0xFF1A1C1E)
val DarkSurfaceAlt = Color(0xFF212325)
val DarkSurfaceCard = Color(0xFF2A2C2E)
val DarkCardBorder = Color(0xFF383A3C)
val DarkDivider = Color(0xFF303234)
val DarkTextPrimary = Color(0xFFE8E8E8)
val DarkTextSecondary = Color(0xFF9E9E9E)
val DarkTextTertiary = Color(0xFF757575)

// ═══ Semantic ═══
val DangerColor = Color(0xFFE57373)
val SuccessGreen = Color(0xFF4CAF50)
val BorderLight = Color(0xFFE0E0E0)
val IconTertiary = Color(0xFFBDBDBD)

// 15色马卡龙课程卡片颜色 (light + dark 各一套)
val CourseColors = listOf(0xFFFFE8E5, 0xFFDEE8FF, ...) // 15 pastel colors
val CourseColorsDark = listOf(0xFF5C3A38, 0xFF38405C, ...)
```

**主题切换方式**: 通过 `LocalDarkMode` + `LocalThemeType` CompositionLocal 获取当前主题。5套主题各有独立 Material3 ColorScheme，通过 `EggRiceTheme` 入口切换。全局 accent 颜色由 `@Composable` 函数 `accentColor()` / `accentLightColor()` / `accentSoftColor()` 根据 `LocalThemeType.current` 动态返回。每个 Composable 内用 `val isDark = LocalDarkMode.current` 判断深浅色。

---

## 3. 核心 UI 文件结构

```
ui/
├── navigation/AppNavigation.kt     — 底部导航 + NavHost
├── timetable/
│   ├── TimetableScreen.kt          — 课程表主页 (顶部栏 + WeekHeader + PeriodGrid)
│   └── components/
│       └── PeriodGrid.kt           — 课表网格 (核心组件 ~500行)
├── profile/
│   ├── ProfileScreen.kt            — "我的"页面 (~2000行, 含大量Dialog)
│   └── SettingsMainScreen.kt       — 课表设置二级页面 + 学期设置
├── treasurebox/
│   ├── TreasureBoxScreen.kt        — 百宝箱主页 (5个渐变卡片入口)
│   ├── TaskChecklistScreen.kt      — 大学任务清单
│   ├── GoodItemListScreen.kt       — 大学好物清单
│   └── TreeHoleScreen.kt           — 留声树洞
└── theme/
    ├── Color.kt                    — 所有颜色定义
    └── Theme.kt                    — Material3主题 + 5套配色方案
```

---

## 4. 各页面当前状态

### 4.1 AppNavigation.kt — 底部导航

```kotlin
// 底部导航栏: 56dp高, 图标20dp, 文字10sp
// 有子页面时自动隐藏 (isSubPage=true)
NavigationBar(
    containerColor = SurfaceCard,
    tonalElevation = 4.dp,
    modifier = Modifier.shadow(12.dp).height(56.dp)
) {
    // 课程 tab + 我的 tab
    NavigationBarItem(icon = 20.dp, label = 10.sp)
}
```

### 4.2 TimetableScreen.kt — 课程表主页

```kotlin
// 顶部栏: 第X周 + 日期范围 + 左右箭头 + "今天"按钮
// 紧凑设计: 14sp / 9sp, padding 2dp
Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)) {
    Column { Text("第${currentWeek}周", 14.sp); Text("日期范围", 9.sp) }
    IconButton(28.dp) { ChevronLeft }
    Button(height=26.dp, rounded=13.dp) { "今天" }
    IconButton(28.dp) { ChevronRight }
}

// WeekHeader — 显示周一~周日日期 + 作业入口
// PeriodGrid — 课表网格
// FAB: 非本周显示"回本周"按钮 + 添加课程圆形FAB
```

### 4.3 PeriodGrid.kt — 核心课表网格

```kotlin
// 参数众多 (~20个), 所有外观由外部 StateFlow 驱动
fun PeriodGrid(
    timeSlots, courses, currentWeek, isCurrentWeek,
    showTeacher, showRoom, showCampus, showSlotTime,
    showDashedBorder, textCentered, gridHeight, cornerRadius,
    gridOpacity, gridTextSize, showOddEven, borderStyle,
    nonCurrentCourses, showNonCurrentWeek, vibrationMode,
    homeworkCourseNames,
    onCourseClick, onEmptyCellClick, onCourseMoved
)

// 结构: Box(verticalScroll) → 12个时间段行 (Row: 32dp侧边栏 + 7天cell)
//   Layer 1: 网格背景 (带圆角、边框样式、今天高亮、当前节次高亮)
//   Layer 2a: 非本周课程 (半透明虚线幽灵卡片)
//   Layer 2b: 本周课程卡片 (绝对定位, 支持拖拽交换)
//   Layer 3: 拖拽浮动卡

// CourseCardContent — 课程卡片内容:
//   课程名(maxLines=3, softWrap, Clip) + 作业黄色Warning图标
//   信息行: 非本周/教师/教室(可隐藏校区)/单双周
```

### 4.4 ProfileScreen.kt — "我的"页面

```kotlin
// LazyColumn 结构:
//   UserProfileArea     — 头像(40dp) + 昵称(15sp) + 编辑按钮
//   Row1: 导入 + 分享   — 2个 DualButton (72dp高)
//   Row2: 课表设置 + 通用设置
//   Divider
//   课表管理 / 百宝箱 / 清理缓存 / 导出日志 / 检查更新 / 更新日志 / 关于 / 清空数据
//   (每项 ArrowRow: 56dp高, 14sp标题, 右箭头)

// 包含 ~20 个 Dialog/AlertDialog 的显示逻辑
// 所有弹窗在当前 Composable 内用 if/return 切换
```

### 4.5 TreasureBoxScreen.kt — 百宝箱

```kotlin
// Scaffold + TopAppBar + verticalScroll Column
// 5个 ToolCard (fillMaxWidth, 110dp高):
//   圆角16dp, 水平渐变背景, 圆形白色半透明图标区(52dp) + 标题(18sp) + 副标题(12sp) + 箭头
//   学习资源 (紫蓝渐变) / 今天吃什么 (粉红渐变) / 大学任务清单 (青绿渐变)
//   大学好物清单 (橙红渐变) / 留声树洞 (棕色渐变)
```

### 4.6 SettingsMainScreen.kt — 设置页面

```kotlin
// SettingsMenuItem: 40dp圆角图标区 + 标题(15sp) + 副标题(12sp) + 右箭头
// SettingsSwitchItem: 同上 + Switch开关
// SemesterSettingsPage: 滚动Column, 3个Card (当前周次显示 + 快捷设置 + 日期WheelPicker + 总周数)
```

---

## 5. 现有 UI 问题 & 可优化方向

请 GPT 针对以下方向给出优化建议和修改代码：

1. **ProfileScreen 过于臃肿** — 单文件 ~2000 行，所有 Dialog 堆在一起，可考虑拆分
2. **课程卡片信息展示** — 当前 CourseCardContent 用 maxLines=3 + Clip，小格子内容会被裁切，是否有更好方案
3. **百宝箱卡片设计** — 5 张渐变卡片风格统一但缺乏辨识度
4. **设置页面层级** — 弹窗套弹窗的交互体验 (AlertDialog → 二级AlertDialog)
5. **空状态/加载态** — 各页面空状态设计不一致
6. **动画过渡** — 页面切换仅有 fadeIn/fadeOut，可丰富转场动画
7. **深色模式一致性** — 某些地方使用了硬编码颜色而非主题色
8. **字体大小可读性** — 部分文字偏小 (9sp, 10sp)，低端机可能看不清
9. **触摸反馈** — 部分可点击区域缺少 Ripple 或缩放动画反馈
10. **课程网格性能** — PeriodGrid 使用了大量绝对定位和 remember 计算

---

## 6. 输出要求

请 GPT 按以下格式输出：

```
### [优化项名称]

**当前问题**: 一句话描述
**优化方案**: 具体方案说明
**修改文件**: `path/to/file.kt`
**修改代码**:
```kotlin
// 完整的新代码 (可直接替换)
```
```

请 GPT 只输出有把握的、可直接实施的方案，避免泛泛而谈。
