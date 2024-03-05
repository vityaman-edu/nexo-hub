import com.ecwid.clickhouse.mapped.ClickHouseMappedClient
import com.ecwid.clickhouse.transport.httpclient.ApacheHttpClientTransport
import com.ecwid.clickhouse.typed.TypedValues
import org.redisson.Redisson
import org.redisson.config.Config
import java.sql.DriverManager
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

fun main() {
    val prop = Properties()
    prop.load(ClassLoader.getSystemResourceAsStream("app.properties"))

    // get properties
    val redisUrl = prop.getProperty("redis.url", "redis://localhost:6379")
    val logsTopic = prop.getProperty("logs.topic", "logs")
    val chUrl = prop.getProperty("clickhouse.url", "http://localhost:8123")
    val chJdbcUrl = prop.getProperty("clickhouse.url.jdbc", "jdbc:ch://localhost")
    val insertSize = prop.getProperty("clickhouse.insert.size", "50000").toInt()
    val logsTable = prop.getProperty("logs.table", "logs")


    // create redis client
    val config = Config()
    config.useSingleServer().address = redisUrl
    val redissonClient = Redisson.create(config)

    // create clickhouse client
    val clientTransport = ApacheHttpClientTransport()
    val chClient = ClickHouseMappedClient(clientTransport)

    DriverManager.getConnection(chJdbcUrl).use { connection ->
        connection.createStatement().execute("create table if not exists logs (timestamp DATETIME, message String) engine MergeTree order by timestamp")
    }

    // create batch for clickhouse insert
    val batch = Batch(insertSize) {
        chClient.insert(chUrl, logsTable, it, LogMessage::toTypedValues)
    }

    // subscribe on topic
    redissonClient.getTopic(logsTopic).addListener(LogMessage::class.java) { _, msg ->
        batch.add(msg)
    }

    // test stuff
    // redissonClient.getTopic(logsTopic).publish(LogMessage(Instant.now(), "test1"))
    // redissonClient.getTopic(logsTopic).publish(LogMessage(Instant.now(), "test2"))
    // redissonClient.getTopic(logsTopic).publish(LogMessage(Instant.now(), "test3"))
}

data class LogMessage(val timestamp: Instant, val message: String)

fun LogMessage.toTypedValues() = TypedValues().apply {
    setDateTime("timestamp", Date.from(timestamp), TimeZone.getTimeZone(ZoneOffset.UTC))
    setString("message", message)
}

class Batch<T>(private val size: Int, private val action: (List<T>) -> Unit) {
    private val batch: MutableList<T>

    init {
        batch = ArrayList(size)
    }

    fun add(obj: T) {
        batch.add(obj)
        if (batch.size == size) flush()
    }

    private fun flush() {
        action.invoke(batch)
        batch.clear()
    }
}