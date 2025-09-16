package com.floatingclock.timing.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress

class NtpClient {
    suspend fun requestTime(server: String, timeoutMillis: Int = 3_000): Result<NtpResult> {
        return runCatching {
            withContext(Dispatchers.IO) {
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
