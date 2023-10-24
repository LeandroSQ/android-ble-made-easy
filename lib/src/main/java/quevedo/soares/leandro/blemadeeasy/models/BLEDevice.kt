package quevedo.soares.leandro.blemadeeasy.models

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

class BLEDevice(var device: BluetoothDevice, var rsii: Int = 0, val advertisingId: Int = -1) {

	@get:SuppressLint("MissingPermission")
	val name: String
		get() = device.name ?: ""

	val macAddress: String get () = device.address

	override fun toString(): String = "$name - $macAddress - ${rsii}dBm"

}