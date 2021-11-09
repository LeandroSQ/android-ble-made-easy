package quevedo.soares.leandro.blemadeeasy

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import quevedo.soares.leandro.blemadeeasy.exceptions.ConnectionClosingException
import quevedo.soares.leandro.blemadeeasy.typealiases.Callback
import quevedo.soares.leandro.blemadeeasy.typealiases.EmptyCallback
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

typealias OnCharacteristicValueChangeCallback<T> = (old: T, new: T) -> Unit

@Suppress("unused")
class BluetoothConnection internal constructor(private val device: BluetoothDevice) {

	/* Bluetooth */
	private var genericAttributeProfile: BluetoothGatt? = null
	private var closingConnection: Boolean = false
	private var connectionActive: Boolean = false

	/* Callbacks */
	private var connectionCallback: Callback<Boolean>? = null

	/* Misc */
	private var operationsInQueue: AtomicInteger = AtomicInteger(0)

	internal var coroutineScope: CoroutineScope? = null

	/**
	 * Indicates whether additional information should be logged
	 **/
	var verbose: Boolean = false

	/**
	 * Called whenever a successful connection is established
	 **/
	var onConnect: EmptyCallback? = null

	/**
	 * Called whenever a connection is lost
	 **/
	var onDisconnect: EmptyCallback? = null

	/**
	 * Indicates whether the connection is active
	 **/
	val isActive get() = this.connectionActive

	/**
	 * Indicates the connection signal strength
	 * <i>Measured in dBm</i>
	 **/
	var rsii: Int = 0
		private set

	private var activeObservers = hashMapOf<String, OnCharacteristicValueChangeCallback<String>>()

	// region Utility related methods
	private fun log(message: String) {
		if (this.verbose) Log.d("BluetoothConnection", message)
	}

	private fun warn(message: String) {
		Log.w("BluetoothConnection", message)
	}

	private fun error(message: String) {
		Log.e("BluetoothConnection", message)
	}

	private fun setupGattCallback(): BluetoothGattCallback {
		return object : BluetoothGattCallback() {

			override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
				super.onConnectionStateChange(gatt, status, newState)

				if (newState == BluetoothProfile.STATE_CONNECTED) {
					log("Device ${device.address} connected!")

					// Notifies that the connection has been established
					if (!connectionActive) {
						onConnect?.invoke()
						connectionActive = true
					}

					// Starts the services discovery
					genericAttributeProfile?.discoverServices()
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					if (status == 133) {// HACK: Multiple reconnections handler
						log("Found 133 connection failure! Reconnecting GATT...")
					} else if (closingConnection) {// HACK: Workaround for Lollipop 21 and 22
						endDisconnection()
					} else {
						// Notifies that the connection has been lost
						if (connectionActive) {
							log("Lost connection with ${device.address}")
							onDisconnect?.invoke()
							connectionActive = false
						}
					}
				}
			}

			override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
				super.onServicesDiscovered(gatt, status)

				coroutineScope?.launch {
					if (status == BluetoothGatt.GATT_SUCCESS) {
						connectionCallback?.invoke(true)
					} else {
						error("Error while discovering services at ${device.address}! Status: $status")
						close()
						connectionCallback?.invoke(false)
					}
				}
			}

			override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
				super.onReadRemoteRssi(gatt, rssi, status)

