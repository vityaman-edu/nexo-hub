package org.vivlaniv.nexohub

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

// Model

data class PropertyInfo(val name: String, val readOnly: Boolean, val value: Any)
data class DeviceProperties(val properties: List<PropertyInfo>)

data class SignalInfo(val name: String, val args: List<Class<*>>)
data class DeviceInfo(val signals: List<SignalInfo>)

interface Device {
    fun getProperties(): DeviceProperties
    fun setProperty(name: String, value: Any)
    fun getInfo(): DeviceInfo
    fun signal(name: String, vararg args: Any)
}

data class Room(val devices: List<Device>)

data class House(val rooms: List<Room>, val owner: String)

// Devices

abstract class AbstractDevice : Device {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun getProperties() = DeviceProperties(listOf())
    override fun setProperty(name: String, value: Any) = log.warn("unknown property $name for $this")
    override fun getInfo(): DeviceInfo = DeviceInfo(listOf())
    override fun signal(name: String, vararg args: Any) = log.warn("unknown signal $name for $this")
}

class Lamp(
    private var turn: Boolean = false,
    private var brightness: Int = 0,
    private var red: Int = 255,
    private var green: Int = 255,
    private var blue: Int = 255
) : AbstractDevice() {
    override fun getProperties() = DeviceProperties(
        listOf(
            PropertyInfo("turn", false, turn),
            PropertyInfo("brightness", false, brightness),
            PropertyInfo("red", false, red),
            PropertyInfo("green", false, green),
            PropertyInfo("blue", false, blue)
        )
    )

    override fun setProperty(name: String, value: Any) {
        when (name) {
            "turn" -> if (value is Boolean) turn = value
            "brightness" -> if (value is Int) brightness = max(0, min(255, value))
            "red" -> if (value is Int) red = max(0, min(255, value))
            "green" -> if (value is Int) green = max(0, min(255, value))
            "blue" -> if (value is Int) blue = max(0, min(255, value))
            else -> super.setProperty(name, value)
        }
    }
}

class Teapot(
    private var volume: Int = 0,
    private var temperature: Int = 0
) : AbstractDevice() {
    init {
        runBlocking {
            launch {
                while (true) {
                    if (temperature > 20) temperature--
                    delay(10_000)
                }
            }
        }
    }

    override fun getProperties() = DeviceProperties(
        listOf(
            PropertyInfo("volume", true, volume),
            PropertyInfo("temperature", true, temperature)
        )
    )

    override fun getInfo() = DeviceInfo(
        listOf(
            SignalInfo("boil", listOf()),
            SignalInfo("hold_temperature", listOf(Int::class.java))
        )
    )

    override fun signal(name: String, vararg args: Any) {
        when (name) {
            "boil" -> runBlocking {
                launch {
                    while (temperature < 100) {
                        temperature++
                        delay(1_000)
                    }
                }
            }
            "hold_temperature" -> runBlocking {
                launch {
                    val target = args[0] as? Int ?: return@launch
                    while (temperature < target) {
                        temperature++
                        delay(1_000)
                    }
                    val finish = System.currentTimeMillis() + 60_000
                    while (System.currentTimeMillis() < finish) {
                        if (temperature < target) temperature++
                        delay(1_000)
                    }
                }
            }
            else -> super.setProperty(name, args)
        }
    }
}

// Entity

//data class DeviceEntity(val id: String, val name: String, val home: String, val room: String?, val alias: String?)

//data class SensorRecord(val device: String, val sensor: String, val value: Any, val timestamp: Instant)