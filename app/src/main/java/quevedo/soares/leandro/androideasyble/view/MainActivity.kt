package quevedo.soares.leandro.androideasyble.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
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
	private var initialized: Boolean = false
	private var connection: BluetoothConnection? = null
	private var active: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setupBluetooth()
		setupListeners()
	}

	@SuppressLint("MissingPermission")
	private fun setupBluetooth() {
		Log.d("MainActivity", "Setting bluetooth manager up...")

		// Creates the bluetooth manager instance
		bluetooth = BluetoothMadeEasy(this@MainActivity).apply {
			verbose = true
		}
	}

	@SuppressLint("MissingPermission")
	override fun onStart() {
		super.onStart()

		if (!initialized) {
			GlobalScope.launch {
				// Checks the bluetooth permissions
				val permissionsGranted = bluetooth.verifyPermissions(rationaleRequestCallback = { next ->
					showToast("We need the bluetooth permissions!")
					next()
				})
				// Shows UI feedback if the permissions were denied
				if (!permissionsGranted) {
					showToast("Permissions denied!")
					return@launch
				}

				// Checks the bluetooth adapter state
				val bluetoothActive = bluetooth.verifyBluetoothAdapterState()
				// Shows UI feedback if the adapter is turned off
				if (!bluetoothActive) {
					showToast("Bluetooth adapter off!")
					return@launch
				}
			}
			initialized = true
		}


	}

	private fun setupListeners() {
		btnToggle.setOnClickListener {
			onBtnToggleClick()
		}

		btnConnect.setOnClickListener {
			onBtnConnectClick()
		}

		btnDisconnect.setOnClickListener {
			onBtnDisconnectClick()
		}
	}

	private fun setLoaderVisible(visible: Boolean) {
		runOnUiThread {
			clLoadingLayout.visibility = if (visible) View.VISIBLE else View.GONE
		}
	}

	private fun updateUI() {
		runOnUiThread {
			val visibility = if (connection == null) View.GONE else View.VISIBLE
			val negatedVisibility = if (connection != null) View.GONE else View.VISIBLE
			btnToggle.visibility = visibility
			btnDisconnect.visibility = visibility
			btnConnect.visibility = negatedVisibility
			clLoadingLayout.visibility = View.GONE
		}
	}

	private fun onBtnToggleClick() {
		GlobalScope.launch {
			setLoaderVisible(true)
			connection?.let {
				// According to the 'active' boolean flag, send the information to the bluetooth device
				val result = if (active) {
					it.write(deviceCharacteristic, "0")
				} else {
					it.write(deviceCharacteristic, "1")
				}

				// If the write operation was successful, toggle it
				if (result) active = !active
			}
			setLoaderVisible(false)
		}
	}

	private fun onBtnConnectClick() {
		GlobalScope.launch {
			setLoaderVisible(true)
			bluetooth.scanFor(macAddress = deviceMacAddress)?.let {
				connection = it
				updateUI()
			}
		}
	}

	private fun onBtnDisconnectClick () {
		GlobalScope.launch {
			setLoaderVisible(true)
			connection?.close()
			connection = null
			updateUI()
		}
	}

	private fun showToast(message: String) {
		Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
	}

}