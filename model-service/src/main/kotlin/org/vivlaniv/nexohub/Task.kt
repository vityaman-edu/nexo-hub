package org.vivlaniv.nexohub

import kotlinx.serialization.Serializable
import java.util.*

sealed class Task {
    abstract val id: String
}
sealed class TaskResult {
    abstract val tid: String
}


@Serializable
data class GetDevicesTask(
    override val id: String = UUID.randomUUID().toString(),
    val user: String
) : Task()

@Serializable
data class GetDevicesTaskResult(
    override val tid: String,
    val devices: List<DeviceInfo>
) : TaskResult()


@Serializable
data class GetDevicesPropertiesTask(
    override val id: String = UUID.randomUUID().toString(),
    val user: String
) : Task()

@Serializable
data class GetDevicesPropertiesTaskResult(
    override val tid: String,
    val properties: Map<String, List<PropertyInfo>>
) : TaskResult()


@Serializable
data class GetDevicePropertiesTask(
    override val id: String = UUID.randomUUID().toString(),
    val user: String,
    val device: String
) : Task()

@Serializable
data class GetDevicePropertiesTaskResult(
    override val tid: String,
    val properties: List<PropertyInfo>
) : TaskResult()


@Serializable
data class SetDevicePropertyTask(
    override val id: String = UUID.randomUUID().toString(),
    val user: String,
    val device: String,
    val name: String,
    val value: Int
) : Task()

@Serializable
data class SetDevicePropertyTaskResult(
    override val tid: String
) : TaskResult()


@Serializable
data class SignalDeviceTask(
    override val id: String = UUID.randomUUID().toString(),
    val user: String,
    val device: String,
    val name: String,
    val args: List<Int>
) : Task()

@Serializable
data class SignalDeviceTaskResult(
    override val tid: String
) : TaskResult()

