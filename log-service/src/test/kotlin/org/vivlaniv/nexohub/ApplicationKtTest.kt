package org.vivlaniv.nexohub

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.redisson.Redisson
import org.redisson.config.Config
import java.time.Instant

fun main() {
    testLoad()
}

fun testLoad() {
    val config = Config()
    config.useSingleServer().address = "redis://localhost:6379"
    val redissonClient = Redisson.create(config)
    for (i in 0 until 20) {
        Thread{
            for (j in 0 until 100000) {
                redissonClient.getTopic("logs").publish(Json.encodeToString(LogMessage(Instant.now().toEpochMilli(), genMsg())))
            }
            println("thread finished")
        }.start()
    }

    println("all request sent")
}

private fun genMsg(): String {
    val chars = CharArray(1024)
    for (i in chars.indices) {
        chars[i] = 'a' + (Math.random() * 26).toInt()
    }
    return String(chars)
}