package com.garfiec.librechat.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParamsEndpointForTest {

    // Shape of Tchat's `/api/endpoints` (2026-10-28).
    private val configs = Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, EndpointConfig>>(
        """
        {
          "agents": { "type": "agents" },
          "TensorGrid": { "type": "custom", "customParams": { "defaultParamsEndpoint": "openAI", "paramDefinitions": [{ "key": "reasoning_effort" }] } },
          "TensorGrid-Claude": { "type": "custom", "customParams": { "defaultParamsEndpoint": "anthropic" } },
          "Plain": { "type": "custom" }
        }
        """,
    )

    @Test
    fun customEndpointsUseTheirDeclaredParameterSet() {
        assertEquals("openAI", configs.paramsEndpointFor("TensorGrid"))
        assertEquals("anthropic", configs.paramsEndpointFor("TensorGrid-Claude"))
        assertEquals("custom", configs.paramsEndpointFor("Plain"))
    }

    @Test
    fun otherEndpointsKeepTheirName() {
        assertEquals("agents", configs.paramsEndpointFor("agents"))
        assertEquals("google", configs.paramsEndpointFor("google"))
        assertNull(configs.paramsEndpointForOrNull(null))
    }
}
