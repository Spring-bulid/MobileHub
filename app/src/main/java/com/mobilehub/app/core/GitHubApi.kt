package com.mobilehub.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------------------
// 数据模型
// ---------------------------------------------------------------------------

data class ApiResult(val status: Int, val body: String, val link: String) {
    val ok: Boolean get() = status in 200..299
}

data class GhUser(
    val login: String,
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val company: String,
    val location: String,
    val blog: String,
    val followers: Int,
    val following: Int,
    val publicRepos: Int,
)

data class GhRepo(
    val owner: String,
    val name: String,
    val fullName: String,
    val description: String,
    val language: String,
    val stars: Int,
    val forks: Int,
    val watchers: Int,
    val openIssues: Int,
    val isPrivate: Boolean,
    val isFork: Boolean,
    val defaultBranch: String,
    val ownerAvatar: String,
    val updatedAt: String,
    val htmlUrl: String,
)

data class GhIssue(
    val number: Int,
    val title: String,
    val state: String,
    val body: String,
    val bodyHtml: String,
    val userLogin: String,
    val userAvatar: String,
    val comments: Int,
    val createdAt: String,
    val isPullRequest: Boolean,
    val merged: Boolean,
    val mergeable: Boolean,
    val labels: List<Pair<String, String>>, // name to hex color
    val repoFullName: String,
    val headRef: String,
    val baseRef: String,
)

data class GhComment(
    val id: Long,
    val userLogin: String,
    val userAvatar: String,
    val body: String,
    val bodyHtml: String,
    val createdAt: String,
)

data class GhNotification(
    val id: String,
    val title: String,
    val type: String,
    val repoFullName: String,
    val reason: String,
    val unread: Boolean,
    val updatedAt: String,
    val subjectUrl: String,
)

data class GhContent(
    val name: String,
    val path: String,
    val type: String, // file | dir
    val size: Long,
)

data class GhCommit(
    val sha: String,
    val message: String,
    val author: String,
    val avatarUrl: String,
    val date: String,
)

data class GhBranch(val name: String)

data class GhWorkflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String,
)

data class GhRun(
    val id: Long,
    val title: String,
    val workflowName: String,
    val runNumber: Int,
    val status: String, // queued | in_progress | completed
    val conclusion: String, // success | failure | cancelled ...
    val branch: String,
    val createdAt: String,
)

// ---------------------------------------------------------------------------
// API 客户端：全部请求经由 Rust 核心执行
// ---------------------------------------------------------------------------

object GitHubApi {

    private const val BASE = "https://api.github.com"

    /** 附带 body_html（GitHub 官方渲染）的媒体类型 */
    private const val FULL_JSON = "application/vnd.github.full+json"

    @Volatile
    var token: String = ""

    @Volatile
    var me: GhUser? = null

    private suspend fun raw(method: String, url: String, body: String = "", accept: String = ""): ApiResult =
        withContext(Dispatchers.IO) {
            val resp = NativeCore.nativeRequest(token, method, url, body, accept)
                ?: return@withContext ApiResult(0, "native layer error", "")
            runCatching {
                val json = JSONObject(resp)
                val headers = json.optJSONObject("headers") ?: JSONObject()
                ApiResult(json.optInt("status"), json.optString("body"), headers.optString("link"))
            }.getOrElse { ApiResult(0, "parse error: ${it.message}", "") }
        }

    private suspend fun get(path: String, accept: String = "") = raw("GET", BASE + path, "", accept)
    private suspend fun post(path: String, body: JSONObject) = raw("POST", BASE + path, body.toString())
    private suspend fun patch(path: String, body: JSONObject) = raw("PATCH", BASE + path, body.toString())
    private suspend fun put(path: String, body: String = "") = raw("PUT", BASE + path, body)
    private suspend fun delete(path: String) = raw("DELETE", BASE + path)

    // ------------------------ 解析 ------------------------

    private fun parseUser(o: JSONObject) = GhUser(
        login = o.optString("login"),
        name = o.optString("name").ifBlank { o.optString("login") },
        avatarUrl = o.optString("avatar_url"),
        bio = o.optString("bio").takeIf { it != "null" } ?: "",
        company = o.optString("company").takeIf { it != "null" } ?: "",
        location = o.optString("location").takeIf { it != "null" } ?: "",
        blog = o.optString("blog").takeIf { it != "null" } ?: "",
        followers = o.optInt("followers"),
        following = o.optInt("following"),
        publicRepos = o.optInt("public_repos"),
    )

