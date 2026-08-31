# 蛋炒饭课程表 — 技术文档

> 包名 `com.eggrice.timetable` · Android 10+ (API 29-34) · Kotlin + Compose

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
app/src/main/java/com/eggrice/timetable/
├── ui/
│   ├── timetable/          — 课程表主页 + 组件
│   ├── import_/            — 教务导入页面
│   ├── profile/            — 个人页 + 设置
│   ├── treasurebox/        — 百宝箱（含成绩查询/修课情况/成绩存档页面）
│   ├── zhengfang/          — 正方「只登录」流程共享组件（ZhengfangLoginViewModel/Ui）
│   └── theme/              — Color.kt + Theme.kt
├── data/
│   ├── db/                 — Room 数据库 + DAO
│   ├── entity/             — 数据实体
│   └── repository/         — 数据仓库
├── network/                — OkHttp 教务爬虫（ZhengfangUtils 公共工具）
├── di/                     — AppContainer 依赖注入
└── util/                   — 工具类
```

## 核心功能清单

### 课程表展示
- [x] 周视图：7天 × 节次网格，单双周切换
- [x] 课程卡片：课程名、教师、教室、颜色标记、边框样式
- [x] 课程详情弹窗：完整信息 + 编辑/删除
- [x] 手动添加课程：表单 + 验证
- [x] 多课表管理：创建/切换/重命名/删除方案
- [x] 课程重叠管理：贪心合并块（Float 坐标）+ 冲突角标 + 底部弹窗切换显示 + 拖拽拆开
- [x] 单双周局部遮罩：非本周课程剩余区域斜纹标注（merge 层算好 visibleRange/maskRanges 并集）
- [x] 拖拽换课：相对更新 DAO（moveByDelta，时序免疫）+ pending 视觉锁 + 合并块整组拖动
- [x] 当前节次高亮

### 教务系统导入
- [x] 正方教务：RSA 加密登录 → 验证码识别 → JSON API → 课程解析
- [x] 正方教务：成功组合记忆（ZhengfangImportMemory，跨会话 30 天 TTL，导入提速）
- [x] 强智教务：WebView 导航 → 页面注入 → HTML 解析
- [x] 青果教务：WebView 导航 → 页面注入 → HTML 解析
- [x] 超星教务：WebView 导航 → 页面注入 → HTML 解析
- [x] 自定义学校：添加 / 编辑 / 删除

### 正方教务查询（试验阶段，仅登录不拉课表）
- [x] 成绩查询：学期列表 + 总评/平时/期末/期中分项，字段别名容错
- [x] 修课情况查询：GPA / 计划内完成统计 / 各类型学分 / 课程明细（正方 v9 xsxy）
- [x] 成绩存档：一键同步全部学期成绩 → 本地 Room 离线查看（SavedGradeEntity）
- [x] 账号密码加密记忆（EncryptedSharedPreferences，跨页面复用登录会话）
- [x] 登录/选校/验证码流程共享组件：ui/zhengfang/（ZhengfangLoginViewModel + ZhengfangLoginUi）

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
- [x] 学校索引热更新：SchoolIndexUpdater（协议版本 + 时间戳版本校验 + 延迟写入），
      tools/generate_school_index.ps1 生成 school_index.json 经 jsDelivr 分发，
      SchoolRegistry 索引优先回退内置 assets；检查更新弹窗手动触发 + 启动时自动检查

### 辅助功能
- [x] 上课提醒通知
- [x] 时间段管理（快捷生成上午/下午/晚上）
- [x] 百宝箱：学习资源 / 今天吃什么 / 大学任务清单 / 好物清单 / 留声树洞
- [x] 更新日志
- [x] 崩溃日志导出

## 实体定义

### CourseEntity
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

### SchemeEntity
```kotlin
@Entity
data class SchemeEntity(
    val name: String,           // 方案名称
    val termStart: String,      // 学期起始日期
    val maxWeeks: Int,          // 最大周数
    val slotCounts: String,     // 每节课时间 JSON
)
```

### TimeSlotEntity
```kotlin
@Entity
data class TimeSlotEntity(
    val slotIndex: Int,     // 节次索引
    val startTime: String,  // 开始时间 HH:mm
    val endTime: String,    // 结束时间 HH:mm
    val schemeId: Long,     // 所属方案
)
```

### SavedGradeEntity（成绩存档，v11 新增）
```kotlin
@Entity(tableName = "saved_grades",
        indices = [Index(value = ["courseName", "termLabel", "totalScore"], unique = true)])
data class SavedGradeEntity(
    val courseName: String, // 课程名称
    val totalScore: String, // 总评成绩
    val credits: String,    // 学分
    val gpa: String,        // 绩点
    val regular: String,    // 平时分
    val final: String,      // 期末分
    val midterm: String,    // 期中分
    val examType: String,   // 考试性质
    val termLabel: String,  // 学年学期标签
    val schoolName: String, // 学校名称
    val savedAt: Long,      // 保存时间
)
```

## 构建产物

构建产出 APK 到项目根目录，仅保留中文资源，R8 混淆 + 资源压缩。
