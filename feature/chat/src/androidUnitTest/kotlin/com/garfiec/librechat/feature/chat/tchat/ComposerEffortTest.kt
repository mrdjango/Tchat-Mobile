package com.garfiec.librechat.feature.chat.tchat

import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.EndpointCustomParams
import com.garfiec.librechat.core.ui.components.ModelParameters
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState
import com.garfiec.librechat.feature.chat.viewmodel.ModelSelectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ComposerEffortTest {

    // Tchat's TensorGrid endpoint as `/api/endpoints` serves it.
    private val configs = mapOf(
        "TensorGrid" to EndpointConfig(type = "custom", customParams = EndpointCustomParams("openAI")),
        "Plain" to EndpointConfig(type = "custom", customParams = EndpointCustomParams("google")),
    )

    private fun state(endpoint: String, model: String = "deepseek-v4-flash", params: ModelParameters = ModelParameters()) =
        ChatUiState(
            selection = ModelSelectionState(
                selectedEndpoint = endpoint,
                selectedModel = model,
                endpointConfigs = configs,
                modelParameters = params,
            ),
        )

    @Test
    fun `the TensorGrid endpoint offers effort on every model, Auto by default`() {
        val chip = assertNotNull(state("TensorGrid").composerEffortChip())
        assertEquals("reasoning_effort", chip.definition.key)
        assertEquals(true, "high" in chip.values)
        assertNull(chip.pinned)
    }

    @Test
    fun `picking a level pins it and Auto clears it`() {
        val chip = assertNotNull(state("TensorGrid").composerEffortChip())
        val pinned = chip.applyTo(ModelParameters(), "high")
        assertEquals("high", assertNotNull(state("TensorGrid", params = pinned).composerEffortChip()).pinned)
        val cleared = chip.applyTo(pinned, null)
        assertNull(assertNotNull(state("TensorGrid", params = cleared).composerEffortChip()).pinned)
    }

    @Test
    fun `no chip for agents, unknown endpoints or a parameter set without effort`() {
        assertNull(state("agents").composerEffortChip())
        assertNull(state("Unknown").composerEffortChip())
        assertNull(state("Plain").composerEffortChip())
    }

    @Test
    fun `effort leads the tray`() {
        val chip = assertNotNull(state("TensorGrid").composerEffortChip())
        val items = trayItems(
            enabledTools = emptySet(),
            pinnedToolKeys = emptyList(),
            mcpServers = emptyList(),
            selectedMcpServerNames = emptySet(),
            showEphemeralTools = true,
            imageLabel = "Nano Banana",
            effort = chip,
        )
        assertEquals(listOf("effort", "image"), items.map { it.key })
        assertEquals("Extra high", effortLabel("xhigh"))
    }
}
