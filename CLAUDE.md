# 🎯 蛋炒饭课程表 — 最高优先级规则

> Claude 每次打开项目必须完整读取，100% 严格遵守，无需用户重复提醒

---

## 🤖 模型自动选择策略

> 根据任务复杂度自动切换模型，节省 token 成本。

| 任务类型 | 模型 | 示例 |
|---------|------|------|
| **简单任务** | `haiku` | 修改变量名、改文案、单行 bug 修复、读文件、问简单问题 |
| **复杂任务** | `opus` | 多文件重构、调试复杂逻辑、新增功能模块、架构设计、代码审查 |

**判断规则**：
- 预计修改 ≤ 2 个文件、≤ 20 行 → Haiku
- 预计修改 > 2 个文件 或 > 20 行 或涉及架构/新功能 → Opus
- 不确定时默认 Opus，宁可多用不可少用
- Agent 子任务默认用 Haiku（探索、搜索、简单问答），复杂 Agent 显式指定 Opus

---

## 🧠 编码工具优先级

> **🔴 每次会话启动时必须执行：`! reasonix code` 启动 Reasonix 终端智能体。**
> 
> Reasonix 是 DeepSeek 原生编码 Agent（npm: `reasonix`），负责所有编码任务的执行。
> 仅在 Reasonix 无法覆盖的场景下，才回退到 Claude Code。
> 
> ⚠️ 此规则为项目基础设施，禁止删除或弱化。

## 📦 打包（用户说"打包"时执行）

1. 读 `app/build.gradle.kts`，`versionCode` +1，`versionName` 递增
2. `gradlew assembleDebug` 编译
3. `gradlew renameApk` 重命名到根目录
4. 告知版本号、文件名、大小
5. 用户可双击 `build.bat` 自行打包

---

## 🔴 强制执行

1. 禁止虚假回复 → 修改必须实际写入文件
2. 改后验证 → 重新读取确认改动生效
3. 版本递增 → versionCode / versionName 每次 +1
4. 必须生成可安装 APK，零编译错误
5. 说明明细 → 改了什么文件、什么内容、APK 版本号

## 📌 项目身份

蛋炒饭课程表 · `D:\AICAN\` · `蛋炒饭课程表_vX.X.apk` · 纯安卓原生 APP

## ❌ 绝对禁令

1. OkHttp 优先处理教务 → WebView 仅作备用兼容方案
2. 禁止模拟/假功能/占位数据
3. 禁止广告/统计/埋点/追踪
4. 禁止上传用户数据 → 仅本地 Room 存储
5. 禁止新建独立项目 → 仅 `D:\AICAN\` 内迭代
6. 禁止其他名字的 APK
7. 禁止付费 API / OCR 验证码识别 → 验证码仅手动输入+刷新

## ⚙️ 技术栈

Kotlin · Compose + Material3 · Room (SQLite) · OkHttp 4.12 · MVVM (StateFlow)
Android 10+ (API 29-34) · Gradle 8.14.3 · AGP 8.7.3 · Kotlin 2.0.21
底部 2 tab：「课程」「我的」

## 🎓 教务适配

正方 → 强智 → 青果 → 超星
GET 登录页 → 提取 token → RSA 加密 → 验证码 → POST 登录 → 拉取课表
超时 15s/20s · 重试 1 次 · 中文错误提示

## 📚 参考

- 本地源码：`D:\AICAN\参考\`（Dawn-Course、WakeupSchedule、shiguangschedule）
- GitHub：dairoot/school-api · openschoolcn/zfn_api · XingHeYuZhuan/shiguang_warehouse
- 竞品：Wake Up、时光课表

## 🎨 UI

极简清新 · 1:1 对标 Wake Up/时光课表 · 圆角 6-12dp · 低饱和马卡龙 · #8B95A8
加载有进度可取消 · 适配深色/浅色

## 🚩 优先级

1. 修复教务导入 → 100% 导入成功
4. 性能优化
5. 构建瘦身：R8 + 资源压缩 + 仅中文
