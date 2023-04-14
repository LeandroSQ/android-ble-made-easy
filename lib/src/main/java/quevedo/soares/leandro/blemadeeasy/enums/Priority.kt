package quevedo.soares.leandro.blemadeeasy.enums

import android.bluetooth.BluetoothGatt

enum class Priority {
	/** Request low power, reduced data rate connection parameters. */
	LowPower,
	/** Use the connection parameters recommended by the Bluetooth SIG. This is the default value if no connection parameter update is requested. */
	Balanced,
	/** Request a high priority, low latency connection. An application should only request high priority connection parameters to transfer large amounts of data over LE quickly. Once the transfer is complete, the application should request [Balanced] connection parameters to reduce energy use. */
	High;

	internal fun toGattEnum(): Int {
		return when (this) {
			LowPower -> BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER
			Balanced -> BluetoothGatt.CONNECTION_PRIORITY_BALANCED
			High -> BluetoothGatt.CONNECTION_PRIORITY_HIGH
		}
	}
}