    private fun parseRepo(o: JSONObject): GhRepo {
        val ownerObj = o.optJSONObject("owner")
        return GhRepo(
            owner = ownerObj?.optString("login") ?: "",
            name = o.optString("name"),
            fullName = o.optString("full_name"),
            description = o.optString("description").takeIf { it != "null" } ?: "",
            language = o.optString("language").takeIf { it != "null" } ?: "",
            stars = o.optInt("stargazers_count"),
            forks = o.optInt("forks_count"),
            watchers = o.optInt("subscribers_count", o.optInt("watchers_count")),
            openIssues = o.optInt("open_issues_count"),
            isPrivate = o.optBoolean("private"),
            isFork = o.optBoolean("fork"),
            defaultBranch = o.optString("default_branch", "main"),
            ownerAvatar = ownerObj?.optString("avatar_url") ?: "",
            updatedAt = o.optString("updated_at"),
            htmlUrl = o.optString("html_url"),
        )
    }

    private fun parseIssue(o: JSONObject): GhIssue {
        val user = o.optJSONObject("user")
        val labels = mutableListOf<Pair<String, String>>()
        o.optJSONArray("labels")?.let { arr ->
            for (i in 0 until arr.length()) {
                val l = arr.optJSONObject(i) ?: continue
                labels.add(l.optString("name") to l.optString("color", "8250DF"))
            }
        }
        val repoUrl = o.optString("repository_url")
        val repoFull = repoUrl.substringAfter("/repos/", "")
        return GhIssue(
            number = o.optInt("number"),
            title = o.optString("title"),
            state = o.optString("state"),
            body = o.optString("body").takeIf { it != "null" } ?: "",
            bodyHtml = o.optString("body_html").takeIf { it != "null" } ?: "",
            userLogin = user?.optString("login") ?: "",
            userAvatar = user?.optString("avatar_url") ?: "",
            comments = o.optInt("comments"),
            createdAt = o.optString("created_at"),
            isPullRequest = o.has("pull_request") || o.has("merged"),
            merged = o.optBoolean("merged") || o.optJSONObject("pull_request")
                ?.optString("merged_at")?.takeIf { it != "null" && it.isNotBlank() } != null,
            mergeable = o.optBoolean("mergeable"),
            labels = labels,
            repoFullName = repoFull,
            headRef = o.optJSONObject("head")?.optString("ref") ?: "",
            baseRef = o.optJSONObject("base")?.optString("ref") ?: "",
        )
    }

    private fun parseComment(o: JSONObject): GhComment {
        val user = o.optJSONObject("user")
        return GhComment(
            id = o.optLong("id"),
            userLogin = user?.optString("login") ?: "",
            userAvatar = user?.optString("avatar_url") ?: "",
            body = o.optString("body"),
            bodyHtml = o.optString("body_html").takeIf { it != "null" } ?: "",
            createdAt = o.optString("created_at"),
        )
    }

    private fun repoArray(result: ApiResult): List<GhRepo> {
        if (!result.ok) return emptyList()
        return runCatching {
            val arr = JSONArray(result.body)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map(::parseRepo)
        }.getOrDefault(emptyList())
    }

    private fun issueArray(result: ApiResult): List<GhIssue> {
        if (!result.ok) return emptyList()
        return runCatching {
            val arr = JSONArray(result.body)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map(::parseIssue)
        }.getOrDefault(emptyList())
    }

    // ------------------------ 账户 ------------------------

    suspend fun fetchMe(): GhUser? {
        val r = get("/user")
        if (!r.ok) return null
        return runCatching { parseUser(JSONObject(r.body)) }.getOrNull()?.also { me = it }
    }

    suspend fun fetchUser(login: String): GhUser? {
        val r = get("/users/$login")
        if (!r.ok) return null
        return runCatching { parseUser(JSONObject(r.body)) }.getOrNull()
    }

    // ------------------------ 仓库 ------------------------

    suspend fun myRepos(page: Int = 1): List<GhRepo> =
        repoArray(get("/user/repos?sort=updated&per_page=30&page=$page"))

    suspend fun userRepos(login: String, page: Int = 1): List<GhRepo> =
        repoArray(get("/users/$login/repos?sort=updated&per_page=30&page=$page"))

    suspend fun starredRepos(page: Int = 1): List<GhRepo> =
        repoArray(get("/user/starred?per_page=30&page=$page"))

