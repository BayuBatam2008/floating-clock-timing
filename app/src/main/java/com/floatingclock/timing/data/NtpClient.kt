package com.floatingclock.timing.data

import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo

class NtpClient {
    suspend fun requestTime(server: String, timeoutMillis: Int = 3_000): Result<NtpResult> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val client = NTPUDPClient()
                client.defaultTimeout = timeoutMillis
                try {
                    client.open()
                    
                    // Try to resolve hostname with better error handling
                    val address = try {
                        InetAddress.getByName(server)
                    } catch (e: UnknownHostException) {
                        throw Exception("Unable to resolve hostname '$server'. Check your internet connection and DNS settings.", e)
                    }
                    
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
                    if (client.isOpen) {
                        client.close()
                    }
                }
            }
        }
    }
    
    suspend fun requestTimeWithFallback(servers: List<String>, timeoutMillis: Int = 3_000): Result<NtpResult> {
        var lastException: Exception? = null
        
        for (server in servers) {
            try {
                val result = requestTime(server, timeoutMillis)
                if (result.isSuccess) {
                    return result
                }
                lastException = result.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
            }
        }
        
        return Result.failure(lastException ?: Exception("All NTP servers failed"))
    }
}

data class NtpResult(
    val ntpTimeMillis: Long,
    val offsetMillis: Long,
    val roundTripMillis: Long
)
