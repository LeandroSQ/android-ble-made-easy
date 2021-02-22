package quevedo.soares.leandro.androideasyble.typealiases

internal typealias EmptyCallback = () -> Unit

internal typealias Callback <T> = (T) -> Unit

internal typealias PermissionRequestCallback = (granted: Boolean) -> Unit
