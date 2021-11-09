package quevedo.soares.leandro.blemadeeasy.exceptions

class HardwareNotPresentException : Exception("Bluetooth and/or Bluetooth Low Energy feature not found!\nDid you forgot to enable it on manifest.xml?")