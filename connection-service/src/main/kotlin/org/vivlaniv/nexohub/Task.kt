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
data class SearchDevicesTask(
    override val id: String = UUID.randomUUID().toString()
) : Task()

@Serializable
data class SearchDevicesTaskResult(
    override val tid: String,
    val devices: List<DeviceInfo>
) : TaskResult()


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
