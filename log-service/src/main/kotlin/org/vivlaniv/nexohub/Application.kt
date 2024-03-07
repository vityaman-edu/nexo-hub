package org.vivlaniv.nexohub

import com.clickhouse.client.ClickHouseClient
import com.clickhouse.client.ClickHouseNode
import com.clickhouse.client.ClickHouseProtocol
import com.clickhouse.client.ClickHouseResponse
import com.clickhouse.data.ClickHouseDataStreamFactory
import com.clickhouse.data.ClickHouseFormat
import com.clickhouse.data.format.BinaryStreamUtils
import org.redisson.Redisson
import org.redisson.config.Config
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.CompletableFuture

fun main() {
    val log = LoggerFactory.getLogger("log-service")
    log.info("Starting log-service")

    val prop = Properties()
    prop.load(ClassLoader.getSystemResourceAsStream("app.properties"))

    // get properties
    val redisUrl = prop.getProperty("redis.url", "redis://localhost:6379")
    val logsTopic = prop.getProperty("logs.topic", "logs")
    val chUrl = prop.getProperty("clickhouse.url", "http://localhost:8123")
    val insertSize = prop.getProperty("clickhouse.insert.size", "50000").toInt()
    val logsTable = prop.getProperty("logs.table", "logs")

    log.info("Properties loaded")

    // create redis client
    val redisConfig = Config()
    redisConfig.useSingleServer().address = redisUrl
    val redissonClient = Redisson.create(redisConfig)

    // create clickhouse client
    val chNode = ClickHouseNode.of(chUrl)

    ClickHouseClient.newInstance(ClickHouseProtocol.HTTP).use { client ->
        client.read(chNode)
            .query("create table if not exists $logsTable (timestamp DATETIME, message String) engine MergeTree order by timestamp")
            .execute().get()
    }

    // create insert batch
    val batch = Batch<LogMessage>(insertSize) {
        log.info("Batch of new log messages inserting")
        val writtenRows: Long
        ClickHouseClient.newInstance(ClickHouseProtocol.HTTP).use { client ->
            val request = client.read(chNode).write().table(logsTable).format(ClickHouseFormat.RowBinary)
            val config = request.config
            val future: CompletableFuture<ClickHouseResponse>
            ClickHouseDataStreamFactory.getInstance().createPipedOutputStream(config).use { stream ->
                future = request.data(stream.inputStream).execute()
                for (message in it) {
                    BinaryStreamUtils.writeDateTime(stream, LocalDateTime.ofInstant(message.timestamp, ZoneOffset.UTC), TimeZone.getTimeZone(ZoneOffset.UTC))
                    BinaryStreamUtils.writeString(stream, message.message)
                }
            }
            writtenRows = future.get().summary.writtenRows
        }
        log.info("Batch of $writtenRows log messages inserted")
    }

    // subscribe on topic
    redissonClient.getTopic(logsTopic).addListener(LogMessage::class.java) { _, msg ->
        batch.add(msg)
    }

    log.info("log-service started")
}

data class LogMessage(val timestamp: Instant, val message: String)

class Batch<T>(private val size: Int, private val action: (List<T>) -> Unit) {
    private var batch: MutableList<T>

    init {
        batch = ArrayList(size)
    }

    fun add(obj: T) {
        synchronized(this) {
            batch.add(obj)
            if (batch.size == size) {
                val oldBatch = batch
                Thread{ action.invoke(oldBatch) }.start()
                batch = ArrayList(size)
            }
        }
    }
}