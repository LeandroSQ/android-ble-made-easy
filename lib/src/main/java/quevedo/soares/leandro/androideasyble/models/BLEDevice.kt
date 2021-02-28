package quevedo.soares.leandro.androideasyble.models

import android.bluetooth.le.ScanResult

class BLEDevice(internal var scanResult: ScanResult) {

	val device get () = scanResult.device

	val name get () = device.name

	val macAddress get () = device.address

	val rsii get () = scanResult.rssi

	val advertisingId get () = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) scanResult.advertisingSid else -1

	override fun toString(): String = "$name - $macAddress - ${rsii}dBm"

}