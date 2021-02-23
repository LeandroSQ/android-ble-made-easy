package quevedo.soares.leandro.androideasyble.exceptions

class ScanFailureException(code: Int) : Exception("Scan failed to execute!\nError code: $code")