    suspend fun repo(owner: String, name: String): GhRepo? {
        val r = get("/repos/$owner/$name")
        if (!r.ok) return null
        return runCatching { parseRepo(JSONObject(r.body)) }.getOrNull()
    }

    suspend fun readme(owner: String, name: String): String {
        val r = get("/repos/$owner/$name/readme", accept = "application/vnd.github.raw+json")
        return if (r.ok) r.body else ""
    }

    /** GitHub 官方渲染的 README HTML，与网页版一致 */
    suspend fun readmeHtml(owner: String, name: String): String {
        val r = get("/repos/$owner/$name/readme", accept = "application/vnd.github.html+json")
        return if (r.ok) r.body else ""
    }

    suspend fun contents(owner: String, name: String, path: String, ref: String): List<GhContent> {
        val refQ = if (ref.isBlank()) "" else "?ref=$ref"
        val pathPart = if (path.isBlank()) "" else "/$path"
        val r = get("/repos/$owner/$name/contents$pathPart$refQ")
        if (!r.ok) return emptyList()
        return runCatching {
            val arr = JSONArray(r.body)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map {
                GhContent(it.optString("name"), it.optString("path"), it.optString("type"), it.optLong("size"))
            }.sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() }))
        }.getOrDefault(emptyList())
    }

    suspend fun fileRaw(owner: String, name: String, path: String, ref: String): String {
        val refQ = if (ref.isBlank()) "" else "?ref=$ref"
        val pathPart = if (path.isBlank()) "" else "/$path"
        val r = get("/repos/$owner/$name/contents$pathPart$refQ", accept = "application/vnd.github.raw+json")
        return if (r.ok) r.body else "加载失败 (HTTP ${r.status})"
    }

    // ------------------------ Actions 工作流（YML 一键编译） ------------------------

    /** 列出仓库的 GitHub Actions 工作流（.github/workflows 目录下的 yml） */
    suspend fun workflows(owner: String, name: String): List<GhWorkflow> {
        val r = get("/repos/$owner/$name/actions/workflows?per_page=50")
        if (!r.ok) return emptyList()
        return runCatching {
            val arr = JSONObject(r.body).optJSONArray("workflows") ?: JSONArray()
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map {
                GhWorkflow(
                    it.optLong("id"),
                    it.optString("name"),
                    it.optString("path"),
                    it.optString("state"),
                )
            }.filter { it.path.endsWith(".yml") || it.path.endsWith(".yaml") }
        }.getOrDefault(emptyList())
    }

    /**
     * 触发 workflow_dispatch 一键编译。
     * 返回 null 表示成功，否则返回错误描述（422 = 该 yml 未声明 workflow_dispatch 触发器）。
     */
    suspend fun dispatchWorkflow(owner: String, name: String, workflowId: Long, ref: String): String? {
        val r = post(
            "/repos/$owner/$name/actions/workflows/$workflowId/dispatches",
            JSONObject().put("ref", ref),
        )
        if (r.ok) return null
        return when (r.status) {
            403 -> "没有权限（token 需 workflow 权限）"
            404 -> "工作流不存在或无权访问"
            422 -> "该工作流未声明手动触发（workflow_dispatch）"
            else -> "触发失败 (HTTP ${r.status})"
        }
    }

    /**
     * 非本人仓库的一键编译：先复刻到自己账号，再在复刻仓库里触发同名工作流。
     * 返回 null 表示成功，否则返回错误描述；过程状态通过 onProgress 回报。
     */
    suspend fun dispatchOnFork(
        owner: String,
        name: String,
        wf: GhWorkflow,
        onProgress: (String) -> Unit,
    ): String? {
        val myLogin = me?.login ?: return "未登录"
        var fork = repo(myLogin, name)
        if (fork?.isFork == false) return "你名下已有同名仓库 $myLogin/$name，无法复刻"
        if (fork == null) {
            onProgress("正在复刻到 $myLogin/$name ...")
            val r = forkRepo(owner, name)
            if (!r.ok) return "复刻失败 (HTTP ${r.status})"
            // fork 是异步的，轮询等待仓库就绪
            repeat(10) {
                delay(1500)
                if (fork == null) fork = repo(myLogin, name)
            }
            if (fork == null) return "复刻超时，请稍后到自己的仓库里手动触发"
        }
        val mine = fork!!
        // fork 仓库默认禁用 Actions，先启用
        onProgress("正在启用复刻仓库的 Actions ...")
        val enableR = raw(
            "PUT",
            "$BASE/repos/$myLogin/${mine.name}/actions/permissions",
            JSONObject().put("enabled", true).put("allowed_actions", "all").toString(),
        )
        if (!enableR.ok) return "启用 Actions 失败 (HTTP ${enableR.status})，请确认 token 是否有 workflow 权限"
        // 按 path 在复刻仓库里找同一个工作流（id 与原仓库不同，注册有延迟）
        var target: GhWorkflow? = null
        repeat(6) {
            if (target == null) {
                target = workflows(myLogin, mine.name).firstOrNull { it.path == wf.path }
                if (target == null) delay(2000)
            }
        }
        val t = target ?: return "复刻仓库里暂未找到该工作流，稍后再试"
        onProgress("正在触发 $myLogin/${mine.name} 的 ${t.name} ...")
        return dispatchWorkflow(myLogin, mine.name, t.id, mine.defaultBranch)
    }

    /** 列出仓库最近的工作流运行记录，用于编译进度页 */
    suspend fun workflowRuns(owner: String, name: String): List<GhRun> {
        val r = get("/repos/$owner/$name/actions/runs?per_page=30")
        if (!r.ok) return emptyList()
        return runCatching {
            val arr = JSONObject(r.body).optJSONArray("workflow_runs") ?: JSONArray()
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { o ->
                GhRun(
                    id = o.optLong("id"),
                    title = o.optString("display_title").ifBlank { o.optString("name") },
                    workflowName = o.optString("name"),
                    runNumber = o.optInt("run_number"),
                    status = o.optString("status"),
                    conclusion = o.optString("conclusion"),
                    branch = o.optString("head_branch"),
                    createdAt = o.optString("created_at"),
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun commits(owner: String, name: String, page: Int = 1): List<GhCommit> {
        val r = get("/repos/$owner/$name/commits?per_page=30&page=$page")
        if (!r.ok) return emptyList()
        return runCatching {
            val arr = JSONArray(r.body)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { c ->
                val commit = c.optJSONObject("commit")
                GhCommit(
                    sha = c.optString("sha").take(7),
                    message = commit?.optString("message")?.lineSequence()?.firstOrNull() ?: "",
                    author = commit?.optJSONObject("author")?.optString("name") ?: "",
                    avatarUrl = c.optJSONObject("author")?.optString("avatar_url") ?: "",
                    date = commit?.optJSONObject("author")?.optString("date") ?: "",
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun branches(owner: String, name: String): List<GhBranch> {
        val r = get("/repos/$owner/$name/branches?per_page=100")
        if (!r.ok) return emptyList()
        return runCatching {
            val arr = JSONArray(r.body)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { GhBranch(it.optString("name")) }
        }.getOrDefault(emptyList())
    }

    suspend fun createRepo(name: String, description: String, isPrivate: Boolean): ApiResult =
        post("/user/repos", JSONObject().put("name", name).put("description", description).put("private", isPrivate))

    suspend fun deleteRepo(owner: String, name: String): ApiResult = delete("/repos/$owner/$name")

    suspend fun forkRepo(owner: String, name: String): ApiResult = post("/repos/$owner/$name/forks", JSONObject())

    // ------------------------ star / watch / follow ------------------------

    suspend fun isStarred(owner: String, name: String): Boolean = get("/user/starred/$owner/$name").status == 204

    suspend fun setStar(owner: String, name: String, star: Boolean): Boolean {
        val r = if (star) put("/user/starred/$owner/$name") else delete("/user/starred/$owner/$name")
        return r.status == 204
    }

    suspend fun isWatching(owner: String, name: String): Boolean = get("/repos/$owner/$name/subscription").ok

    suspend fun setWatch(owner: String, name: String, watch: Boolean): Boolean {
        return if (watch) {
            raw("PUT", "$BASE/repos/$owner/$name/subscription", JSONObject().put("subscribed", true).toString()).ok
        } else {
            delete("/repos/$owner/$name/subscription").status == 204
        }
    }

    suspend fun isFollowing(login: String): Boolean = get("/user/following/$login").status == 204

    suspend fun setFollow(login: String, follow: Boolean): Boolean {
        val r = if (follow) put("/user/following/$login") else delete("/user/following/$login")
        return r.status == 204
    }

    // ------------------------ issue / PR ------------------------

    suspend fun myIssues(page: Int = 1): List<GhIssue> =
        issueArray(get("/issues?filter=all&state=open&per_page=30&page=$page"))

    suspend fun repoIssues(owner: String, name: String, state: String, page: Int = 1): List<GhIssue> =
        issueArray(get("/repos/$owner/$name/issues?state=$state&per_page=30&page=$page"))
            .filter { !it.isPullRequest }

    suspend fun repoPulls(owner: String, name: String, state: String, page: Int = 1): List<GhIssue> =
        issueArray(get("/repos/$owner/$name/pulls?state=$state&per_page=30&page=$page"))
            .map { it.copy(isPullRequest = true) }

    suspend fun issue(owner: String, name: String, number: Int): GhIssue? {
        val r = get("/repos/$owner/$name/issues/$number", accept = FULL_JSON)
        if (!r.ok) return null
        return runCatching { parseIssue(JSONObject(r.body)) }.getOrNull()
    }

    suspend fun pull(owner: String, name: String, number: Int): GhIssue? {
        val r = get("/repos/$owner/$name/pulls/$number", accept = FULL_JSON)
        if (!r.ok) return null
        return runCatching { parseIssue(JSONObject(r.body)).copy(isPullRequest = true) }.getOrNull()
    }

    suspend fun comments(owner: String, name: String, number: Int): List<GhComment> {
        val r = get("/repos/$owner/$name/issues/$number/comments?per_page=100", accept = FULL_JSON)
        if (!r.ok) return emptyList()
        return runCatching {
            val arr = JSONArray(r.body)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map(::parseComment)
        }.getOrDefault(emptyList())
    }

    suspend fun addComment(owner: String, name: String, number: Int, body: String): ApiResult =
        post("/repos/$owner/$name/issues/$number/comments", JSONObject().put("body", body))

    suspend fun createIssue(owner: String, name: String, title: String, body: String): ApiResult =
        post("/repos/$owner/$name/issues", JSONObject().put("title", title).put("body", body))

    suspend fun setIssueState(owner: String, name: String, number: Int, state: String): ApiResult =
        patch("/repos/$owner/$name/issues/$number", JSONObject().put("state", state))

    suspend fun mergePull(owner: String, name: String, number: Int): ApiResult =
        put("/repos/$owner/$name/pulls/$number/merge", JSONObject().put("merge_method", "merge").toString())

    // ------------------------ 通知 ------------------------

    suspend fun notifications(all: Boolean): List<GhNotification> {
        val r = get("/notifications?all=$all&per_page=50")
        if (!r.ok) return emptyList()
        return runCatching {
            val arr = JSONArray(r.body)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { n ->
                val subject = n.optJSONObject("subject")
                GhNotification(
                    id = n.optString("id"),
                    title = subject?.optString("title") ?: "",
                    type = subject?.optString("type") ?: "",
                    repoFullName = n.optJSONObject("repository")?.optString("full_name") ?: "",
                    reason = n.optString("reason"),
                    unread = n.optBoolean("unread"),
                    updatedAt = n.optString("updated_at"),
                    subjectUrl = subject?.optString("url") ?: "",
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun markThreadRead(id: String): Boolean = raw("PATCH", "$BASE/notifications/threads/$id", "").ok

    suspend fun markAllRead(): Boolean = put("/notifications", JSONObject().toString()).status in listOf(202, 205)

    // ------------------------ 搜索 ------------------------

    suspend fun searchRepos(query: String, page: Int = 1): List<GhRepo> {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val r = get("/search/repositories?q=$q&per_page=30&page=$page")
        if (!r.ok) return emptyList()
        return runCatching {
            val items = JSONObject(r.body).optJSONArray("items") ?: JSONArray()
            (0 until items.length()).mapNotNull { items.optJSONObject(it) }.map(::parseRepo)
        }.getOrDefault(emptyList())
    }

    suspend fun searchUsers(query: String, page: Int = 1): List<GhUser> {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val r = get("/search/users?q=$q&per_page=30&page=$page")
        if (!r.ok) return emptyList()
        return runCatching {
            val items = JSONObject(r.body).optJSONArray("items") ?: JSONArray()
            (0 until items.length()).mapNotNull { items.optJSONObject(it) }.map(::parseUser)
        }.getOrDefault(emptyList())
    }

    suspend fun trending(): List<GhRepo> = searchRepos("stars:>10000 sort:stars")
}
