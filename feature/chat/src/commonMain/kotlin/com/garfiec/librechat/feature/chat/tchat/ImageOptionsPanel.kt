package com.garfiec.librechat.feature.chat.tchat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.model.config.ImageChoice
import com.garfiec.librechat.core.model.config.ImageChoiceField
import com.garfiec.librechat.core.model.config.ImageSpec
import com.garfiec.librechat.core.model.config.imageSizeLabel
import com.garfiec.librechat.core.ui.components.AdaptiveFilterChip
import com.garfiec.librechat.feature.chat.resources.Res
import com.garfiec.librechat.feature.chat.resources.tchat_image_aspect_ratio
import com.garfiec.librechat.feature.chat.resources.tchat_image_auto
import com.garfiec.librechat.feature.chat.resources.tchat_image_options
import com.garfiec.librechat.feature.chat.resources.tchat_image_options_hint
import com.garfiec.librechat.feature.chat.resources.tchat_image_resolution
import com.garfiec.librechat.feature.chat.resources.tchat_image_size
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val FieldTitles: Map<ImageChoiceField, StringResource> = mapOf(
    ImageChoiceField.SIZE to Res.string.tchat_image_size,
    ImageChoiceField.ASPECT_RATIO to Res.string.tchat_image_aspect_ratio,
    ImageChoiceField.IMAGE_SIZE to Res.string.tchat_image_resolution,
)

/**
 * The active image model's parameters, under the picker's Image Gen group: one chip row per
 * parameter the model honours, each starting at Auto, which leaves the choice to the chat model.
 * Mirrors web's Image Gen chip menu. Picking a chip keeps the sheet open, unlike a model row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImageOptionsPanel(
    spec: ImageSpec,
    choice: ImageChoice?,
    onChoice: (ImageChoiceField, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = spec.options ?: return
    val autoLabel = stringResource(Res.string.tchat_image_auto)
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${stringResource(Res.string.tchat_image_options)} · ${spec.label}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.tchat_image_options_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ImageChoiceField.entries.forEach { field ->
            val values = options.valuesFor(field)
            if (values.isEmpty()) return@forEach
            val pinned = choice?.get(field)?.takeIf { it in values }
            Text(
                text = stringResource(FieldTitles.getValue(field)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AdaptiveFilterChip(
                    selected = pinned == null,
                    onClick = { onChoice(field, null) },
                    label = { Text(autoLabel, maxLines = 1) },
                )
                values.forEach { value ->
                    AdaptiveFilterChip(
                        selected = pinned == value,
                        onClick = { onChoice(field, value) },
                        label = {
                            Text(if (field == ImageChoiceField.SIZE) imageSizeLabel(value) else value, maxLines = 1)
                        },
                    )
                }
            }
        }
    }
}