				// Update the internal rsii variable
				if (status == BluetoothGatt.GATT_SUCCESS) this@BluetoothConnection.rsii = rsii
			}

			override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
				super.onCharacteristicChanged(gatt, characteristic)

				if (characteristic == null) return

				val key = characteristic.uuid.toString()
				activeObservers[key]?.invoke("", characteristic.getStringValue(0))
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

	// region Operation queue related methods
	private fun beginOperation() {
		// Don't allow operations while closing an active connection
		if (closingConnection) throw ConnectionClosingException()

		// Increment the amount of operations
		this.operationsInQueue.incrementAndGet()
	}

	private fun finishOperation() {
		// Decrement the amount of operations, because the current operation has finished
		this.operationsInQueue.decrementAndGet()
	}
	// endregion

	// region Value writing related methods
	/**
	 * Performs a write operation on a specific characteristic
	 *
	 * @see [write] For a variant that receives a [String] value
	 *
	 * @param characteristic The uuid of the target characteristic
	 *
	 * @return True when successfully written the specified value
	 **/
	fun write(characteristic: String, message: ByteArray): Boolean {
		// TODO: Add reliable writing implementation
		this.log("Writing to device ${device.address} (${message.size} bytes)")
		this.beginOperation()

		val characteristicUuid = UUID.fromString(characteristic)

		genericAttributeProfile?.let { gatt ->
			gatt.services?.forEach { service ->
				service.characteristics.forEach { characteristic ->
					if (characteristic.uuid == characteristicUuid) {
						characteristic.value = message
						return gatt.writeCharacteristic(characteristic).also {
							if (!it) log("Could not write to device ${device.address}")
							this.finishOperation()
						}
					}
				}
			}
		}

		error("Characteristic $characteristic not found on device ${device.address}!")
		this.finishOperation()
		return false
	}

	/**
	 * Performs a write operation on a specific characteristic
	 *
	 * @see [write] For a variant that receives a [ByteArray] value
	 *
	 * @param characteristic The uuid of the target characteristic
	 *
	 * @return True when successfully written the specified value
	 **/
	fun write(characteristic: String, message: String, charset: Charset = Charsets.UTF_8): Boolean = this.write(characteristic, message.toByteArray(charset))
	// endregion

	// region Value reading related methods
	/**
	 * Performs a read operation on a specific characteristic
	 *
	 * @see [read] For a variant that returns a [String] value
	 *
	 * @param characteristic The uuid of the target characteristic
	 *
	 * @return A nullable [ByteArray], null when failed to read
	 **/
	fun read(characteristic: String): ByteArray? {
		this.beginOperation()

		// Null safe let of the generic attribute profile
		genericAttributeProfile?.let { gatt ->
			// Searches for the characteristic
			val c = this.getCharacteristic(gatt, characteristic)
			if (c != null) {
				// Tries to read its value, if successful return it
				if (gatt.readCharacteristic(c)) {
					return c.value.also {
						this.finishOperation()
					}
				} else {
					error("Failed to read characteristic $characteristic on device ${device.address}")
				}
			} else {
				error("Characteristic $characteristic not found on device ${device.address}!")
			}
		}

		this.finishOperation()
		return null
	}

	/**
	 * Performs a read operation on a specific characteristic
	 *
	 * @see [read] For a variant that returns a [ByteArray] value
	 *
	 * @param characteristic The uuid of the target characteristic
	 *
	 * @return A nullable [String], null when failed to read
	 **/
	fun read(characteristic: String, charset: Charset = Charsets.UTF_8): String? = this.read(characteristic)?.let { String(it, charset) }
	// endregion
	
	// region Polling
	fun observe(owner: LifecycleOwner, characteristic: String, interval: Long = 1000L, callback: OnCharacteristicValueChangeCallback<String>): String {
		// Generate an id for the observation
		this.activeObservers[characteristic] = callback

		genericAttributeProfile?.let { gatt ->
			val c = this.getCharacteristic(gatt, characteristic)
			if (c != null) {
				c.getDescriptor(UUID.fromString(characteristic))?.let {
					it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
					gatt.writeDescriptor(it)
				}
				gatt.setCharacteristicNotification(c, true)
			} else {
				error("Characteristic $characteristic not found on device ${device.address}!")
			}
		}

		/*this.coroutineScope?.launch {
			var startValue: ByteArray? = null

			// If the lifecycle owner is not destroyed and the currentObserverId is active
			while (owner.lifecycle.currentState != Lifecycle.State.DESTROYED && activeObservers.contains(observerId)) {
				// Saves the start time
				val startTime = System.currentTimeMillis()

				// Fetch the characteristic current value
				val currentValue = read(characteristic)
				// Check if it has changed since last interval
				if (!currentValue.contentEquals(startValue)) {
					// It has changed, update its value and invoke the callback
					callback.invoke(startValue, currentValue)
					startValue = currentValue
				}

				// Saves the end time
				val endTime = System.currentTimeMillis()
				// Calculate the elapsed time and subtract it from the interval time
				val elapsedTime = endTime - startTime
				val sleepTime = (interval - elapsedTime).coerceAtLeast(0L)

				// Updates too close can be harmful to the battery, warn the user
				if (sleepTime <= 0L) warn("The elapsed time ${elapsedTime} between reads exceeds the specified interval of ${interval}ms. You should consider increasing the interval!")

				delay(sleepTime)
			}
		}*/

		return characteristic
	}

	fun stopObserving(characteristic: String) {
		genericAttributeProfile?.let { gatt ->
			val c = this.getCharacteristic(gatt, characteristic)
			if (c != null) {
				gatt.setCharacteristicNotification(c, false)
			} else {
				error("Characteristic $characteristic not found on device ${device.address}!")
			}
		}

		this.activeObservers.remove(characteristic)
	}
	// endregion

	// region Workaround for lollipop
	@SuppressLint("ObsoleteSdkInt")
	private fun isLollipop() = VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && VERSION.SDK_INT <= VERSION_CODES.M

	private fun startDisconnection() {
		try {
			this.closingConnection = true
			this.genericAttributeProfile?.disconnect()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun endDisconnection() {
		log("Disconnected succesfully from ${device.address}!\nClosing connection...")

		try {
			connectionActive = false
			connectionCallback?.invoke(false)
			onDisconnect?.invoke()
			genericAttributeProfile?.close()
			genericAttributeProfile = null
			closingConnection = false
		} catch (e: Exception) {
			log("Ignoring closing connection with ${device.address} exception -> ${e.message}")
		}
	}
	// endregion

	// region Connection handling related methods
	internal fun establish(context: Context, callback: Callback<Boolean>) {
		this.connectionCallback = {
			// Clear the operations queue
			closingConnection = false
			operationsInQueue.set(0)

			// Calls the external connection callback
			callback(it)
			connectionCallback = null
		}

		// HACK: Android M+ requires a transport LE in order to skip the 133 of death status when connecting
		this.genericAttributeProfile = if (VERSION.SDK_INT > VERSION_CODES.M)
			device.connectGatt(context, true, setupGattCallback(), BluetoothDevice.TRANSPORT_LE)
		else
			device.connectGatt(context, false, setupGattCallback())
	}

	/**
	 * Closes the connection
	 **/
	suspend fun close() {
		// Wait for ongoing operations to finish before closing the connection
		// Has a counter of 20 times 500ms each
		// Being 10s in total of timeout
		var counter = 0
		while (operationsInQueue.get() > 0 && counter < 20) {
			log("${operationsInQueue.get()} operations in queue! Waiting for 500ms (${counter * 500}ms elapsed)")
			delay(500)
			counter++
		}

		// HACK: Workaround for Lollipop 21 and 22
		startDisconnection()
	}
	// endregion

}