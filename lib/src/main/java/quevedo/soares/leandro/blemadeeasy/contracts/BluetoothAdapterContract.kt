package quevedo.soares.leandro.blemadeeasy.contracts

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/**
 * Custom Contract to determine whether a Bluetooth activation request is fulfilled
 *
 * Resulting in a boolean, true when success
 **/
class BluetoothAdapterContract: ActivityResultContract<Unit, Boolean>() {

	override fun createIntent(context: Context, input: Unit): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

	override fun parseResult(resultCode: Int, intent: Intent?): Boolean = resultCode == Activity.RESULT_OK

}