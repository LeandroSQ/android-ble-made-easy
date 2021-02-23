package quevedo.soares.leandro.androideasyble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import quevedo.soares.leandro.androideasyble.typealiases.Callback
import quevedo.soares.leandro.androideasyble.typealiases.EmptyCallback
import java.nio.charset.Charset
import java.util.*

@Suppress("unused")
class BluetoothConnection(private val device: BluetoothDevice) {

	private var genericAttributeProfile: BluetoothGatt? = null
	private var connectionActive: Boolean = false
	private var connectionCallback: Callback<Boolean>? = null

	/***
	 * Called whenever a successful connection is established
	 ***/
	var onConnect: EmptyCallback? = null

	/***
	 * Called whenever a connection is lost
	 ***/
	var onDisconnect: EmptyCallback? = null

	/***
	 * Indicates whether the connection is active
	 ***/
	val isActive get() = this.connectionActive

	// region Private utility related methods
	private fun setupGattCallback(): BluetoothGattCallback {
		return object : BluetoothGattCallback() {

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
					Log.e("BluetoothConnection", "Error while discovering services at ${device.address}! Status: $status")
					close()
					connectionCallback?.invoke(false)
				}
			}

		}
	}

	private fun getCharacteristic(gatt: BluetoothGatt, characteristic: String): BluetoothGattCharacteristic? {
		// Converts the specified string into an UUID
		val characteristicUuid = UUID.fromString(characteristic)

		// Iterates trough every service on the gatt
		gatt.services?.forEach { service ->
			// Iterate trough every characteristic on the service
			service.characteristics.forEach { characteristic ->
				// If matches the uuid, then return it
				if (characteristic.uuid == characteristicUuid) return characteristic
			}
		}

		return null
	}
	// endregion

	// region Value writing related methods
	/***
	 * Performs a write operation on a specific characteristic
	 *
	 * @see [write] For a variant that receives a [String] value
	 *
	 * @param characteristic The uuid of the target characteristic
	 *
	 * @return True when successfully written the specified value
	 ***/
	fun write(characteristic: String, message: ByteArray): Boolean {
		val characteristicUuid = UUID.fromString(characteristic)

		genericAttributeProfile?.let { gatt ->
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

	/***
	 * Performs a write operation on a specific characteristic
	 *
	 * @see [write] For a variant that receives a [ByteArray] value
	 *
	 * @param characteristic The uuid of the target characteristic
	 *
	 * @return True when successfully written the specified value
	 ***/
	fun write(characteristic: String, message: String, charset: Charset = Charsets.UTF_8): Boolean = this.write(characteristic, message.toByteArray(charset))
	// endregion

	// region Value reading related methods
	/***
	 * Performs a read operation on a specific characteristic
	 *
	 * @see [read] For a variant that returns a [String] value
	 *
	 * @param characteristic The uuid of the target characteristic
	 *
	 * @return A nullable [ByteArray], null when failed to read
	 ***/
	fun read(characteristic: String): ByteArray? {
		// Null safe let of the generic attribute profile
		genericAttributeProfile?.let { gatt ->
			// Searches for the characteristic
			val c = this.getCharacteristic(gatt, characteristic)
			if (c != null) {
				// Tries to read its value, if successful return it
				if (gatt.readCharacteristic(c)) return c.value
				else Log.e("BluetoothConnection", "Failed to read characteristic $characteristic on device ${device.address}")
			} else {
				Log.e("BluetoothConnection", "Characteristic $characteristic not found on device ${device.address}!")
			}
		}

		return null
	}

	/***
	 * Performs a read operation on a specific characteristic
	 *
	 * @see [read] For a variant that returns a [ByteArray] value
	 *
	 * @param characteristic The uuid of the target characteristic
	 *
	 * @return A nullable [String], null when failed to read
	 ***/
	fun read(characteristic: String, charset: Charset = Charsets.UTF_8): String? = this.read(characteristic)?.let { String(it, charset) }
	// endregion

	// region Connection handling related methods
	internal fun establish(context: Context, callback: Callback<Boolean>) {
		this.connectionCallback = {
			callback(it)
			connectionCallback = null
		}

		this.genericAttributeProfile = device.connectGatt(context, false, setupGattCallback())
	}

	/***
	 * Closes the connection
	 ***/
	fun close() {
		this.connectionCallback?.invoke(false)

		this.genericAttributeProfile?.disconnect()
		this.genericAttributeProfile?.close()
		this.genericAttributeProfile = null
	}
	// endregion

}