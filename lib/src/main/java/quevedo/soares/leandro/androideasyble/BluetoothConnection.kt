package quevedo.soares.leandro.androideasyble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import quevedo.soares.leandro.androideasyble.typealiases.Callback
import quevedo.soares.leandro.androideasyble.typealiases.EmptyCallback
import java.util.*

class BluetoothConnection(private val device: BluetoothDevice) {

	private var genericAttributeProfile: BluetoothGatt? = null
	private var connectionActive: Boolean = false
	private var connectionCallback: Callback<Boolean>? = null

	var onConnect: EmptyCallback? = null
	var onDisconnect: EmptyCallback? = null

	val isActive get() = this.connectionActive

	private fun setupGattCallback(): BluetoothGattCallback {
		return object : BluetoothGattCallback () {

			override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
				super.onConnectionStateChange(gatt, status, newState)

				if (newState == BluetoothProfile.STATE_CONNECTED) {
					// Notifies that the connection has been established
					connectionActive = true
					onConnect?.invoke()

					// Starts the services discovery
					genericAttributeProfile?.discoverServices()
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					// Notifies that the connection has been lost
					connectionActive = false
					onDisconnect?.invoke()
				}
			}

			override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
				super.onServicesDiscovered(gatt, status)

				if (status == BluetoothGatt.GATT_SUCCESS) {
					connectionCallback?.invoke(true)
				} else {
					broadcastError("Error while discovering services at ${device.address}! Status: $status")
					connectionCallback?.invoke(false)
				}
			}

			override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
				super.onCharacteristicRead(gatt, characteristic, status)

				if (status == BluetoothGatt.GATT_SUCCESS) {

				}
			}
		}
	}

	private fun broadcastError(message: String?) {
		message?.let { Log.e("BluetoothConnection", it) }

		close()
	}

	fun write(characteristic: String, message: ByteArray): Boolean {
		val characteristicUuid = UUID.fromString(characteristic)

		genericAttributeProfile?.let {gatt ->
			gatt.services?.forEach { service ->
				service.characteristics.forEach { characteristic ->
					if (characteristic.uuid == characteristicUuid) {
						characteristic.value = message
						return gatt.writeCharacteristic(characteristic)
					}
				}
			}
		}

		Log.e("BluetoothConnection", "Characteristic $characteristic not found on device ${device.address}!")
		return false
	}

	fun write(characteristic: String, message: String): Boolean = this.write(characteristic, message.toByteArray(Charsets.UTF_8))

	fun read(characteristic: String): ByteArray? {
		val characteristicUuid = UUID.fromString(characteristic)

		genericAttributeProfile?.let {gatt ->
			gatt.services?.forEach { service ->
				service.characteristics.forEach { characteristic ->
					if (characteristic.uuid == characteristicUuid) {
						if (gatt.readCharacteristic(characteristic)) return characteristic.value
					}
				}
			}
		}

		Log.e("BluetoothConnection", "Characteristic $characteristic not found on device ${device.address}!")
		return null
	}

	fun establish(context: Context, callback: Callback<Boolean>) {
		this.connectionCallback = {
			callback(it)
			connectionCallback = null
		}

		this.genericAttributeProfile = device.connectGatt(context, false, setupGattCallback())
	}

	fun close() {
		this.connectionCallback?.invoke(false)

		this.genericAttributeProfile?.disconnect()
		this.genericAttributeProfile?.close()
		this.genericAttributeProfile = null
	}


}