package com.mobilehub.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Apple 风格悬浮液态玻璃导航栏。
 * 基于 Kyant0/AndroidLiquidGlass (io.github.kyant0:backdrop)：
 * 折射 lens + 模糊 blur + 提饱和 vibrancy，配半透明表面保证可读性。
 * Android 13+ 有完整折射效果，12 仅模糊，更低版本退化为半透明悬浮栏。
 */
@Composable
fun LiquidNavBar(
    items: List<Pair<ImageVector, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val surfaceTint = if (dark) Color(0xFF1C1C1E).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.5f)
    val activeColor = MiuixTheme.colorScheme.primary
    val inactiveColor = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.65f)

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                    lens(14.dp.toPx(), 28.dp.toPx())
                },
                onDrawSurface = { drawRect(surfaceTint) },
            )
            .height(64.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val active = i == selected
            val tint = if (active) activeColor else inactiveColor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(i) }
                    .width(64.dp)
                    .fillMaxHeight(),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = tint,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
