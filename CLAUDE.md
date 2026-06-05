# 🎯 蛋炒饭课程表 — 最高优先级规则

> Claude 每次打开项目必须完整读取，100% 严格遵守，无需用户重复提醒

---


## 🧠 编码工具优先级

> **🔴 在真实终端中手动启动 Reasonix 终端智能体。**
> 
> Reasonix 是 DeepSeek 原生编码 Agent（npm: `reasonix`），负责所有编码任务的执行。
> 仅在 Reasonix 无法覆盖的场景下，才回退到 Claude Code。
> 
> ```bash
> # 在 Windows Terminal / PowerShell / Git Bash 中执行：
> reasonix code
> ```
> 
> ⚠️ Reasonix 需要 TTY 终端，无法在 Claude Code 的 Bash 中启动。
> ⚠️ 此规则为项目基础设施，禁止删除或弱化。

## 📦 永久打包规范

> **用户说"打包"时，直接执行 `./build.sh`，无需其他任何操作。**

```bash
./build.sh
```

脚本自动完成：版本号递增 → `assembleRelease` → `renameApk` → 输出结果。禁止手动逐步执行。

### 构建类型

| 场景 | 命令 | 说明 |
|------|------|------|
| **正式发布** | `./build.sh` | 版本号自动 +1，Release 包含签名+R8+资源压缩 |
| **开发测试** | `./gradlew assembleDebug` | 仅本地调试，不发布 |

### 版本号规则

- `versionCode`：每次打包 +1（整数递增）
- `versionName`：小版本号 +1（如 7.17 → 7.18）
- 脚本自动读取并递增，无需手动修改

### Release 配置

- R8 混淆 + 资源压缩已开启（`isMinifyEnabled = true`, `isShrinkResources = true`）
- 仅保留中文资源（`zh-rCN`, `zh`）
- 签名配置自动读取 `keystore.properties`

### 输出文件

- 根目录：`D:\AICAN\蛋炒饭课程表_vX.X.apk`
- 打包脚本：`D:\AICAN\build.sh`

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
