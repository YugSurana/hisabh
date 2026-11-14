package com.hisabh.app

import com.hisabh.app.data.model.TransactionType
import com.hisabh.app.domain.parser.HdfcSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HdfcSmsParserTest {

    @Test
    fun testHdfcSenderValidation() {
        // Valid HDFC Headers
        assertTrue(HdfcSmsParser.isHdfcSender("AD-HDFCBK-T"))
        assertTrue(HdfcSmsParser.isHdfcSender("AD-HDFCBN-S"))
        assertTrue(HdfcSmsParser.isHdfcSender("VM-HDFCBK-T"))
        assertTrue(HdfcSmsParser.isHdfcSender("VK-HDFCBK-T"))
        assertTrue(HdfcSmsParser.isHdfcSender("BZ-HDFCBN-S"))
        assertTrue(HdfcSmsParser.isHdfcSender("BW-HDFCBK-S"))
        assertTrue(HdfcSmsParser.isHdfcSender("HDFCBK"))

        // Invalid senders
        assertFalse(HdfcSmsParser.isHdfcSender("AD-SBIINB-T"))
        assertFalse(HdfcSmsParser.isHdfcSender("VK-ICICIB-T"))
        assertFalse(HdfcSmsParser.isHdfcSender("SPAMMER"))
        assertFalse(HdfcSmsParser.isHdfcSender(null))
        assertFalse(HdfcSmsParser.isHdfcSender(""))
    }

    @Test
    fun testNoiseMessageFiltering() {
        val otpSms = "Your OTP for HDFC Bank NetBanking is 584910. Do not share with anyone."
        assertTrue(HdfcSmsParser.isNoiseMessage(otpSms))
        assertNull(HdfcSmsParser.parse(otpSms))

        val failedSms = "Transaction failed for Rs.500.00 at Swiggy due to insufficient funds."
        assertTrue(HdfcSmsParser.isNoiseMessage(failedSms))
        assertNull(HdfcSmsParser.parse(failedSms))

        val mandateSms = "Mandate creation requested for Rs.1000 on your account."
        assertTrue(HdfcSmsParser.isNoiseMessage(mandateSms))
        assertNull(HdfcSmsParser.parse(mandateSms))

        val declinedSms = "Payment of Rs.250 was declined by bank."
        assertTrue(HdfcSmsParser.isNoiseMessage(declinedSms))
        assertNull(HdfcSmsParser.parse(declinedSms))
    }

    @Test
    fun testSentToFormat() {
        val sms = "Sent Rs.150.00 from HDFC Bank A/C x1234 To CHAI TAPRI On 12/11/26 Ref 631894021123"
        val parsed = HdfcSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals(150.00, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.txType)
        assertEquals("CHAI TAPRI", parsed.merchantRaw)
        assertEquals("631894021123", parsed.utrNumber)
    }

    @Test
    fun testDebitedToVpaFormat() {
        val sms = "Rs. 240.00 debited from a/c *1234 on 12-11-26 to VPA zomato@okaxis (UPI Ref No 631894021123)"
        val parsed = HdfcSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals(240.00, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.txType)
        assertEquals("zomato@okaxis", parsed.merchantRaw)
        assertEquals("631894021123", parsed.utrNumber)
    }

    @Test
    fun testPhonePeDebitFormat() {
        val sms = "Rs. 45.00 debited from a/c *4521 on 13-11-26 to VPA ramesh.tea@ybl (UPI Ref No 631894029988)"
        val parsed = HdfcSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals(45.00, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.txType)
        assertEquals("ramesh.tea@ybl", parsed.merchantRaw)
        assertEquals("631894029988", parsed.utrNumber)
    }

    @Test
    fun testCreditedByVpaFormat() {
        val sms = "Rs. 500.00 credited to a/c *1234 on 12-11-26 by VPA sender@okicici (UPI Ref No 631894021123)"
        val parsed = HdfcSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals(500.00, parsed!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.txType)
        assertEquals("sender@okicici", parsed.merchantRaw)
        assertEquals("631894021123", parsed.utrNumber)
    }

    @Test
    fun testReceivedFromFormat() {
        val sms = "Received Rs.500.00 in your a/c x1234 on 12/11/26 from John Doe Ref 631894021123"
        val parsed = HdfcSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals(500.00, parsed!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.txType)
        assertEquals("John Doe", parsed.merchantRaw)
        assertEquals("631894021123", parsed.utrNumber)
    }

    @Test
    fun testFallbackHashGenerationWhenUtrMissing() {
        val smsWithoutUtr = "Sent Rs.80.00 from HDFC Bank A/C x1234 To Local Store On 12/11/26"
        val parsed = HdfcSmsParser.parse(smsWithoutUtr)

        assertNotNull(parsed)
        assertEquals(80.00, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.txType)
        assertEquals("Local Store", parsed.merchantRaw)
        // Check fallback SHA-256 length is 64 hex characters
        assertEquals(64, parsed.utrNumber.length)
    }
}

