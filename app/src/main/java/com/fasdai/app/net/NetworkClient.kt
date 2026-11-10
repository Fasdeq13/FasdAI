package com.fasdai.app.net

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class CustomDns(private val serverIp: String) : Dns {

    private val cache = ConcurrentHashMap<String, List<InetAddress>>()

    override fun lookup(hostname: String): List<InetAddress> {
        cache[hostname]?.let { return it }
        return try {
            val resolved = UdpDnsResolver.resolveA(serverIp, hostname)
            if (resolved.isNotEmpty()) {
                cache[hostname] = resolved
                resolved
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
        } catch (e: Exception) {
            Dns.SYSTEM.lookup(hostname)
        }
    }
}

/** A minimal stub DNS client: one A-record query over UDP, no recursion flags beyond the default. */
object UdpDnsResolver {

    fun resolveA(server: String, hostname: String): List<InetAddress> {
        val query = buildQuery(hostname)
        DatagramSocket().use { socket ->
            socket.soTimeout = 4000
            val serverAddr = InetAddress.getByName(server)
            socket.send(DatagramPacket(query, query.size, serverAddr, 53))
            val buf = ByteArray(512)
            val packet = DatagramPacket(buf, buf.size)
            socket.receive(packet)
            return parseAnswers(buf, packet.length)
        }
    }

    private fun buildQuery(hostname: String): ByteArray {
        val id = Random.nextInt(0, 0xFFFF)
        val header = byteArrayOf(
            (id shr 8).toByte(), id.toByte(),
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
        )
        val question = ArrayList<Byte>()
        for (label in hostname.split(".")) {
            question.add(label.length.toByte())
            question.addAll(label.toByteArray(Charsets.US_ASCII).toList())
        }
        question.add(0)
        question.add(0x00); question.add(0x01)
        question.add(0x00); question.add(0x01)
        return header + question.toByteArray()
    }

    private fun parseAnswers(buf: ByteArray, length: Int): List<InetAddress> {
        if (length < 12) return emptyList()
        val answerCount = ((buf[6].toInt() and 0xFF) shl 8) or (buf[7].toInt() and 0xFF)
        if (answerCount <= 0) return emptyList()
        var pos = 12
        while (pos < length && buf[pos].toInt() != 0) {
            val len = buf[pos].toInt() and 0xFF
            if (len and 0xC0 == 0xC0) { pos += 2; break }
            pos += len + 1
        }
        pos += 1 + 4
        val result = ArrayList<InetAddress>()
        repeat(answerCount) {
            if (pos + 12 > length) return@repeat
            if (buf[pos].toInt() and 0xC0 == 0xC0) pos += 2 else {
                while (pos < length && buf[pos].toInt() != 0) pos += (buf[pos].toInt() and 0xFF) + 1
                pos += 1
            }
            val type = ((buf[pos].toInt() and 0xFF) shl 8) or (buf[pos + 1].toInt() and 0xFF)
            val rdLength = ((buf[pos + 8].toInt() and 0xFF) shl 8) or (buf[pos + 9].toInt() and 0xFF)
            val rdOffset = pos + 10
            if (type == 1 && rdLength == 4 && rdOffset + 4 <= length) {
                val bytes = byteArrayOf(buf[rdOffset], buf[rdOffset + 1], buf[rdOffset + 2], buf[rdOffset + 3])
                result.add(InetAddress.getByAddress(bytes))
            }
            pos = rdOffset + rdLength
        }
        return result
    }
}

fun buildHttpClient(proxyHost: String?, proxyPort: Int?, customDns: String?): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)

    if (!proxyHost.isNullOrBlank() && proxyPort != null) {
        builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
    }
    if (!customDns.isNullOrBlank()) {
        builder.dns(CustomDns(customDns))
    }
    return builder.build()
}
