# 星月英语 Android

星月英语是面向 Android 手机端的英语听读学习 App。当前交付面是 Android App 与共享 Core 逻辑，旧桌面端源码、桌面运行资源、VLC/Wix/Rust JNI 子工程不作为本轮交付范围。

## 当前交付版本

- 版本: `2.16.1`
- versionCode: `13`
- Release APK: `delivery/星月英语-2.16.1-release.apk`
- APK 大小: `179,858,191` bytes，约 `171.53 MiB`
- SHA256: `9E5AA81783685A04F473D285D24A682075ABAC81BE7E92686671F09FD2472275`
- 签名: APK Signature Scheme v2 验证通过
- 签名证书 SHA-256: `696b113850caee457f3f7516b17ab3cb79532828426b58c8836298d14142a1bf`
- 包名: `com.xingyue.english`
- 启动 Activity: `com.xingyue.english.android.XingYueEnglishActivity`

## 2.16.1 更新重点

- 学习产品重建: 新增 `EnglishLearningArchitecture`，把功能按“输入、理解、词汇、记忆、输出、支持、反馈”分层，防止游戏/统计挤占英语学习主线。
- 学习页拆分: 从拥挤功能堆叠改为 `今日路径 / 练习舱 / 词库阶梯 / 能力地图` 四个分区。
- 练习闭环补强: 打字背词/听写/逐句默写改为小组练习，显示本组进度、正确数、平均准确率；完成后写入练习会话和今日学习路径。
- 真实交互修复: 未判定当前题时点击“下一题”会提示“先判定本题，再进入下一题”；普通单词输入改为单行 Done 动作，判定后自动收起键盘。
- TTS 配置补强: 我的页使用显式开关和状态文案展示云端 TTS 开启/关闭，避免“配置了但不知道是否生效”。
- 交付文档补充: 新增 `docs/learning-product-rebuild.md`，说明前端分区、本地服务层和未来后端拆分边界。

## 当前能力

- 本地导入: 支持 SRT/VTT/ASS/SSA、TXT、JSON、音频、视频和部分文档素材。
- 直链导入: 支持 SRT/TXT/JSON、10 MB 内音频和常见视频地址；`text/html` 网页不会误判成文档。
- 平台链接导入: YouTube/Bilibili/Douyin 公开链接进入 Android 端内置 `yt-dlp` 链路。只处理公开、用户有权访问、无登录、无付费、无 DRM 内容。
- 播放页: 双语时间线、点词查词、手动收词、句子重播、英文/中文/双语 TTS 入口。
- 学习页: 今日路径负责主线，练习舱负责主动回忆，词库阶梯负责通用英语/IELTS/TOEFL 阶段，能力地图解释学习状态。
- TTS 与词典: 百炼 CosyVoice 云端 mp3 和本地 Room/cache 优先，系统 TTS 兜底；云端词典补全成功后回写本地。

## 学习结构原则

星月英语不是把所有功能平铺给用户，而是每天执行一条学习路径:

1. 导入真实素材。
2. 生成可理解的双语字幕或段落。
3. 逐句精听和重播。
4. 用户手动收词。
5. 主动回忆练习。
6. FSRS 间隔复习。
7. TTS、词典、统计提供反馈。

Wordle、成就、看描述猜词只作为低压力巩固，不阻塞今日主线。详细重建说明见 `docs/learning-product-rebuild.md`。

## 第三方来源

| 项目 | 本地归档 | Commit | 许可证 | 借鉴范围 |
| --- | --- | --- | --- | --- |
| TypeWords | `third_party/upstream/TypeWords/` | `0de1e75d41f7438b738a5e83700b49e3d1e3c858` | GPL-3.0 | 打字背词、听写、自测、逐句默写、错词/熟词/收藏、FSRS 学习闭环 |
| WordHub | `third_party/upstream/WordHub/` | `fa0c50c4acd28e857dba5d985edf8c052369dadd` | MIT | 词库管理、查询历史、学习/复习、成就、统计、Wordle、看描述猜词 |
| ChattyPlay-Agent | 未归档运行时 | upstream GitHub | 以原仓库为准 | 只参考“解析中/缩略图/平台标签/失败原因”的交互表达，不嵌入 Web/Hono 服务或第三方代理 |

