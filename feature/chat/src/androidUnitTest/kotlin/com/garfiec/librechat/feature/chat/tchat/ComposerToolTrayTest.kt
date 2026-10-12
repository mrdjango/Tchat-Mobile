package com.garfiec.librechat.feature.chat.tchat

import com.garfiec.librechat.core.common.ToolConstants
import com.garfiec.librechat.core.model.config.ImageChoice
import com.garfiec.librechat.core.model.config.ImageChoiceField
import com.garfiec.librechat.core.model.config.ImageOptions
import com.garfiec.librechat.core.model.config.ImageSpec
import com.garfiec.librechat.feature.chat.model.McpServerDisplayData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposerToolTrayTest {

    private val webFetch =
        McpServerDisplayData(name = "fetch", title = "Web fetch", description = null, isConnected = true)
    private val github =
        McpServerDisplayData(name = "github", title = null, description = null, isConnected = true)

    private fun items(
        enabledTools: Set<String> = emptySet(),
        pinned: List<String> = emptyList(),
        selectedMcp: Set<String> = emptySet(),
        showEphemeralTools: Boolean = true,
        imageLabel: String? = null,
        imageOptions: List<ImageOptionChip> = emptyList(),
    ) = trayItems(
        enabledTools = enabledTools,
        pinnedToolKeys = pinned,
        mcpServers = listOf(webFetch, github),
        selectedMcpServerNames = selectedMcp,
        showEphemeralTools = showEphemeralTools,
        imageLabel = imageLabel,
        imageOptions = imageOptions,
    )

    @Test
    fun `image gen first, then tools that are on, then MCP servers, then pinned shortcuts`() {
        val result = items(
            enabledTools = setOf(ToolConstants.CODE_INTERPRETER, IMAGE_GEN_TOOL_KEY),
            pinned = listOf(ToolConstants.WEB_SEARCH),
            selectedMcp = setOf("fetch"),
            imageLabel = "GPT Image",
        )
        assertEquals(
            listOf(
                TrayItem.ImageGen("GPT Image"),
                TrayItem.ActiveTool(ToolConstants.CODE_INTERPRETER),
                TrayItem.Mcp("fetch", "Web fetch"),
                TrayItem.PinnedOff(ToolConstants.WEB_SEARCH),
            ),
            result,
        )
    }

    @Test
    fun `a pinned tool that is on shows once, as on`() {
        val result = items(enabledTools = setOf(ToolConstants.WEB_SEARCH), pinned = listOf(ToolConstants.WEB_SEARCH))
        assertEquals(listOf<TrayItem>(TrayItem.ActiveTool(ToolConstants.WEB_SEARCH)), result)
    }

    @Test
    fun `memory shows only when pinned`() {
        assertTrue(items(enabledTools = setOf(ToolConstants.MEMORY)).isEmpty())
        assertEquals(
            listOf<TrayItem>(TrayItem.ActiveTool(ToolConstants.MEMORY)),
            items(enabledTools = setOf(ToolConstants.MEMORY), pinned = listOf(ToolConstants.MEMORY)),
        )
    }

    @Test
    fun `an MCP server without a title uses its name`() {
        assertEquals(listOf<TrayItem>(TrayItem.Mcp("github", "github")), items(selectedMcp = setOf("github")))
    }

    @Test
    fun `nothing ephemeral shows for agents`() {
        val result = items(
            enabledTools = setOf(ToolConstants.WEB_SEARCH),
            pinned = listOf(ToolConstants.FILE_SEARCH),
            selectedMcp = setOf("fetch"),
            showEphemeralTools = false,
            imageLabel = "GPT Image",
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `item keys are unique so the row can animate them`() {
        val result = items(
            enabledTools = setOf(ToolConstants.WEB_SEARCH, ToolConstants.FILE_SEARCH),
            pinned = listOf(ToolConstants.CODE_INTERPRETER, ToolConstants.WEB_SEARCH),
            selectedMcp = setOf("fetch", "github"),
            imageLabel = "GPT Image",
        )
        assertEquals(result.size, result.map { it.key }.toSet().size)
    }

    private val gemini = ImageSpec(
        name = "gemini",
        label = "Gemini",
        model = "gemini-3-pro-image",
        options = ImageOptions(aspectRatios = listOf("1:1", "16:9"), imageSizes = listOf("1K", "4K")),
    )

    @Test
    fun `image parameters follow the Image Gen chip, ratio before resolution`() {
        val options = imageOptionItems(gemini, ImageChoice(aspectRatio = "16:9"))
        assertEquals(
            listOf(
                ImageOptionChip(ImageChoiceField.ASPECT_RATIO, listOf("1:1", "16:9"), pinned = "16:9"),
                ImageOptionChip(ImageChoiceField.IMAGE_SIZE, listOf("1K", "4K"), pinned = null),
            ),
            options,
        )
        val result =
            items(enabledTools = setOf(ToolConstants.CODE_INTERPRETER), imageLabel = "Gemini", imageOptions = options)
        assertEquals(
            listOf("image", "image_option:ASPECT_RATIO", "image_option:IMAGE_SIZE", "tool:${ToolConstants.CODE_INTERPRETER}"),
            result.map { it.key },
        )
    }

    @Test
    fun `no parameter chips without Image Gen, options, or for a value the model dropped`() {
        val options = imageOptionItems(gemini, null)
        assertTrue(items(imageOptions = options).none { it is TrayItem.ImageOption })
        assertTrue(imageOptionItems(gemini.copy(options = ImageOptions()), null).isEmpty())
        assertTrue(imageOptionItems(gemini.copy(options = null), null).isEmpty())
        assertTrue(imageOptionItems(null, null).isEmpty())
        assertEquals(null, imageOptionItems(gemini, ImageChoice(imageSize = "8K")).last().pinned)
    }
}
