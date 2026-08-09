# 蛋炒饭课程表

> 编码: `npx reasonix code`（TTY 终端）| 打包: `./build.sh` → 版本号自动+1 → assembleRelease → renameApk

---

## 项目身份

- **路径** `D:\AICAN\danchaofankechengbiao\`
- **APK** `蛋炒饭课程表_vX.X.apk`
- **类型** Android 原生 · Kotlin + Compose + Material3 · 底部 2 tab: 课程 / 我的

## 技术栈

| 层 | 选型 |
|---|------|
| 语言 | Kotlin 2.0.21 |
| UI | Compose + Material3, BOM 2024.03+ |
| 数据 | Room (SQLite), DataStore |
| 网络 | OkHttp 4.12 |
| 架构 | MVVM (StateFlow) |
| 构建 | Gradle 8.14.3 · AGP 8.7.3 |
| 目标 | API 29-34, Android 10+ |

## 教务适配流程

```
正方 → 强智 → 青果 → 超星
GET 登录页 → 提取 token → RSA 加密 → 验证码(手动输入+刷新) → POST 登录 → 拉取课表
超时 15s/20s · 重试 1 次 · 中文错误提示
```

OkHttp 优先；WebView 仅备用兼容。

## 构建

| 场景 | 命令 |
|------|------|
| 正式发布 | `./build.sh`（versionCode/Name 自动+1, R8 混淆+资源压缩, 签名） |
| 开发测试 | `./gradlew assembleDebug` |

- 仅保留中文资源 (zh-rCN, zh)
- 签名配置: `keystore.properties`

## 约束

1. OkHttp + 教务优先 — WebView 仅备用兼容
2. 禁止模拟数据/广告/统计/埋点/追踪
3. 禁止上传用户数据 — 仅本地 Room 存储
4. 禁止付费 API / 第三方 OCR
5. 禁止新建项目 — 仅本目录内迭代
6. 禁止其他命名 APK

## UI 风格

极简清新 · 1:1 对标 Wake Up/时光课表 · 圆角 6-12dp · 低饱和马卡龙 · `#8B95A8` · 深色/浅色适配

## 参考

- 本地: `参考/`（Dawn-Course, WakeupSchedule, shiguangschedule）
- GitHub: dairoot/school-api · openschoolcn/zfn_api · XingHeYuZhuan/shiguang_warehouse
- 竞品: Wake Up, 时光课表

## 优先级

1. 教务导入 → 100% 成功率
2. 性能优化
3. 构建瘦身: R8 + 资源压缩 + 仅中文
