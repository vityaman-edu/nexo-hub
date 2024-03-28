package org.vivlaniv.nexohub

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.redisson.Redisson
import org.redisson.config.Config
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun main() {
    val log = LoggerFactory.getLogger("connection-service")
    log.info("Starting connection-service")

    val prop = Properties()
    prop.load(ClassLoader.getSystemResourceAsStream("app.properties"))

    // get properties
    val redisUrl = prop.getProperty("redis.url", "redis://localhost:6379")
    val getDevicesTopic = prop.getProperty("topic.get.devices", "getDevices")
    val mqttUrl = prop.getProperty("mqtt.url", "tcp://localhost:1883")

    val requests = ConcurrentHashMap<String, Task>()
    val waitResponse = ConcurrentHashMap<String, Task>()

    // create redis client
    val redisConfig = Config()
    redisConfig.useSingleServer().address = redisUrl
    val redissonClient = Redisson.create(redisConfig)

    // create mqtt client
    val mqttOptions = MqttConnectOptions()
    mqttOptions.isAutomaticReconnect = true
    mqttOptions.isCleanSession = true
    mqttOptions.connectionTimeout = 10
    val mqttClient = MqttClient(mqttUrl, MqttClient.generateClientId())
    mqttClient.connect(mqttOptions)

    // subscribe on redis topic
    redissonClient.getTopic("${getDevicesTopic}Out").addListener(String::class.java) { _, msg ->
        log.info("got get devices response")
        val result = Json.decodeFromString<GetDevicesTaskResult>(msg)
        val task = waitResponse.remove(result.tid) as GetDevicesTask

        val request = requests.remove(task.id) as SearchDevicesTask
        val response = SearchDevicesTaskResult(request.id, result.devices)

        val payload = Json.encodeToString(response).encodeToByteArray()
        mqttClient.publish("${task.user}/search/out", payload, 2, false)
        log.info("publish search devices response")
    }

    // subscribe on mosquitto topic
    mqttClient.subscribe("+/+/in", 2) { topic, msg ->
        log.info("got publish on topic {}", topic)
        val match = "(?<user>\\w+)/(?<action>\\w+)/in".toRegex().matchEntire(topic)?.groups
            ?: return@subscribe log.warn("unexpected topic name {}", topic)
        val user = match["user"]?.value!!
        val action = match["action"]?.value!!
        val payload = msg.payload.decodeToString()
        when (action) {
            "search" ->  {
                val requestTask = Json.decodeFromString<SearchDevicesTask>(payload)
                val task = GetDevicesTask(user = user).apply {
                    waitResponse[id] = this
                    requests[id] = requestTask
                }
                redissonClient.getTopic("${getDevicesTopic}In").publish(Json.encodeToString(task))
                log.info("publish get devices request")
            }
            else -> log.warn("unknown action {}", action)
        }
    }

    log.info("connection-service started")
}
