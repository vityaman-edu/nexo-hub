package org.vivlaniv.nexohub

import kotlinx.serialization.Serializable

@Serializable
data class PropertyInfo(val name: String, val readOnly: Boolean, val value: Int)
@Serializable
data class SignalInfo(val name: String, val args: List<String>)
@Serializable
data class DeviceInfo(val id: String, val properties: List<PropertyInfo>, val signals: List<SignalInfo>)
