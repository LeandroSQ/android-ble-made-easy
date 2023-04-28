package quevedo.soares.leandro.blemadeeasy.enums

/**
 * Represents the GATT possible status codes
 *
 * @property code The code of the status
 * @property description The description of the status
 *
 * @see https://cs.android.com/android/platform/superproject/+/master:packages/modules/Bluetooth/system/stack/include/gatt_api.h;l=543;drc=6cf6099dcab87865e33439215e7ea0087e60c9f2#:~:text=/*%20Success%20code%20and%20error%20codes%20*/
 */
enum class GattStatus(val code: Int, val description: String) {
    Unknown(-1, "Unknown"),
    InvalidHandle(1, "Invalid handle"),
    ReadNotPermitted(2, "Read not permitted"),
    WriteNotPermitted(3, "Write not permitted"),
    InvalidPdu(4, "Invalid PDU"),
    InsufficientAuthentication(5, "Insufficient authentication"),
    RequestNotSupported(6, "Request not supported"),
    InvalidOffset(7, "Invalid offset"),
    InsufficientAuthorization(8, "Insufficient authorization"),
    PrepareQueueFull(9, "Prepare queue full"),
    AttributeNotFound(10, "Attribute not found"),
    AttributeNotLong(11, "Attribute not long"),
    InsufficientEncryptionKeySize(12, "Insufficient encryption key size"),
    InvalidAttributeValueLength(13, "Invalid attribute value length"),
    UnlikelyError(14, "Unlikely error"),
    InsufficientEncryption(15, "Insufficient encryption"),
    UnsupportedGroupType(16, "Unsupported group type"),
    InsufficientResources(17, "Insufficient resources"),
    DatabaseOutOfSync(18, "Database out of sync"),
    NoResources(128, "No resources"),
    InternalError(129, "Internal error"),
    WrongState(130, "Wrong state"),
    DbFull(131, "Database full"),
    Busy(132, "Busy"),
    Error(133, "Error"),
    CmdStarted(134, "Command started"),
    IllegalParameter(135, "Illegal parameter"),
    Pending(136, "Pending"),
    AuthFail(137, "Authentication failure"),
    More(138, "More"),
    InvalidConfiguration(139, "Invalid configuration"),
    ServiceStarted(140, "Service started"),
    EncryptedNoMitm(141, "Encrypted no MITM"),
    NotEncrypted(142, "Not encrypted"),
    Congested(143, "Congested"),
    CccCfgError(253, "CCC config error"),
    PrcInProgress(254, "Procedure in progress"),
    ValueOutOfRange(255, "Value out of range");
    ConnectionCancel(256, "Connection Cancelled"),
    Failure(257, "Failure");

    companion object {
        /**
         * Returns the GattStatus for the given code
         *
         * @param code The code to be converted
         * @return The GattStatus for the given code
         */
        fun fromCode(code: Int): GattStatus = values().find { it.code == code } ?? Unknown
    }
}