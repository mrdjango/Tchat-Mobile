package com.garfiec.librechat.core.data.repository

import com.garfiec.librechat.core.common.result.apiCallCatching
import com.garfiec.librechat.core.model.tchat.TchatCredit
import com.garfiec.librechat.core.network.api.TchatCreditApi

/** The signed-in user's TensorGrid credit, for the drawer's footer line. */
interface TchatCreditRepository {
    /** Null on any failure, including an older server's 404: the line is simply not shown. */
    suspend fun fetch(): TchatCredit?
}

class TchatCreditRepositoryImpl(
    private val api: TchatCreditApi,
) : TchatCreditRepository {
    // Failing by design on an older server, so not routed through safeApiCall's error logging.
    override suspend fun fetch(): TchatCredit? =
        apiCallCatching({ api.getCredit() }, { null })
}
