package io.github.rhythmcache.dioxamine.adb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class AdbConnectionValidationTest {
    @Test
    fun `wireless connection rejects unusable ports`() {
        listOf("", "0", "-1", "65536", "999999999999", "abc").forEach { assertFalse(it, isValidPort(it)) }
        listOf("1", "5555", "37099", "65535").forEach { assertTrue(it, isValidPort(it)) }
    }

    @Test
    fun `wireless connection accepts local and valid IPv4 endpoints`() {
        listOf("localhost", "127.0.0.1", "192.168.1.142", "10.0.2.15").forEach { assertTrue(it, isValidIp(it)) }
        listOf("", "300.1.1.1", "192.168.1", "device", "192.168.1.1:5555").forEach { assertFalse(it, isValidIp(it)) }
    }

    @Test
    fun `pairing requires the complete six digit code and preserves leading zeroes`() {
        assertTrue(isValidPairingCode("012345"))
        assertTrue(isValidPairingCode("000000"))
        listOf("", "1", "12345", "1234567", "12a456", "12 456", "１２３４５６").forEach {
            assertFalse(it, isValidPairingCode(it))
        }
    }

    @Test
    fun `pasting a device endpoint separates address and port`() {
        assertEquals("192.168.1.142" to "37099", parseIpAndPort(" 192.168.1.142:37099 "))
        assertEquals("localhost" to null, parseIpAndPort("localhost"))
    }
}
