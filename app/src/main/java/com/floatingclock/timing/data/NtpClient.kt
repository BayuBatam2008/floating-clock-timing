package com.floatingclock.timing.data

import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress

class NtpClient {
    suspend fun requestTime(server: String, timeoutMillis: Int = 3_000): Result<NtpResult> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val address = InetAddress.getByName(server)
                DatagramSocket().use { socket ->
                    socket.soTimeout = timeoutMillis
                    socket.connect(address, NTP_PORT)

                    val requestBuffer = ByteArray(NTP_PACKET_SIZE)
                    requestBuffer[0] = NTP_MODE_CLIENT_REQUEST

                    val requestTime = System.currentTimeMillis()
                    val requestTicks = SystemClock.elapsedRealtime()
                    writeTimeStamp(requestBuffer, TRANSMIT_TIME_OFFSET, requestTime)

                    val requestPacket = DatagramPacket(requestBuffer, requestBuffer.size)
                    socket.send(requestPacket)

                    val responseBuffer = ByteArray(NTP_PACKET_SIZE)
                    val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                    socket.receive(responsePacket)

                    if (responsePacket.length < NTP_PACKET_SIZE) {
                        error("Invalid NTP response length: ${responsePacket.length}")
                    }

                    val responseTicks = SystemClock.elapsedRealtime()
                    val responseTime = requestTime + (responseTicks - requestTicks)

                    val originateTime = readTimestamp(responseBuffer, ORIGINATE_TIME_OFFSET)
                        .takeIf { it != 0L } ?: requestTime
                    val receiveTime = readTimestamp(responseBuffer, RECEIVE_TIME_OFFSET)
                    val transmitTime = readTimestamp(responseBuffer, TRANSMIT_TIME_OFFSET)
                    require(receiveTime != 0L && transmitTime != 0L) {
                        "Invalid NTP response timestamps"
                    }

                    val roundTrip = ((responseTime - requestTime) - (transmitTime - receiveTime))
                        .coerceAtLeast(0L)
                    val offset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2L
                    val networkNow = responseTime + offset

                    NtpResult(
                        ntpTimeMillis = networkNow,
                        offsetMillis = offset,
                        roundTripMillis = roundTrip
                    )
                }
            }
        }
    }

    private fun readTimestamp(buffer: ByteArray, offset: Int): Long {
        val seconds = readUnsigned32(buffer, offset)
        val fraction = readUnsigned32(buffer, offset + 4)
        if (seconds == 0L && fraction == 0L) {
            return 0L
        }
        val millis = (seconds - NTP_EPOCH_OFFSET_SECONDS) * 1000L
        val fractionMillis = ((fraction * 1000L) ushr 32)
        return millis + fractionMillis
    }

    private fun readUnsigned32(buffer: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 4) {
            value = (value shl 8) or (buffer[offset + i].toLong() and 0xFF)
        }
        return value
    }

    private fun writeTimeStamp(buffer: ByteArray, offset: Int, time: Long) {
        val seconds = time / 1000L + NTP_EPOCH_OFFSET_SECONDS
        val fraction = ((time % 1000L) * 0x100000000L) / 1000L

        buffer[offset] = (seconds shr 24).toByte()
        buffer[offset + 1] = (seconds shr 16).toByte()
        buffer[offset + 2] = (seconds shr 8).toByte()
        buffer[offset + 3] = seconds.toByte()

        buffer[offset + 4] = (fraction shr 24).toByte()
        buffer[offset + 5] = (fraction shr 16).toByte()
        buffer[offset + 6] = (fraction shr 8).toByte()
        buffer[offset + 7] = fraction.toByte()
    }

    private companion object {
        private const val NTP_PORT = 123
        private const val NTP_PACKET_SIZE = 48
        private const val NTP_MODE_CLIENT_REQUEST: Byte = 0x1B
        private const val TRANSMIT_TIME_OFFSET = 40
        private const val ORIGINATE_TIME_OFFSET = 24
        private const val RECEIVE_TIME_OFFSET = 32
        private const val NTP_EPOCH_OFFSET_SECONDS = 2208988800L
    }
                val client = NTPUDPClient()
                client.defaultTimeout = timeoutMillis
                try {
                    client.open()
                    val address = InetAddress.getByName(server)
                    val timeInfo: TimeInfo = client.getTime(address)
                    timeInfo.computeDetails()
                    val message = timeInfo.message
                    val offset = timeInfo.offset ?: 0L
                    val delay = timeInfo.delay ?: 0L
                    val ntpTime = message.receiveTimeStamp.time
                    NtpResult(
                        ntpTimeMillis = ntpTime,
                        offsetMillis = offset,
                        roundTripMillis = delay
                    )
                } finally {
                    client.close()
                }
            }
        }
    }
}

data class NtpResult(
    val ntpTimeMillis: Long,
    val offsetMillis: Long,
    val roundTripMillis: Long
)
