package com.garfiec.librechat.core.network.api

import com.garfiec.librechat.core.model.tchat.TchatCredit
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.path

/**
 * Tchat's credit route, `GET /api/tchat/credit` (Tchat `api/server/routes/tchatCredit.js`,
 * JWT-authenticated). Only a Tchat server answers it; an older one 404s.
 */
class TchatCreditApi(
    private val client: HttpClient,
) {
    suspend fun getCredit(): TchatCredit =
        client.get {
            url { path("api/tchat/credit") }
        }.body()
}
