package quevedo.soares.leandro.androideasyble.view

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import quevedo.soares.leandro.androideasyble.BluetoothConnection
import quevedo.soares.leandro.androideasyble.BluetoothMadeEasy
import quevedo.soares.leandro.androideasyble.R
import java.util.*

class MainActivity : AppCompatActivity() {

	private val deviceMacAddress = "7C:9E:BD:F4:18:76"
	private val deviceCharacteristic = "4ac8a682-9736-4e5d-932b-e9b31405049c"

	private lateinit var bluetooth: BluetoothMadeEasy
	private var connection: BluetoothConnection? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setupBluetooth()
	}

	private fun setupBluetooth() {
		Log.d("MainActivity", "Setting bluetooth manager up...")

		// Creates the bluetooth manager instance
		this.bluetooth = BluetoothMadeEasy(this)

		handlePermissions()
	}

	@SuppressLint("MissingPermission")
	private fun handlePermissions() {
		Log.d("MainActivity", "Verifying permissions state...")

		// Check if all the bluetooth permissions are granted, and request them if not
		this.bluetooth.verifyPermissionsAsync(

				rationaleRequestCallback = {
					showToast("We need the bluetooth permissions!")
					handlePermissions()
				},

				callback = { permissionsGranted ->
					if (permissionsGranted) {
						// Continue the flow
						handleAdapterState()
					} else {
						showToast("Permissions denied!")
					}
				}

		)
	}

	private fun handleAdapterState() {
		Log.d("MainActivity", "Verifying bluetooth adapter state...")

		// Check if the bluetooth adapter is enabled, and request to enable it if not
		this.bluetooth.verifyBluetoothAdapterStateAsync {
			if (it) {
				startScan()
			} else {
				showToast("You need to enable bluetooth!")
			}
		}
	}

	private fun startScan() {
		Log.d("MainActivity", "Starting ble scan...")

		GlobalScope.launch {
			bluetooth.scanFor(macAddress = deviceMacAddress)?.let {
				connection = it
			}

			/*val filter = ScanFilter.Builder()
					.setDeviceAddress(deviceMacAddress)
//					.setServiceUuid(ParcelUuid(deviceCharacteristic))
					.build()

			val devices = bluetooth.scanSync(arrayListOf(filter))
			Log.d("MainActivity", "Scan found ${devices.size} devices!")*/
		}

		connection?.write(deviceCharacteristic, "0")

	}

	private fun showToast(message: String) {
		Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
	}

}