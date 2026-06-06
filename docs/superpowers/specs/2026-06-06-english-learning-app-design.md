# 英语学习 App 设计文档

> CET-4/6 词汇 · 闪卡背诵 · 自测考察 · 真题训练
> Android 原生 · Kotlin + Compose + Room + OkHttp + MVVM

## 项目基础信息

| 属性 | 值 |
|------|-----|
| 项目名称 | （待定） |
| 包名 | `com.eggrice.english` |
| 项目类型 | Android 原生应用 |
| 语言 | Kotlin |
| 最低 SDK | 29 (Android 10) |
| 目标 SDK | 34 (Android 14) |
| Gradle | 8.14.3 |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 |

## 底部导航 (4 Tab)

| Tab | 图标 | 功能 |
|-----|------|------|
| **背诵** | 大脑/记忆 | 闪卡模式（秒/想了一下）+ 列表模式，浏览记忆单词 |
| **考察** | 笔/对勾 | 看英文想中文，点开答案，自判对错，支持反向 |
| **真题** | 文件/证书 | 听力（音频+选题）/ 阅读（文章+答题）/ 翻译（输入+答案对比）|
| **我的** | 人物 | 学习统计 + 设置 + 数据管理 |

## 核心功能

### 1. 背诵 — 单词记忆

**闪卡模式：**
- 显示英文单词 + 音标
- 点击卡片翻转显示中文释义 + 例句
- 下方两按钮：
  - 🟢 秒 — 瞬间回忆出意思，已掌握
  - 🟡 想了一下 — 有犹豫，半熟状态
- 记录反应时间 → 秒过的词降低复习频率，慢的词加重复习

**列表模式：**
- 按单元分组，按字母搜索
- 每行显示单词 + 掌握状态（秒 / 已掌握 / 待复习）
- 左右滑动切换模式

### 2. 考察 — 自测对错

- 显示英文单词
- 用户脑中回忆中文意思 → 点击"显示答案"
- 看到释义 + 例句
- 点击"我记得"或"没想起来"
- 支持反向：看中文想英文
- 记录正确/错误次数 → 错词自动加入复习队列
- 可选单元范围、数量

### 3. 真题 — 考试真题

**听力真题：**
- 播放音频（assets 预置 mp3）
- 显示 4 选 1 选择题
- 提交后显示答案 + 听力原文

**阅读真题：**
- 显示文章 + 选择题
- 提交后显示正确答案 + 解析

**翻译真题：**
- 显示中文 → 用户输入英文 → 点击对比参考答案
- 自动高亮差异

### 4. 我的 — 个人中心

- 学习统计：连续天数、掌握单词数、正确率曲线
- 反应速度曲线（秒/慢分布）
- 词库管理：选择 CET-4 / CET-6 范围
- 数据管理：导入/导出学习进度、重置数据
- 设置：深色/浅色主题、字体大小

## 数据模型

```kotlin
@Entity WordEntity(
    id: Long,
    word: String,           // 英文单词
    phonetic: String,       // 音标
    meaning: String,        // 中文释义
    example: String,        // 例句
    exampleMeaning: String, // 例句翻译
    level: String,          // "cet4" | "cet6"
    unit: Int               // 单元编号
)

@Entity StudyRecordEntity(
    wordId: Long,
    status: String,         // "fast" | "slow" | "unknown"
    correctCount: Int,
    wrongCount: Int,
    lastReviewDate: Long,
    nextReviewDate: Long    // 艾宾浩斯复习日期
)

@Entity ExamQuestionEntity(
    id: Long,
    type: String,           // "listening" | "reading" | "translation"
    content: String,        // 题目内容
    options: String,        // JSON 选项数组
    answer: String,         // 正确答案
    analysis: String,       // 解析
    year: Int,              // 年份
    month: Int,             // 月份
    audioPath: String       // 听力音频路径（可为空）
)
```

## 架构

```
com.eggrice.english/
├── ui/
│   ├── memorize/       — 背诵（闪卡/列表）
│   ├── quiz/           — 考察（自测）
│   ├── exam/           — 真题（听力/阅读/翻译）
│   └── profile/        — 个人中心
├── data/
│   ├── db/             — Room 数据库 + DAO
│   ├── entity/         — 数据实体
│   └── repository/     — 数据仓库
├── util/               — 工具类
└── MainActivity.kt
```

## 数据来源

- 词汇数据：本地 JSON 预置 assets/words_cet4.json, words_cet6.json
- 真题数据：本地 JSON 预置 assets/exam_questions.json
- 听力音频：本地 assets/audio/ 目录
- 无需网络 —— 全部离线可用

## 设计原则

- 极简清新 UI，同蛋炒饭课程表风格
- 100% 离线，无广告无统计无数据上传
- 学习进度仅存本地 Room
- 不做社交、不做付费、不做云端同步
