# 星月英语 Android

星月英语是面向 Android 手机端的英语听读学习 App。当前仓库只保留源码、构建脚本、开发测试工具和运行所需资产；APK、QA 截图、构建目录、本地签名配置和第三方 upstream 快照不入库。

## 当前版本

- 版本: `2.17.0`
- versionCode: `14`
- 包名: `com.xingyue.english`
- 启动 Activity: `com.xingyue.english.android.XingYueEnglishActivity`

## 当前能力

- 本地导入: 支持 SRT/VTT/ASS/SSA、TXT、JSON、APKG、PDF、音频、视频和部分文档素材。
- 直链导入: 支持 SRT/TXT/JSON、10 MB 内音频和常见视频地址；`text/html` 网页不会误判成文档。
- 平台链接导入: YouTube/Bilibili/Douyin 公开链接进入 Android 端内置 `yt-dlp` 链路。只处理公开、用户有权访问、无登录、无付费、无 DRM 内容。
- 播放页: 双语时间线、AB 循环、单句重播、速度切换、点词查词、手动收词和 TTS 入口。
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

Wordle、成就、看描述猜词只作为低压力巩固，不阻塞今日主线。

## 目录结构

- `androidApp/`: Android 应用源码、Room 数据层、Compose UI、导入处理、媒体处理、yt-dlp 平台解析和百炼接入。
- `core/`: 自动字幕状态机、字幕解析、生词模型、学习计划、学习架构、练习/游戏/成就引擎、FSRS 调度和单元测试。
- `tools/`: Android 全流程 QA、红绿灯报告和 release readiness 检查脚本。
- `docs/`: 发布基线和维护说明。
- `gradle/`、`gradlew`、`gradlew.bat`: Gradle Wrapper 和构建入口。

## 构建与测试

推荐在 Windows PowerShell 中使用 Gradle Wrapper，避免并行 Gradle 任务：

```powershell
.\gradlew.bat test --console=plain
.\gradlew.bat :androidApp:assembleDebug --console=plain
.\gradlew.bat :androidApp:assembleRelease --console=plain
```

上架前检查：

```powershell
.\tools\android_release_readiness.ps1
```

`release` 在没有正式 `local.properties` 签名配置时会回退到 debug 签名，只能用于本地测试，不能直接上架。正式签名配置见 `docs/release-readiness.md`。

## 第三方来源

| 项目 | Commit | 许可证 | 借鉴范围 |
| --- | --- | --- | --- |
| TypeWords | `0de1e75d41f7438b738a5e83700b49e3d1e3c858` | GPL-3.0 | 打字背词、听写、自测、逐句默写、错词/熟词/收藏、FSRS 学习闭环 |
| WordHub | `fa0c50c4acd28e857dba5d985edf8c052369dadd` | MIT | 词库管理、查询历史、学习/复习、成就、统计、Wordle、看描述猜词 |
| ChattyPlay-Agent | upstream GitHub | 以原仓库为准 | 只参考“解析中/缩略图/平台标签/失败原因”的交互表达，不嵌入 Web/Hono 服务或第三方代理 |

用户/项目方声明已获得 TypeWords 和 WordHub 合作借鉴同意；正式授权记录由项目方留存。本仓库只记录来源、许可证和借鉴范围。第三方 upstream 源码快照不入库，不加入 Android source set，也不打包进 APK。

## 百炼配置与边界

App 不内置、不提交、不记录百炼 Key。真实测试时 Key 只应放在当前 shell 的临时环境变量或 App 内输入框，测试结束后清空并轮换。

- ASR 会上传用户主动导入或通过直链提供的音频内容。
- 翻译、词典补全和 CosyVoice TTS 只在用户主动配置 Key 后调用。
- 真实云端能力受用户账号权限、地域、网络和阿里云计费规则影响。

## 许可证与责任

根目录 `LICENSE` 为 GNU General Public License v3.0。TypeWords、WordHub、youtubedl-android、yt-dlp、FFmpegKit、阿里云百炼、Android SDK、字体、用户导入素材和第三方平台媒体分别受各自许可证、服务条款和内容授权约束。用户需要确认自己有权上传、处理和学习所导入的素材；分发修改版时应按 GPLv3 要求提供相应源码和许可证文本。
