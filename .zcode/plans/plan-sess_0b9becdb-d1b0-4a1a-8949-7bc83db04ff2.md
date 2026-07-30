# BUG修复计划

## 问题诊断

### 1. YML无效问题

**根因 A**: `RepoDetailScreen.kt` 中的 `WorkflowCard` 展示了仓库中**所有** `.yml/.yaml` 工作流，包括那些没有 `workflow_dispatch` 触发器的（如仅含 `push`/`pull_request` 的工作流）。用户点击"编译"后才收到 422 错误，但此时已经造成了困惑。

**修复**: 在 `GitHubApi.workflows()` 增加检查——通过 GitHub Contents API 获取每个工作流文件的原始内容，检查是否包含 `workflow_dispatch:` 关键字，只返回可手动触发的工作流。为避免 N+1 API 调用，改为批量获取 `.github/workflows` 目录内容后过滤。

**根因 B**: `dispatchOnFork()` 中启用复刻仓库 Actions 的 API 调用结果未被检查，失败时静默继续。

**修复**: 检查 `raw()` 返回值，如果启用 Actions 失败则返回明确的错误提示。

### 2. 多处操作缺少提示反馈

| 位置 | 操作 | 当前行为 |
|------|------|----------|
| `RepoDetailScreen` - Fork | 调用 `forkRepo` | 无任何反馈 |
| `RepoDetailScreen` - Star | 静默切换 | 无成功/失败提示 |
| `RepoDetailScreen` - Watch | 静默切换 | 无成功/失败提示 |
| `IssueDetailScreen` - 评论 | 成功清空，失败静默 | 无失败提示 |
| `IssueDetailScreen` - 关闭/重开 | 静默刷新 | 无成功/失败提示 |
| `IssueDetailScreen` - 合并PR | 静默刷新 | 无成功/失败提示 |
| `MainScreen` - 全部标已读 | 调用后静默 | 无成功/失败提示 |
| `UserProfileScreen` - 关注/取消 | 静默切换 | 无成功/失败提示 |
| `LoginScreen` - 粘贴按钮 | 剪贴板为空时静默 | 无提示 |
| `NewIssueScreen` - 创建 | 失败仅显示HTTP码 | 错误信息不友好 |

**修复方案**: 使用 Android `Toast` 提供轻量反馈（统一、简洁、不改变UI结构）。对于已有 `Text` 错误区域的表单操作，增强错误信息的友好度。

### 3. build-apk.yml 验证

`build-apk.yml` 文件本身语法正确。`workflow_dispatch:` （无输入参数）是 GitHub Actions 合法语法。`gradle` 命令依赖 `gradle/actions/setup-gradle@v4` action 提供，配置正确。

## 修改文件清单

### `GitHubApi.kt` — 3处修改
1. `workflows()`: 增加工作流内容检查，只返回含 `workflow_dispatch` 的工作流
2. `dispatchOnFork()`: 检查启用 Actions 的 API 结果
3. `forkRepo()`: 无需修改（调用者处理反馈）

### `RepoDetailScreen.kt` — 4处修改
1. `WorkflowCard`: 优化提示文案，只展示可触发的工作流
2. `onFork`: 添加 Toast 反馈 fork 成功/失败
3. `onStar`: 添加 Toast 反馈
4. `onWatch`: 添加 Toast 反馈

### `IssueDetailScreen.kt` — 4处修改
1. 评论发布: 失败时显示错误提示
2. 关闭/重开: 添加 Toast 反馈结果
3. 合并PR: 添加 Toast 反馈结果
4. 新建议题/仓库: 增强错误信息

### `MainScreen.kt` — 2处修改
1. 全部标已读: 添加 Toast 反馈
2. 单条通知已读: 点击后刷新列表

### `UserProfileScreen.kt` — 1处修改
1. 关注/取消关注: 添加 Toast 反馈

### `LoginScreen.kt` — 1处修改
1. 粘贴按钮: 剪贴板为空时显示提示文字

### `Components.kt` — 1处新增
1. 新增 `toast(context, msg)` 工具函数，统一管理 Toast 调用

## 不修改的部分
- Rust 核心 (`rustcore/`) — 网络层本身工作正常
- `build-apk.yml` — 语法正确无需修改
- `NativeCore.kt` / `TokenStore.kt` / `AppPrefs.kt` — 无相关bug
- `CodeScreens.kt` / `ActionsScreen.kt` — 功能正常
- `LiquidNavBar.kt` / `GhMarkdown.kt` / `Octicons.kt` — 无相关bug