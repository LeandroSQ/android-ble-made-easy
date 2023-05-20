package quevedo.soares.leandro.blemadeeasy.enums

/**
 * Represents the GATT possible states
 *
 * @property code The code of the state
 * @property description The description of the state
 *
 * @see https://developer.android.com/reference/android/bluetooth/BluetoothProfile#STATE_CONNECTED
 */
enum class GattState(val code: Int, val description: String) {
    Unknown(-1, "Unknown"),
    Disconnected(0, "Disconnected"),
    Connecting(1, "Connecting"),
    Connected(2, "Connected"),
    Disconnecting(3, "Disconnecting");

    override fun toString() = "Code: $code - State.$name - $description"

    companion object {
        /**
         * Returns the GattState for the given code
         *
         * @param code The code to be converted
         * @return The GattState for the given code
         */
        fun fromCode(code: Int) = values().find { it.code == code } ?: Unknown
    }
}