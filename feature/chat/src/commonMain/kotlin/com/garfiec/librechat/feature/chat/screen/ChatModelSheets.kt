package com.garfiec.librechat.feature.chat.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.garfiec.librechat.feature.chat.components.ComparisonLane
import com.garfiec.librechat.feature.chat.components.localizedStreamError
import com.garfiec.librechat.feature.chat.tchat.ALL_GROUPS
import com.garfiec.librechat.feature.chat.tchat.TchatModelPickerSheet
import com.garfiec.librechat.feature.chat.tchat.rememberTchatPicker
import com.garfiec.librechat.feature.chat.tchat.showsPinned
import com.garfiec.librechat.feature.chat.tchat.tchatPickerActions
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState
import com.garfiec.librechat.feature.chat.viewmodel.ChatViewModel
import com.garfiec.librechat.feature.chat.viewmodel.SendBlockReason

/*
 * The standalone model-selector sheets, shared by the Android and iOS chat screens. Unlike the
 * selector page in `ChatOptionsBottomSheet`, these open straight to the list and dismiss back to
 * the chat on selection. Both surfaces share the list body, TchatModelPickerContent (TCHAT: was
 * upstream's ModelSelectorSheetContent). While comparing, both show the A/B pair and edit
 * whichever lane is picked there, and during Compare setup the primary one stages instead (see
 * [rememberComparisonSelectorRouting]).
 */

/**
 * The primary standalone selector, driven by `uiState.showModelSheet` (top-bar chip, dual-pane
 * primary, send-block auto-open, which supplies [sendBlockMessage]). Renders unconditionally.
 */
// TCHAT: onNavigateToProviderKeys is kept so upstream's call site merges unchanged.
@Suppress("UnusedParameter")
@Composable
internal fun PrimaryModelSelectorSheet(
    uiState: ChatUiState,
    viewModel: ChatViewModel,
    sendBlockMessage: String?,
    onNavigateToProviderKeys: (endpointName: String?) -> Unit,
    /** Compare setup's staged picks: during setup the list stages into them (see the routing). */
    setupPick: CompareSetupPick? = null,
) {
    val error = uiState.error?.let { localizedStreamError(it) }
    // A send refused for lane B opens on lane B. Read once: dismissing the reason inline must not
    // move the sheet back to lane A.
    val initialTarget = remember {
        if (uiState.sendBlockReason == SendBlockReason.SelectSecondaryModel) ComparisonLane.B else ComparisonLane.A
    }
    val comparison = rememberComparisonSelectorRouting(
        uiState = uiState,
        initialTarget = initialTarget,
        onSelectPrimary = viewModel::onModelSelected,
        onSelectSecondary = viewModel::setSecondaryModel,
        setupPick = setupPick,
        onSelectPrimarySpec = viewModel::onSpecSelected,
        onSelectSecondarySpec = viewModel::setSecondarySpec,
    )
    // TCHAT: the Tchat picker replaces upstream's ModelSelectorSheet here (models by maker, Image
    // Gen as a toggle, primary request only). Tchat has no user-provided keys, so there is no
    // "Set API key" action, and no model specs.
    val picker = rememberTchatPicker(
        uiState = uiState,
        selectedEndpoint = comparison.selectedEndpoint,
        selectedModel = comparison.selectedModel,
        showImageGen = comparison.header == null,
    )
    val close = {
        // Clear a pending snackbar for the same error so it doesn't flash behind the close.
        viewModel.dismissError()
        viewModel.dismissSendBlockReason()
        viewModel.dismissModelSheet()
    }
    TchatModelPickerSheet(
        picker = picker,
        actions = tchatPickerActions(
            viewModel,
            onSelectModel = { endpoint, model ->
                comparison.onModelSelect(endpoint, model)
                close()
            },
        ),
        onDismiss = close,
        initialFilter = uiState.tchat.pickerOpenOn ?: ALL_GROUPS,
        showPinned = uiState.starredModelsDisplay.showsPinned,
        header = comparison.header,
        serverUrl = uiState.serverUrl,
        // Send-block reasons take precedence: when set, the sheet was auto-opened
        // to help the user resolve the block, so surface that context inline.
        errorMessage = sendBlockMessage ?: error,
        onErrorDismiss = {
            viewModel.dismissSendBlockReason()
            viewModel.dismissError()
        },
    )
}

/**
 * The selector opened for comparison mode's second model (pane B's header, the phone chip on the A
 * tab). Opens on lane B. Renders unconditionally.
 */
@Suppress("UnusedParameter") // TCHAT: see PrimaryModelSelectorSheet
@Composable
internal fun SecondaryModelSelectorSheet(
    uiState: ChatUiState,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onNavigateToProviderKeys: (endpointName: String?) -> Unit,
) {
    // Route through prepareModelSelector like every selector path, so a failed cold-start agent
    // load can self-heal here too (see its KDoc).
    LaunchedEffect(Unit) { viewModel.prepareModelSelector() }

    val comparison = rememberComparisonSelectorRouting(
        uiState = uiState,
        initialTarget = ComparisonLane.B,
        onSelectPrimary = viewModel::onModelSelected,
        onSelectSecondary = viewModel::setSecondaryModel,
        onSelectPrimarySpec = viewModel::onSpecSelected,
        onSelectSecondarySpec = viewModel::setSecondarySpec,
    )
    // TCHAT: the Tchat picker, without Image Gen (only the primary request carries it).
    val picker = rememberTchatPicker(
        uiState = uiState,
        selectedEndpoint = comparison.selectedEndpoint,
        selectedModel = comparison.selectedModel,
        showImageGen = false,
    )
    TchatModelPickerSheet(
        picker = picker,
        actions = tchatPickerActions(
            viewModel,
            onSelectModel = { endpoint, model ->
                comparison.onModelSelect(endpoint, model)
                onDismiss()
            },
        ),
        header = comparison.header,
        onDismiss = onDismiss,
        showPinned = uiState.starredModelsDisplay.showsPinned,
        serverUrl = uiState.serverUrl,
    )
}
