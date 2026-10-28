package com.garfiec.librechat.core.model.tchat

import kotlin.test.Test
import kotlin.test.assertEquals

class TchatCreditTest {
    @Test
    fun usdIsCentsWithTwoDecimalsAndGrouping() {
        assertEquals("12.34", TchatCredit(1234, "USD").amountText())
        assertEquals("0.05", TchatCredit(5, "USD").amountText())
        assertEquals("1,234,567.00", TchatCredit(123_456_700, "USD").amountText())
    }

    @Test
    fun irtHasNoDecimals() {
        assertEquals("1,250,000", TchatCredit(1_250_000, "IRT").amountText())
        assertEquals("0", TchatCredit(0, "IRT").amountText())
    }

    @Test
    fun negativeBalanceKeepsItsSign() {
        assertEquals("-0.50", TchatCredit(-50, "USD").amountText())
    }
}
