package com.mobilehub.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Octicons 风格图标（GitHub 官方 16x16 路径的手工重绘子集）。
 * 全部为单色 ImageVector，配合 Icon(tint=...) 使用。
 */
object Octicons {

    private val cache = HashMap<String, ImageVector>()

    private fun icon(name: String, d: String): ImageVector = cache.getOrPut(name) {
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black),
        ).build()
    }

    val Home: ImageVector
        get() = icon(
            "home",
            "M8 1.5 L14.5 7 L13.5 8.2 L12.5 7.4 V13.5 A1 1 0 0 1 11.5 14.5 H9.5 V10.5 H6.5 V14.5 H4.5 A1 1 0 0 1 3.5 13.5 V7.4 L2.5 8.2 L1.5 7 Z"
        )

    val Bell: ImageVector
        get() = icon(
            "bell",
            "M8 1 A4.5 4.5 0 0 0 3.5 5.5 V8.6 L2.2 11 A0.8 0.8 0 0 0 2.9 12.2 H13.1 A0.8 0.8 0 0 0 13.8 11 L12.5 8.6 V5.5 A4.5 4.5 0 0 0 8 1 Z M6.5 13.3 A1.6 1.6 0 0 0 9.5 13.3 Z"
        )

    val Search: ImageVector
        get() = icon(
            "search",
            "M10.68 11.74 a6 6 0 1 1 1.06 -1.06 l3.04 3.04 a0.75 0.75 0 1 1 -1.06 1.06 Z M11.5 7 a4.5 4.5 0 1 0 -9 0 a4.5 4.5 0 0 0 9 0 Z"
        )

    val Person: ImageVector
        get() = icon(
            "person",
            "M8 2 A3 3 0 1 1 8 8 A3 3 0 0 1 8 2 Z M2.5 13.5 A5.5 4.5 0 0 1 13.5 13.5 V14 H2.5 Z"
        )

    val Repo: ImageVector
        get() = icon(
            "repo",
            "M3.5 1 H12 A1 1 0 0 1 13 2 V14 L10 12.5 L7 14 V11 H4.5 A0.5 0.5 0 0 1 4.5 10 H7 V2.5 H4.5 A0.75 0.75 0 0 0 4.5 4 H5.5 V5 H4.5 A1.75 1.75 0 0 1 4.5 1.5 Z M3 2.75 A1.5 1.5 0 0 1 4.5 1.25 V10 A2 2 0 0 0 3 11 Z"
        )

    val Star: ImageVector
        get() = icon(
            "star",
            "M8 1.75 L9.9 5.6 L14.2 6.2 L11.1 9.2 L11.8 13.4 L8 11.4 L4.2 13.4 L4.9 9.2 L1.8 6.2 L6.1 5.6 Z M8 3.6 L6.7 6.3 L3.7 6.7 L5.9 8.8 L5.4 11.7 L8 10.3 L10.6 11.7 L10.1 8.8 L12.3 6.7 L9.3 6.3 Z"
        )

    val StarFill: ImageVector
        get() = icon(
            "star-fill",
            "M8 1.75 L9.9 5.6 L14.2 6.2 L11.1 9.2 L11.8 13.4 L8 11.4 L4.2 13.4 L4.9 9.2 L1.8 6.2 L6.1 5.6 Z"
        )

    val Fork: ImageVector
        get() = icon(
            "fork",
            "M4 2 A1.75 1.75 0 1 1 4.75 5.35 V6 A1 1 0 0 0 5.75 7 H10.25 A1 1 0 0 0 11.25 6 V5.35 A1.75 1.75 0 1 1 12.75 5.35 V6 A2.5 2.5 0 0 1 10.25 8.5 H8.75 V10.65 A1.75 1.75 0 1 1 7.25 10.65 V8.5 H5.75 A2.5 2.5 0 0 1 3.25 6 V5.35 A1.75 1.75 0 0 1 4 2 Z"
        )

    val IssueOpen: ImageVector
        get() = icon(
            "issue-open",
            "M8 1 A7 7 0 1 1 8 15 A7 7 0 0 1 8 1 Z M8 2.5 A5.5 5.5 0 1 0 8 13.5 A5.5 5.5 0 0 0 8 2.5 Z M8 6.5 A1.5 1.5 0 1 1 8 9.5 A1.5 1.5 0 0 1 8 6.5 Z"
        )

    val Check: ImageVector
        get() = icon(
            "check",
            "M13.78 4.22 a0.75 0.75 0 0 1 0 1.06 l-7.25 7.25 a0.75 0.75 0 0 1 -1.06 0 L2.22 9.28 a0.75 0.75 0 1 1 1.06 -1.06 L6 10.94 l6.72 -6.72 a0.75 0.75 0 0 1 1.06 0 Z"
        )

    val PullRequest: ImageVector
        get() = icon(
            "pr",
            "M3.75 2 A1.75 1.75 0 1 1 4.5 5.35 V10.65 A1.75 1.75 0 1 1 3 10.65 V5.35 A1.75 1.75 0 0 1 3.75 2 Z M9.5 3.25 H10.75 A2.5 2.5 0 0 1 13.25 5.75 V10.65 A1.75 1.75 0 1 1 11.75 10.65 V5.75 A1 1 0 0 0 10.75 4.75 H9.5 V6.5 L6.5 4 L9.5 1.5 Z"
        )

    val Merge: ImageVector
        get() = icon(
            "merge",
            "M4.75 2 A1.75 1.75 0 1 1 5.5 5.3 V6 A5 5 0 0 0 10.6 8.6 A1.75 1.75 0 1 1 10.7 10.1 A6.4 6.4 0 0 1 5.5 8 V10.7 A1.75 1.75 0 1 1 4 10.7 V5.3 A1.75 1.75 0 0 1 4.75 2 Z"
        )

    val Code: ImageVector
        get() = icon(
            "code",
            "M4.7 4.3 L1 8 L4.7 11.7 L5.8 10.6 L3.2 8 L5.8 5.4 Z M11.3 4.3 L10.2 5.4 L12.8 8 L10.2 10.6 L11.3 11.7 L15 8 Z"
        )

    val File: ImageVector
        get() = icon(
            "file",
            "M3.5 1 H9 L13 5 V14 A1 1 0 0 1 12 15 H3.5 A1 1 0 0 1 2.5 14 V2 A1 1 0 0 1 3.5 1 Z M9 2.5 V5.5 H12 L9 2.5 Z M4 2.5 V13.5 H11.5 V7 H8 A0.5 0.5 0 0 1 7.5 6.5 V2.5 Z"
        )

    val Folder: ImageVector
        get() = icon(
            "folder",
            "M1.75 2.5 H6 L7.5 4 H14.25 A0.75 0.75 0 0 1 15 4.75 V12.75 A0.75 0.75 0 0 1 14.25 13.5 H1.75 A0.75 0.75 0 0 1 1 12.75 V3.25 A0.75 0.75 0 0 1 1.75 2.5 Z"
        )

    val Comment: ImageVector
        get() = icon(
            "comment",
            "M2 2.75 A0.75 0.75 0 0 1 2.75 2 H13.25 A0.75 0.75 0 0 1 14 2.75 V10.25 A0.75 0.75 0 0 1 13.25 11 H8.06 L5.28 13.78 A0.75 0.75 0 0 1 4 13.25 V11 H2.75 A0.75 0.75 0 0 1 2 10.25 Z M3.5 3.5 V9.5 H4.75 A0.75 0.75 0 0 1 5.5 10.25 V11.7 L7.7 9.5 H12.5 V3.5 Z"
        )

    val Eye: ImageVector
        get() = icon(
            "eye",
            "M8 3 C11.5 3 14.2 5.4 15.2 8 C14.2 10.6 11.5 13 8 13 C4.5 13 1.8 10.6 0.8 8 C1.8 5.4 4.5 3 8 3 Z M8 4.5 C5.4 4.5 3.3 6.1 2.4 8 C3.3 9.9 5.4 11.5 8 11.5 C10.6 11.5 12.7 9.9 13.6 8 C12.7 6.1 10.6 4.5 8 4.5 Z M8 5.8 A2.2 2.2 0 1 1 8 10.2 A2.2 2.2 0 0 1 8 5.8 Z"
        )

    val Plus: ImageVector
        get() = icon(
            "plus",
            "M7.25 2 H8.75 V7.25 H14 V8.75 H8.75 V14 H7.25 V8.75 H2 V7.25 H7.25 Z"
        )

    val Back: ImageVector
        get() = icon(
            "back",
            "M9.78 3.22 a0.75 0.75 0 0 1 0 1.06 L6.06 8 l3.72 3.72 a0.75 0.75 0 1 1 -1.06 1.06 l-4.25 -4.25 a0.75 0.75 0 0 1 0 -1.06 l4.25 -4.25 a0.75 0.75 0 0 1 1.06 0 Z"
        )

    val ChevronRight: ImageVector
        get() = icon(
            "chevron-right",
            "M6.22 3.22 a0.75 0.75 0 0 1 1.06 0 l4.25 4.25 a0.75 0.75 0 0 1 0 1.06 l-4.25 4.25 a0.75 0.75 0 0 1 -1.06 -1.06 L9.94 8 L6.22 4.28 a0.75 0.75 0 0 1 0 -1.06 Z"
        )

    val Commit: ImageVector
        get() = icon(
            "commit",
            "M8 5 A3 3 0 0 1 10.85 7.25 H15 V8.75 H10.85 A3 3 0 0 1 5.15 8.75 H1 V7.25 H5.15 A3 3 0 0 1 8 5 Z M8 6.5 A1.5 1.5 0 1 0 8 9.5 A1.5 1.5 0 0 0 8 6.5 Z"
        )

    val Gear: ImageVector
        get() = icon(
            "gear",
            "M6.8 1.5 H9.2 L9.7 3.4 L10.9 4.1 L12.8 3.5 L14 5.6 L12.6 7 V8.4 L14 10 L12.8 12.1 L10.9 11.5 L9.7 12.2 L9.2 14.1 H6.8 L6.3 12.2 L5.1 11.5 L3.2 12.1 L2 10 L3.4 8.4 V7 L2 5.6 L3.2 3.5 L5.1 4.1 L6.3 3.4 Z M8 5.7 A2.1 2.1 0 1 0 8 9.9 A2.1 2.1 0 0 0 8 5.7 Z"
        )

    val Link: ImageVector
        get() = icon(
            "link",
            "M7.775 3.275 a0.75 0.75 0 0 0 1.06 1.06 l1.25 -1.25 a2 2 0 1 1 2.83 2.83 l-2.5 2.5 a2 2 0 0 1 -2.83 0 a0.75 0.75 0 0 0 -1.06 1.06 a3.5 3.5 0 0 0 4.95 0 l2.5 -2.5 a3.5 3.5 0 0 0 -4.95 -4.95 Z M8.225 12.725 a0.75 0.75 0 0 0 -1.06 -1.06 l-1.25 1.25 a2 2 0 1 1 -2.83 -2.83 l2.5 -2.5 a2 2 0 0 1 2.83 0 a0.75 0.75 0 0 0 1.06 -1.06 a3.5 3.5 0 0 0 -4.95 0 l-2.5 2.5 a3.5 3.5 0 0 0 4.95 4.95 Z"
        )

    val Location: ImageVector
        get() = icon(
            "location",
            "M8 1 A5 5 0 0 1 13 6 C13 9.5 8 15 8 15 C8 15 3 9.5 3 6 A5 5 0 0 1 8 1 Z M8 4 A2 2 0 1 0 8 8 A2 2 0 0 0 8 4 Z"
        )

    val Org: ImageVector
        get() = icon(
            "org",
            "M3 2 A1 1 0 0 1 4 1 H12 A1 1 0 0 1 13 2 V14.5 H10.5 V12 H5.5 V14.5 H3 Z M5 3.5 H7 V5.5 H5 Z M9 3.5 H11 V5.5 H9 Z M5 7 H7 V9 H5 Z M9 7 H11 V9 H9 Z"
        )

    val Close: ImageVector
        get() = icon(
            "close",
            "M3.72 3.72 a0.75 0.75 0 0 1 1.06 0 L8 6.94 l3.22 -3.22 a0.75 0.75 0 1 1 1.06 1.06 L9.06 8 l3.22 3.22 a0.75 0.75 0 1 1 -1.06 1.06 L8 9.06 l-3.22 3.22 a0.75 0.75 0 0 1 -1.06 -1.06 L6.94 8 L3.72 4.78 a0.75 0.75 0 0 1 0 -1.06 Z"
        )

    val Send: ImageVector
        get() = icon(
            "send",
            "M1.5 1.7 L14.8 8 L1.5 14.3 L3.5 8.75 H8 V7.25 H3.5 Z"
        )

    val SignOut: ImageVector
        get() = icon(
            "sign-out",
            "M2 2.75 A0.75 0.75 0 0 1 2.75 2 H8 V3.5 H3.5 V12.5 H8 V14 H2.75 A0.75 0.75 0 0 1 2 13.25 Z M10.44 4.44 L11.5 3.38 L15.12 7 A1.4 1.4 0 0 1 15.12 9 L11.5 12.62 L10.44 11.56 L13.25 8.75 H6 V7.25 H13.25 Z"
        )

    val Book: ImageVector
        get() = icon(
            "book",
            "M1 2.75 A0.75 0.75 0 0 1 1.75 2 H6 A2.5 2.5 0 0 1 8 3 A2.5 2.5 0 0 1 10 2 H14.25 A0.75 0.75 0 0 1 15 2.75 V12.25 A0.75 0.75 0 0 1 14.25 13 H9.8 A1.5 1.5 0 0 0 8.7 13.5 H7.3 A1.5 1.5 0 0 0 6.2 13 H1.75 A0.75 0.75 0 0 1 1 12.25 Z M7.25 4.5 A1 1 0 0 0 6 3.5 H2.5 V11.5 H6.3 A2.9 2.9 0 0 1 7.25 11.8 Z M8.75 11.8 A2.9 2.9 0 0 1 9.7 11.5 H13.5 V3.5 H10 A1 1 0 0 0 8.75 4.5 Z"
        )

    val History: ImageVector
        get() = icon(
            "history",
            "M8 1 A7 7 0 1 1 1.6 5.2 L3 5.8 A5.5 5.5 0 1 0 8 2.5 V5 L4.5 2.5 L8 0 Z M7.25 4.5 H8.75 V8.2 L11.4 9.7 L10.65 11 L7.25 9 Z"
        )

    val Play: ImageVector
        get() = icon(
            "play",
            "M8 1 A7 7 0 1 1 8 15 A7 7 0 0 1 8 1 Z M8 2.5 A5.5 5.5 0 1 0 8 13.5 A5.5 5.5 0 0 0 8 2.5 Z M6.3 5.2 A0.5 0.5 0 0 1 7.05 4.77 L11.05 7.57 A0.5 0.5 0 0 1 11.05 8.43 L7.05 11.23 A0.5 0.5 0 0 1 6.3 10.8 Z"
        )
}
