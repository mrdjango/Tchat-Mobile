package com.garfiec.librechat.core.data.repository

import com.garfiec.librechat.core.model.tchat.TchatCredit
import com.garfiec.librechat.core.network.api.TchatCreditApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TchatCreditRepositoryImplTest {
    private val api = mockk<TchatCreditApi>()
    private val repository = TchatCreditRepositoryImpl(api)

    @Test
    fun `returns the server's credit`() = runTest {
        coEvery { api.getCredit() } returns TchatCredit(1234, "USD")

        assertThat(repository.fetch()).isEqualTo(TchatCredit(1234, "USD"))
    }

    @Test
    fun `any failure hides the line instead of throwing`() = runTest {
        coEvery { api.getCredit() } throws IllegalStateException("404 on an older server")

        assertThat(repository.fetch()).isNull()
    }
}
