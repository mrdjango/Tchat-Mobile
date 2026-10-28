package com.garfiec.librechat.feature.conversations.tchat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.garfiec.librechat.core.data.repository.TchatCreditRepository
import com.garfiec.librechat.core.model.tchat.TchatCredit
import com.garfiec.librechat.feature.conversations.resources.Res
import com.garfiec.librechat.feature.conversations.resources.tchat_credit_irt
import com.garfiec.librechat.feature.conversations.resources.tchat_credit_other
import com.garfiec.librechat.feature.conversations.resources.tchat_credit_usd
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The signed-in user's current TensorGrid credit, one quiet line at the bottom of the drawer.
 * Fetched whenever the drawer is shown and the app is started or resumed; shows nothing until it
 * has an answer, and nothing again after any failure (an older server answers 404). Held per
 * account so a swipe-switch never shows the previous account's balance.
 */
@Composable
fun TchatCreditLine(
    accountId: String?,
    isOpen: Boolean,
    modifier: Modifier = Modifier,
    repository: TchatCreditRepository = koinInject(),
) {
    var credit by remember(accountId) { mutableStateOf<TchatCredit?>(null) }
    val scope = rememberCoroutineScope()

    // Runs on every start/resume while the drawer is showing, so reopening it refreshes too.
    LifecycleStartEffect(accountId, isOpen) {
        val job = if (isOpen) scope.launch { credit = repository.fetch() } else null
        onStopOrDispose { job?.cancel() }
    }

    val current = credit ?: return
    val text = when (current.currency) {
        TchatCredit.USD -> stringResource(Res.string.tchat_credit_usd, current.amountText())
        TchatCredit.IRT -> stringResource(Res.string.tchat_credit_irt, current.amountText())
        else -> stringResource(Res.string.tchat_credit_other, current.amountText(), current.currency)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
