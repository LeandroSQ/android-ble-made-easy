package quevedo.soares.leandro.androideasyble.exceptions

class HardwareNotPresentException : Exception("Bluetooth and/or Bluetooth Low Energy feature not found!\nDid you forgot to enable it on manifest.xml?")