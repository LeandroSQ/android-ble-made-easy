package quevedo.soares.leandro.blemadeeasy.exceptions

class ScanFailureException(val code: Int) : Exception("Scan failed to execute!\nError code: $code")