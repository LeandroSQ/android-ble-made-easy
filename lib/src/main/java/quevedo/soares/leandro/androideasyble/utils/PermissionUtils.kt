package quevedo.soares.leandro.androideasyble.utils

import android.Manifest.*
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal object PermissionUtils {

    val permissions = arrayOf(permission.BLUETOOTH, permission.BLUETOOTH_ADMIN, permission.ACCESS_FINE_LOCATION)

    fun isBluetoothLowEnergyPresentOnDevice(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBluetoothPresentOnDevice(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    private fun isPermissionRequestRequired(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (!isPermissionRequestRequired()) return true

        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isEveryBluetoothPermissionsGranted(context: Context): Boolean {
         return this.permissions.all { isPermissionGranted(context, it) }
    }

    fun isPermissionRationaleNeeded(context: Activity): Boolean {
        if (!isPermissionRequestRequired()) return false

        return permissions.any {context.shouldShowRequestPermissionRationale(it) }
    }

}