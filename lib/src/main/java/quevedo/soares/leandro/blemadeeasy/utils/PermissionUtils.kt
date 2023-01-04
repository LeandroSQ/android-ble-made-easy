package quevedo.soares.leandro.blemadeeasy.utils

import android.Manifest.*
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

internal object PermissionUtils {

	@RequiresApi(Build.VERSION_CODES.S)
	private val android12Permissions = arrayOf(permission.BLUETOOTH_CONNECT, permission.BLUETOOTH_SCAN)

	@RequiresApi(Build.VERSION_CODES.Q)
	private val android10Permissions = arrayOf(permission.ACCESS_BACKGROUND_LOCATION)

	private val legacyPermissions = arrayOf(permission.BLUETOOTH, permission.BLUETOOTH_ADMIN)
	private val locationPermissions = arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION)

	val permissions by lazy {
		val list = arrayListOf<String>()

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			list.addAll(android12Permissions)
		} else {
			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) list.addAll(android10Permissions)

			list.addAll(legacyPermissions)
			list.addAll(locationPermissions)
		}

		list.toTypedArray()
	}

	fun isBluetoothLowEnergyPresentOnDevice(packageManager: PackageManager): Boolean {
		return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
	}

	fun isBluetoothPresentOnDevice(packageManager: PackageManager): Boolean {
		return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
	}

	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
	private fun isPermissionRequestRequired() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

	private fun isPermissionGranted(context: Context, permission: String): Boolean {
		if (!isPermissionRequestRequired()) return true

		return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
	}

	fun isEveryBluetoothPermissionsGranted(context: Context) = permissions.all { isPermissionGranted(context, it) }

	fun isPermissionRationaleNeeded(context: Activity): Boolean {
		if (!isPermissionRequestRequired()) return false

		return permissions.any { context.shouldShowRequestPermissionRationale(it) }
	}

}