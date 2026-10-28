package com.garfiec.librechat.feature.chat.tchat

import com.garfiec.librechat.core.common.EndpointConstants
import com.garfiec.librechat.core.model.config.EndpointDropParams
import com.garfiec.librechat.core.model.paramsEndpointFor
import com.garfiec.librechat.core.ui.components.EndpointParameterRegistry
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState

/**
 * The composer's effort chip for the selected model, as web's composer shows one: from the same
 * parameter set the options sheet renders, so the two always agree. Null for agents (their effort
 * is the agent's own), when parameters are hidden, or when the set has no effort.
 */
fun ChatUiState.composerEffortChip(): EffortChip? {
    val endpoint = selectedEndpoint
    if (!parametersEnabled || endpoint.isBlank() || endpoint == EndpointConstants.AGENTS) return null
    val model = selectedModel
    val definitions = EndpointParameterRegistry.getDefinitions(
        endpoint = endpointConfigs.paramsEndpointFor(endpoint),
        extendedEffortSupported = extendedEffortSupported,
        model = model,
        dropParams = EndpointDropParams.resolve(gates.dropParamsMap, endpoint, null, model),
        thinkingDisplayUpdatesSupported = thinkingDisplayUpdatesSupported,
    )
    return effortChipFor(definitions, modelParameters)
}
