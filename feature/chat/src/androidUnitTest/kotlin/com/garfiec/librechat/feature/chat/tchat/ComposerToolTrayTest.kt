package com.garfiec.librechat.feature.chat.tchat

import com.garfiec.librechat.core.common.ToolConstants
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
    ) = trayItems(
        enabledTools = enabledTools,
        pinnedToolKeys = pinned,
        mcpServers = listOf(webFetch, github),
        selectedMcpServerNames = selectedMcp,
        showEphemeralTools = showEphemeralTools,
        imageLabel = imageLabel,
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
}
