package org.vivlaniv.nexohub

import java.time.Instant

//interface Device {
//    fun
//}

data class DeviceEntity(val id: String, val name: String, val home: String, val room: String?, val alias: String?)

data class SensorRecord(val device: String, val sensor: String, val value: Number, val timestamp: Instant)