用户/项目方声明已获得 TypeWords 和 WordHub 合作借鉴同意；正式授权记录由项目方留存。本仓库只记录来源、许可证和借鉴范围。第三方 upstream 目录仅用于审阅归档，不加入 Android source set，也不打包进 APK。

## 目录结构

- `androidApp/`: Android 应用源码、Room 数据层、Compose UI、导入处理、媒体处理、yt-dlp 平台解析和百炼接入。
- `core/`: 自动字幕状态机、字幕解析、生词模型、学习计划、学习架构、练习/游戏/成就引擎、FSRS 调度和单元测试。
- `docs/learning-product-rebuild.md`: 从英语学习必需路径出发的前端/本地服务层重建方案。
- `third_party/upstream/`: TypeWords 与 WordHub 源码快照，仅用于归档和借鉴审阅，不参与 APK 编译。
- `design/huashu/xingyue-2.16.0/`: 高保真 UI/动效原型和图标 SVG。
- `delivery/`: release APK 和交付说明。
- `screenshots/final-2.16.1/`: QA 截图、UI tree、APK 元数据、签名和测试证据。

## 构建与测试

推荐在 Windows PowerShell 中使用 Gradle Wrapper，避免并行 Gradle 任务：

```powershell
.\gradlew.bat :core:test --console=plain
.\gradlew.bat :androidApp:testDebugUnitTest --console=plain
.\gradlew.bat :androidApp:assembleRelease --console=plain
.\gradlew.bat test --console=plain
```

2026-04-26 `2.16.1` 验证结果：

- `:core:test`: 通过。
- `:androidApp:testDebugUnitTest`: 通过。
- `:androidApp:assembleRelease`: 通过。
- 根项目 `test`: 通过。
- 19 个 XML test suite，103 tests / 0 failures / 0 errors / 0 skipped。
- `apksigner verify --verbose --print-certs` 通过，v2 签名有效。
- `aapt dump badging` 通过: `versionCode=13`、`versionName=2.16.1`、minSdk 26、targetSdk 35。
- Secret 扫描通过: 源码/README/交付说明/原型/日志摘要未发现 `sk-...` 百炼 Key。
- App 内 QA 使用 `XingYue_QA_Medium_API_35_2_16_0`，ADB serial `emulator-5554`。
- release APK 安装、启动、首页和学习页通过；学习页四分区、练习舱必做/辅助分组、练习开始页和“下一题前必须判定”提示通过 UI tree 验证。
- 2.16.0 已完成的 SRT/TXT/JSON 直链导入、播放页字幕、点词保存和重启持久化证据保留为回归基线；2.16.1 未改动导入管线。

## 百炼配置与边界

App 不内置、不提交、不记录百炼 Key。真实测试时 Key 只应放在当前 shell 的临时环境变量或 App 内输入框，测试结束后清空并轮换。

- ASR 会上传用户主动导入或通过直链提供的音频内容。
- 翻译、词典补全和 CosyVoice TTS 只在用户主动配置 Key 后调用。
- 真实云端能力受用户账号权限、地域、网络和阿里云计费规则影响。

## 许可证与责任

根目录 `LICENSE` 为 GNU General Public License v3.0。TypeWords、WordHub、youtubedl-android、yt-dlp、FFmpegKit、阿里云百炼、Android SDK、字体、用户导入素材和第三方平台媒体分别受各自许可证、服务条款和内容授权约束。用户需要确认自己有权上传、处理和学习所导入的素材；分发修改版时应按 GPLv3 要求提供相应源码和许可证文本。

## 已知限制

- Bilibili/Douyin 真实公网链接未完成 App 内端到端成功验证；相关解析和错误映射已有单元测试覆盖。
- YouTube 真实入口已证明可进入 yt-dlp 链路并回写失败卡片；测试链接平台侧返回 unavailable。
- 2.16.1 的 App 内补测覆盖了安装、启动、首页、学习页分区、练习舱入口、练习页拦截提示；未用自动化跑完整 5 题小组。
- App 内百炼 Key 配置、ASR、翻译、词典、TTS 和 TTS 缓存本轮未重新截图验收。
- 未覆盖真机、厂商 ROM、弱网、长素材、大文件压力、横屏和性能压测。
