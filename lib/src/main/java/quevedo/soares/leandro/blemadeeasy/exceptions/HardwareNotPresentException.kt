package quevedo.soares.leandro.blemadeeasy.exceptions

class HardwareNotPresentException : Exception("Bluetooth and/or Bluetooth Low Energy feature not found!\nDid you forgot to enable it on manifest.xml?\n\nIf running on an Emulator, emulators usually do not provide BLE hardware emulation, please try with a physical device.")