package com.garfiec.librechat.feature.chat.tchat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.common.ToolConstants
import com.garfiec.librechat.core.ui.components.focusRing
import com.garfiec.librechat.core.ui.theme.LocalReduceMotion
import com.garfiec.librechat.core.ui.theme.focusRingColor
import com.garfiec.librechat.feature.chat.components.ephemeralToolMeta
import com.garfiec.librechat.feature.chat.model.McpServerDisplayData
import com.garfiec.librechat.feature.chat.resources.Res
import com.garfiec.librechat.feature.chat.resources.tchat_image_chip_cd
import com.garfiec.librechat.feature.chat.resources.tchat_image_chip_remove
import com.garfiec.librechat.feature.chat.resources.tchat_tray_on_cd
import com.garfiec.librechat.feature.chat.resources.tchat_tray_remove
import com.garfiec.librechat.feature.chat.resources.tchat_tray_turn_on
import org.jetbrains.compose.resources.stringResource

/**
 * What the composer's tool tray needs beyond `ChatInputState`: the Image Gen chip and the actions
 * the upstream composer doesn't carry. Non-null switches the composer to the tray.
 */
@Immutable
class ComposerTrayExtras(
    /** The active image entry's label, or null while Image Gen is off or hidden. */
    val imageLabel: String?,
    val onOpenImage: () -> Unit,
    val onTurnOffImage: () -> Unit,
    val onOpenTools: () -> Unit,
    val onToggleMcpServer: (String) -> Unit,
)

/** One chip in the tray, in display order. */
internal sealed interface TrayItem {
    val key: String

    data class ImageGen(val label: String) : TrayItem {
        override val key: String get() = "image"
    }

    /** An ephemeral tool that's on; ✕ toggles it off. */
    data class ActiveTool(val tool: String) : TrayItem {
        override val key: String get() = "tool:$tool"
    }

    /** A selected MCP server; ✕ deselects it. */
    data class Mcp(val name: String, val label: String) : TrayItem {
        override val key: String get() = "mcp:$name"
    }

    /** A server-pinned tool that's off: a one-tap shortcut to turn it on. */
    data class PinnedOff(val tool: String) : TrayItem {
        override val key: String get() = "pinned:$tool"
    }
}

/** Tools that show as on whenever they're in the enabled set; others only when pinned. */
private val ALWAYS_SHOWN_TOOLS = listOf(
    ToolConstants.WEB_SEARCH,
    ToolConstants.URL_CONTEXT,
    ToolConstants.CODE_INTERPRETER,
    ToolConstants.FILE_SEARCH,
)

/**
 * The tray's chips: Image Gen, then the tools that are on, then MCP servers, then pinned tools
 * that are off. A pinned tool that's on shows once, as on. Nothing ephemeral shows for agents.
 */
internal fun trayItems(
    enabledTools: Set<String>,
    pinnedToolKeys: List<String>,
    mcpServers: List<McpServerDisplayData>,
    selectedMcpServerNames: Set<String>,
    showEphemeralTools: Boolean,
    imageLabel: String?,
): List<TrayItem> {
    if (!showEphemeralTools) return emptyList()
    val active = (ALWAYS_SHOWN_TOOLS + pinnedToolKeys)
        .distinct()
        .filter { it in enabledTools && it != IMAGE_GEN_TOOL_KEY && ephemeralToolMeta(it) != null }
    return buildList {
        imageLabel?.let { add(TrayItem.ImageGen(it)) }
        active.forEach { add(TrayItem.ActiveTool(it)) }
        mcpServers.filter { it.name in selectedMcpServerNames }
            .forEach { add(TrayItem.Mcp(it.name, it.title ?: it.name)) }
        pinnedToolKeys.filter { it !in enabledTools && ephemeralToolMeta(it) != null }
            .forEach { add(TrayItem.PinnedOff(it)) }
    }
}

/**
 * One row of chips above the composer field, aligned with its edge: what's on (tonal, with ✕) and
 * the server's pinned shortcuts (outlined). Scrolls sideways rather than wrapping, so it never
 * pushes the field up more than one row.
 */
@Composable
internal fun ComposerToolTray(
    items: List<TrayItem>,
    extras: ComposerTrayExtras,
    onToggleTool: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val reduceMotion = LocalReduceMotion.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(items, key = { it.key }) { item ->
            val itemModifier = if (reduceMotion) Modifier else Modifier.animateItem()
            TrayItemChip(item, extras, onToggleTool, itemModifier)
        }
    }
}

