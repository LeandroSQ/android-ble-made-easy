package quevedo.soares.leandro.blemadeeasy.models

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

class BLEDevice(val device: BluetoothDevice, var rsii: Int = 0, val advertisingId: Int = -1) {

	val name: String get () = device.name ?: ""

	val macAddress: String get () = device.address

	override fun toString(): String = "$name - $macAddress - ${rsii}dBm"

}