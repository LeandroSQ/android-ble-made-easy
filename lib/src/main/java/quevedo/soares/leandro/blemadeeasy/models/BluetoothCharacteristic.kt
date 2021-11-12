package quevedo.soares.leandro.blemadeeasy.models

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import quevedo.soares.leandro.blemadeeasy.exceptions.PeripheralNotObservableException
import java.util.*

data class BluetoothCharacteristic(
	private val characteristic: BluetoothGattCharacteristic
) {
	val descriptors get() = this.characteristic.descriptors
	val isWritable get() = this.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
	val isReadable get() = this.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
	val isNotifiable get() = this.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
	val uuid get() = this.characteristic.uuid

	fun getDescriptor(characteristic: String): BluetoothGattDescriptor? {
		val c = UUID.fromString(characteristic)
		return this.characteristic.descriptors.find { it.uuid == c }
	}

	fun read(gatt: BluetoothGatt) = if (gatt.readCharacteristic(this.characteristic)) {
		this.characteristic.value
	} else null

	fun write(gatt: BluetoothGatt, value: ByteArray): Boolean {
		this.characteristic.value = value
		return gatt.writeCharacteristic(this.characteristic)
	}

	fun enableNotify(gatt: BluetoothGatt) {
		// If the characteristic does not export the NOTIFY property, throw an exception
		if (!this.isNotifiable) throw PeripheralNotObservableException(gatt.device.address, this.uuid.toString().lowercase())

		this.descriptors.firstOrNull()?.let {
			it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
			gatt.writeDescriptor(it)
		}

		gatt.setCharacteristicNotification(this.characteristic, true)
	}

	fun disableNotify(gatt: BluetoothGatt) {
		// If the characteristic does not export the NOTIFY property, throw an exception
		if (!this.isNotifiable) throw PeripheralNotObservableException(gatt.device.address, this.uuid.toString().lowercase())

		this.descriptors.firstOrNull()?.let {
			it.value = byteArrayOf(0, 0)
			gatt.writeDescriptor(it)
		}

		gatt.setCharacteristicNotification(this.characteristic, false)
	}

	fun checkUuid(uuid: UUID): Boolean {
		if (this.uuid == uuid) return true

		if (this.descriptors.any { it.uuid == uuid }) return true

		return false
	}

}