@Composable
private fun TrayItemChip(
    item: TrayItem,
    extras: ComposerTrayExtras,
    onToggleTool: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is TrayItem.ImageGen -> ActiveChip(
            icon = Icons.Default.Image,
            label = item.label,
            bodyDescription = stringResource(Res.string.tchat_image_chip_cd, item.label),
            removeDescription = stringResource(Res.string.tchat_image_chip_remove),
            onClick = extras.onOpenImage,
            onRemove = extras.onTurnOffImage,
            modifier = modifier,
        )
        is TrayItem.ActiveTool -> {
            val meta = ephemeralToolMeta(item.tool) ?: return
            val label = stringResource(meta.titleRes)
            ActiveChip(
                icon = meta.icon,
                label = label,
                bodyDescription = stringResource(Res.string.tchat_tray_on_cd, label),
                removeDescription = stringResource(Res.string.tchat_tray_remove, label),
                onClick = extras.onOpenTools,
                onRemove = { onToggleTool(item.tool) },
                modifier = modifier,
            )
        }
        is TrayItem.Mcp -> ActiveChip(
            icon = Icons.Default.Extension,
            label = item.label,
            bodyDescription = stringResource(Res.string.tchat_tray_on_cd, item.label),
            removeDescription = stringResource(Res.string.tchat_tray_remove, item.label),
            onClick = extras.onOpenTools,
            onRemove = { extras.onToggleMcpServer(item.name) },
            modifier = modifier,
        )
        is TrayItem.PinnedOff -> {
            val meta = ephemeralToolMeta(item.tool) ?: return
            val label = stringResource(meta.titleRes)
            PinnedChip(
                icon = meta.icon,
                label = label,
                description = stringResource(Res.string.tchat_tray_turn_on, label),
                onTurnOn = { onToggleTool(item.tool) },
                modifier = modifier,
            )
        }
    }
}

/** Design `Chip`: 36dp tall, `radius-sm`, hit area extended to `size-touch` vertically. */
private val ChipHeight = 36.dp
private val TouchHeight = 48.dp
private val ChipRadius = 8.dp
private val ChipShape = RoundedCornerShape(ChipRadius)
private val StartShape = RoundedCornerShape(topStart = ChipRadius, bottomStart = ChipRadius)
private val EndShape = RoundedCornerShape(topEnd = ChipRadius, bottomEnd = ChipRadius)
private val MaxLabelWidth = 160.dp

/**
 * An on chip in `accent-container`: the body opens where it's configured, ✕ turns it off. Two
 * targets, each the full 48dp tall; only the 36dp pill is drawn.
 */
@Composable
private fun ActiveChip(
    icon: ImageVector,
    label: String,
    bodyDescription: String,
    removeDescription: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = colors.primaryContainer
    val content = colors.onPrimaryContainer
    val pressedOverlay = content.copy(alpha = PRESSED_ALPHA)
    val bodySource = remember { MutableInteractionSource() }
    val removeSource = remember { MutableInteractionSource() }
    val bodyPressed by bodySource.collectIsPressedAsState()
    val removePressed by removeSource.collectIsPressedAsState()
    Row(
        modifier = modifier
            .height(TouchHeight)
            .focusRing(ChipShape, focusRingColor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .pill(StartShape, container, if (bodyPressed) pressedOverlay else null)
                .clickable(interactionSource = bodySource, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = bodyDescription }
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = MaxLabelWidth),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(30.dp)
                .pill(EndShape, container, if (removePressed) pressedOverlay else null)
                .clickable(interactionSource = removeSource, indication = null, role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = removeDescription },
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = content,
                modifier = Modifier.padding(start = 6.dp).size(16.dp),
            )
        }
    }
}

/** An off pinned tool: outlined, muted, one tap turns it on. */
@Composable
private fun PinnedChip(
    icon: ImageVector,
    label: String,
    description: String,
    onTurnOn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val content = colors.onSurfaceVariant
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier = modifier
            .height(TouchHeight)
            .focusRing(ChipShape, focusRingColor)
            .pill(
                ChipShape,
                if (pressed) content.copy(alpha = PRESSED_ALPHA) else Color.Transparent,
                border = BorderStroke(1.dp, colors.outlineVariant),
            )
            .toggleable(
                value = false,
                interactionSource = source,
                indication = null,
                role = Role.Switch,
                onValueChange = { onTurnOn() },
            )
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = MaxLabelWidth),
        )
    }
}

private const val PRESSED_ALPHA = 0.12f

/**
 * Draws a [ChipHeight] pill centred in a taller touch area, so the hit target reaches
 * `size-touch` without the chip looking 48dp tall. [overlay] is the pressed state layer.
 */
private fun Modifier.pill(
    shape: Shape,
    color: Color,
    overlay: Color? = null,
    border: BorderStroke? = null,
    height: Dp = ChipHeight,
): Modifier = drawBehind {
    val pillHeight = height.toPx()
    val top = (size.height - pillHeight) / 2f
    val outline = shape.createOutline(Size(size.width, pillHeight), layoutDirection, this)
    translate(top = top) {
        drawOutline(outline, color)
        overlay?.let { drawOutline(outline, it) }
        border?.let { drawOutline(outline, it.brush, style = Stroke(it.width.toPx())) }
    }
}
