package quevedo.soares.leandro.blemadeeasy.exceptions

class PeripheralNotObservableException(device: String, characteristic: String) : Exception("Device's ($device) characteristic ($characteristic) does not support property NOTIFY")