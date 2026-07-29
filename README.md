# MobileHub

用一枚 GitHub Token 掌控整个 GitHub 的 Android 客户端。

UI 采用 [miuix](https://github.com/compose-miuix-ui/miuix)（HyperOS 设计风格的 Compose 组件库），信息架构仿 GitHub Mobile；所有网络请求由 Rust 核心执行，Token 由 C 语言保险库加密后存储在本机。

## 功能

- 登录：粘贴 Personal Access Token 即可，内置一键跳转 GitHub 创建页（已预填权限）
- 主页：我的仓库、分配给我的议题
- 通知：未读 / 全部、单条已读并跳转、全部标为已读
- 探索：热门仓库、搜索仓库与开发者
- 仓库：README 渲染、议题 / PR 列表、提交历史、代码目录浏览、文件查看、star / watch / fork、新建议题
- 议题 / PR：状态徽章、正文、评论流、发表评论、关闭 / 重开、合并 PR
- 用户：资料页、关注 / 取消关注、仓库列表
- 其他：新建仓库、已加星标列表、深色模式跟随系统

## 架构

```
┌─────────────────────────────────────────────┐
│  UI 层  Kotlin Jetpack Compose + miuix      │
│         页面、导航、Markdown 轻渲染          │
├─────────────────────────────────────────────┤
│  桥接   NativeCore (JNI)                    │
├─────────────────────────────────────────────┤
│  核心   Rust  libmobilehub_core.so          │
│         ureq + rustls 执行全部 REST 请求     │
│  ┌───────────────────────────────────────┐  │
│  │  C  vault.c  XTEA-CBC Token 保险库    │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

| 层 | 技术 | 说明 |
|---|---|---|
| UI | Kotlin 2.4 + Compose + miuix 0.9.3 | HyperOS 风格全部页面，自绘 Octicons 图标 |
| 网络 | Rust（jni 0.21 / ureq / serde_json） | GitHub REST API v3，统一超时与错误处理 |
| 存储 | C（XTEA-CBC，编译进 .so） | Token 本地加密持久化，密钥由设备种子派生 |
| 头像 | Coil 3 | 图片独立加载，不经文本通道 |

JNI 接口只有三个：

```kotlin
external fun nativeRequest(token, method, url, body, accept): String?  // 返回 {"status","body","headers"}
external fun nativeSealToken(seed, token): ByteArray?
external fun nativeOpenToken(seed, sealed): String?
```

## 构建

环境要求：

- JDK 17
- Android SDK（compileSdk 37）+ NDK r27
- Rust（含 `aarch64-linux-android` target）与 [cargo-ndk](https://github.com/bbqsrc/cargo-ndk)

步骤：

```bash
# 1. 编译 Rust 核心（产物自动放入 app/src/main/jniLibs/arm64-v8a/）
cd rustcore
cargo ndk -t arm64-v8a -o ../app/src/main/jniLibs build --release

# 2. 编译 APK
cd ..
gradle :app:assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`（约 9 MB，arm64-v8a）。

仓库已附带编译好的 `libmobilehub_core.so`，只改 Kotlin 代码时可跳过第 1 步。

`local.properties` 需自行创建并指向本机 SDK：

```properties
sdk.dir=/path/to/android-sdk
```

## 使用

1. 安装 APK 并打开
2. 点击"没有 Token？去 GitHub 创建"，在打开的页面中直接点 Generate token（权限已预勾）
3. 复制 Token，回到应用点击"粘贴"，登录

Token 权限建议：`repo`、`notifications`、`user`；如需删除仓库、操作 Actions，再勾 `delete_repo`、`workflow`。

## 安全说明

- Token 仅保存在本机：经 C 保险库 XTEA-CBC 加密后写入应用私有目录
- 不经过任何第三方服务器，所有请求直连 `api.github.com`
- 应用不申请存储、定位等无关权限，仅使用网络权限

## 目录结构

```
app/
  src/main/java/com/mobilehub/app/
    core/        NativeCore(JNI) / TokenStore / GitHubApi
    ui/          Octicons 图标、通用组件
    ui/screens/  登录、主界面、仓库、议题、用户、代码浏览
  src/main/jniLibs/arm64-v8a/   Rust 编译产物
rustcore/
  src/lib.rs     JNI 导出 + ureq 网络核心
  src/vault.c    XTEA-CBC Token 保险库
  build.rs       cc 编译 C 代码
```

## License

MIT
