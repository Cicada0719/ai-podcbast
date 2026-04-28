# 星月英语发布基线

这份文档用于区分“可安装测试 APK”和“可上架发布 APK”。`debug` 包和 debug 签名的 `release` 包都只能用于测试，不能直接上传应用商店。

## 当前发布坐标

- 包名：`com.xingyue.english`
- 当前版本：`2.17.0`
- 当前 `versionCode`：`14`
- `minSdk`：26
- `targetSdk`：35

## 正式签名配置

正式上架前需要准备自己的 release keystore，并把配置写入根目录 `local.properties`。不要把 keystore、密码或 `local.properties` 提交到仓库。

示例键名：

```properties
xingyue.release.storeFile=E:\\secure\\xingyue-release.jks
xingyue.release.storePassword=your-store-password
xingyue.release.keyAlias=xingyue
xingyue.release.keyPassword=your-key-password
```

创建 keystore 的示例命令：

```powershell
keytool -genkeypair -v -keystore E:\secure\xingyue-release.jks -alias xingyue -keyalg RSA -keysize 4096 -validity 10000
```

## 发布检查

构建后运行：

```powershell
.\tools\android_release_readiness.ps1
```

脚本会检查：

- release APK 是否存在。
- 包名、版本号和 SDK 配置。
- release 合并 Manifest 中是否关闭 `allowBackup`。
- release Manifest 是否没有 `debuggable=true`。
- 最终权限是否已在隐私政策中披露。
- APK 是否仍使用 `Android Debug` 签名。
- 是否存在正式 release signing 配置。

只有报告为 `GREEN` 时，才可以把 release APK 视作上架候选包。`RED` 表示阻塞；`YELLOW` 表示可以测试但发布前需要人工确认。
