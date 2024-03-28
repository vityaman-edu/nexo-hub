package org.vivlaniv.nexohub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage

const val TAG = "mobile-app"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val searchDevices = mutableStateListOf<DeviceInfo>()

        val mqttClient = MqttAndroidClient(applicationContext, "tcp://10.0.2.2:1883", MqttClient.generateClientId())
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                Log.e(TAG, "mqtt connection lost", cause)
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                Log.i(TAG, "mqtt arrived $topic ${message.payload.decodeToString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
                Log.i(TAG, "mqtt delivery completed")
            }
        })
        mqttClient.connect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.i(TAG, "mqtt client connection succeeded")
                mqttClient.subscribe("user/+/out", 0) { topic, message ->
                    val match = "\\w+/(?<action>\\w+)/out".toRegex().matchEntire(topic)?.groups
                    if (match == null) {
                        Log.w(TAG, "unexpected topic name $topic")
                        return@subscribe
                    }
                    val action = match["action"]?.value!!
                    val payload = message.payload.decodeToString()
                    when (action) {
                        "search" ->  {
                            val response = Json.decodeFromString<SearchDevicesTaskResult>(payload)
                            searchDevices.clear()
                            searchDevices.addAll(response.devices)
                            Log.i(TAG, "search performed")
                        }
                        else -> Log.w(TAG, "unknown action $action")
                    }
                }
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                Log.e(TAG, "mqtt client connection failed", exception)
            }
        })
        fun sendSearchDevices() {
            Log.i(TAG, "button typed")
            val request = SearchDevicesTask()
            mqttClient.publish("user/search/in", Json.encodeToString(request).encodeToByteArray(), 2, false)
        }
        fun clearSearchDevices() {
            searchDevices.clear()
        }

        setContent {
            Column {
                Row {
                    Button(onClick = { sendSearchDevices() }) {
                        Text(text = "+")
                    }
                    Button(onClick = { clearSearchDevices() }) {
                        Text(text = "-")
                    }
                }
                Text(text = "${searchDevices.toList()}")
            }
        }
    }
}