package org.vivlaniv.nexohub

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.redisson.Redisson
import org.redisson.config.Config
import org.slf4j.LoggerFactory
import java.util.*

fun main() {
    val log = LoggerFactory.getLogger("model-service")
    log.info("Starting model-service")

    val prop = Properties()
    prop.load(ClassLoader.getSystemResourceAsStream("app.properties"))

    // get properties
    val redisUrl = prop.getProperty("redis.url", "redis://localhost:6379")
    val getDevicesTopic = prop.getProperty("topic.get.devices", "getDevices")
    val getDevicesPropertiesTopic = prop.getProperty("topic.get.devices.properties", "getDevicesProperties")
    val getDevicePropertiesTopic = prop.getProperty("topic.get.device.properties", "getDeviceProperties")
    val setDevicePropertyTopic = prop.getProperty("topic.set.device.property", "setDeviceProperty")
    val signalDeviceTopic = prop.getProperty("topic.signal.device", "signalDevice")

    log.info("Properties loaded")

    val userToDevices: MutableMap<String, MutableMap<String, Device>> = mutableMapOf(
        "user" to mutableMapOf(
            Lamp().let { it.getId() to it },
            Teapot().let { it.getId() to it }
        )
    )

    fun userDevicesOrEmpty(user: String) = userToDevices.computeIfAbsent(user) { mutableMapOf() }
    fun userDeviceOrNull(user: String, device: String) = userToDevices[user]?.get(device)

    log.info("Model initialized")

    // create redis client
    val redisConfig = Config()
    redisConfig.useSingleServer().address = redisUrl
    val redissonClient = Redisson.create(redisConfig)

    // subscribe on topics
    redissonClient.getTopic("${getDevicesTopic}In").addListener(String::class.java) { _, msg ->
        val task = Json.decodeFromString<GetDevicesTask>(msg)
        val devices = userDevicesOrEmpty(task.user).values.map { it.getInfo() }
        val result = GetDevicesTaskResult(task.id, devices)
        redissonClient.getTopic("${getDevicesTopic}Out").publish(Json.encodeToString(result))
    }

    redissonClient.getTopic("${getDevicesPropertiesTopic}In").addListener(String::class.java) { _, msg ->
        val task = Json.decodeFromString<GetDevicesPropertiesTask>(msg)
        val properties = userDevicesOrEmpty(task.user).mapValues { it.value.getProperties() }
        val result = GetDevicesPropertiesTaskResult(task.id, properties)
        redissonClient.getTopic("${getDevicesPropertiesTopic}Out").publish(Json.encodeToString(result))
    }

    redissonClient.getTopic("${getDevicePropertiesTopic}In").addListener(String::class.java) { _, msg ->
        val task = Json.decodeFromString<GetDevicePropertiesTask>(msg)
        val properties = userDeviceOrNull(task.user, task.device)?.getProperties() ?: listOf()
        val result = GetDevicePropertiesTaskResult(task.id, properties)
        redissonClient.getTopic("${getDevicePropertiesTopic}Out").publish(Json.encodeToString(result))
    }

    redissonClient.getTopic("${setDevicePropertyTopic}In").addListener(String::class.java) { _, msg ->
        val task = Json.decodeFromString<SetDevicePropertyTask>(msg)
        userDeviceOrNull(task.user, task.device)?.setProperty(task.name, task.value)
        val result = SetDevicePropertyTaskResult(task.id)
        redissonClient.getTopic("${setDevicePropertyTopic}Out").publish(Json.encodeToString(result))
    }

    redissonClient.getTopic("${signalDeviceTopic}In").addListener(String::class.java) { _, msg ->
        val task = Json.decodeFromString<SignalDeviceTask>(msg)
        userDeviceOrNull(task.user, task.device)?.signal(task.name, task.args)
        val result = SignalDeviceTaskResult(task.id)
        redissonClient.getTopic("${signalDeviceTopic}Out").publish(Json.encodeToString(result))
    }

    log.info("model-service started")
}
