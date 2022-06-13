package quevedo.soares.leandro.blemadeeasy.view.singledevice

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.*
import quevedo.soares.leandro.blemadeeasy.databinding.FragmentSingleDeviceBinding
import quevedo.soares.leandro.blemadeeasy.BLE
import quevedo.soares.leandro.blemadeeasy.BluetoothConnection
import quevedo.soares.leandro.blemadeeasy.exceptions.ScanTimeoutException

class SingleDeviceFragmentTest : Fragment() {

	/* Constants */
	private val deviceMacAddress by lazy { this.navArguments.deviceMacAddress }
	private val deviceCharacteristic by lazy { this.navArguments.deviceCharacteristic }

	/* Navigation */
	private val navController by lazy { findNavController() }
	private val navArguments by navArgs<SingleDeviceFragmentArgs>()

	/* Binding */
	private lateinit var binding: FragmentSingleDeviceBinding
	private val isDeviceConnected: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
	private val isLoading: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
	private val currentStatusText: MutableLiveData<String> = MutableLiveData<String>()

	/* Bluetooth variables */
	private var ble: BLE? = null
	private var connection: BluetoothConnection? = null

	/* Misc */
	private var command: Boolean = false

	// region Fragment creation related methods
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		updateStatus(false, "Starting...")

		// If you intend to use the permission handling, you need to instantiate the library in the onCreate method
		setupBluetooth()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = FragmentSingleDeviceBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setupBinding()
		requestPermissions()
	}
	// endregion

	// region Fragment destruction related methods
	override fun onDestroy() {
		super.onDestroy()

		GlobalScope.launch {
			// Closes the connection with the device
			connection?.close()
			connection = null

			// Destroys the ble instance
			ble?.stopScan()
			ble = null
		}
	}
	// endregion

	// region Private utility methods
	private fun showToast(message: String) {
		Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
	}

	private fun updateStatus(loading: Boolean, text: String) {
		currentStatusText.postValue("Current status: $text")
		isLoading.postValue(loading)
	}

	@SuppressLint("MissingPermission")
	private fun requestPermissions() {
		Log.d("MainActivity", "Setting bluetooth manager up...")

		GlobalScope.launch {
			// Checks the bluetooth permissions
			val permissionsGranted = ble?.verifyPermissions(rationaleRequestCallback = { next ->
				showToast("We need the bluetooth permissions!")
				next()
			})
			// Shows UI feedback if the permissions were denied
			if (permissionsGranted == false) {
				showToast("Permissions denied!")
				return@launch
			}

			// Checks the bluetooth adapter state
			val bluetoothActive = ble?.verifyBluetoothAdapterState()
			// Shows UI feedback if the adapter is turned off
			if (bluetoothActive == false) {
				showToast("Bluetooth adapter off!")
				return@launch
			}

			// Checks the location services state
			val locationActive = ble?.verifyLocationState()
			// Shows UI feedback if location services are turned off
			if (locationActive == false) {
				showToast("Location services off!")
				return@launch
			}
		}
	}

	private fun setupBluetooth() {
		Log.d("MainActivity", "Setting bluetooth manager up...")

		// Creates the bluetooth manager instance
		ble = BLE(this).apply {
			verbose = true// Optional variable for debugging purposes
		}
	}

	private fun startConnection() {
		GlobalScope.launch {
			ble?.scanFor(macAddress = "0.0.0.0.0", timeout = 20000)?.let {
				startReadings(it)
			}
		}
	}

	private fun startReadings(device: BluetoothConnection) {
		val pollingRate = 1000L// Define the interval between the readings, in ms
		var lastValue: String? = null
		val characteristic = "0.0.0.0.0"// Define the characteristic to keep track of

		GlobalScope.launch {
			// Reads the characteristic from the device
			val currentValue = device.read(characteristic, Charsets.UTF_8)
			// Check if it has changed
			if (lastValue != currentValue) {
				// TODO: Call a callback or do some action when the value changes

				// Update the lastValue to reflect the changes
				lastValue = currentValue
			}

			// Waits until the next reading
			delay(pollingRate)
		}
	}




	private fun setupBinding() {
		binding.apply {
			// Set the on click listeners
			fsdBtnToggle.setOnClickListener(this@SingleDeviceFragmentTest::onButtonToggleClick)
			fsdBtnConnect.setOnClickListener(this@SingleDeviceFragmentTest::onButtonConnectClick)
			fsdBtnDisconnect.setOnClickListener(this@SingleDeviceFragmentTest::onButtonDisconnectClick)
		}
	}
	// endregion

	// region Event listeners
	private fun onButtonToggleClick(v: View) {
		GlobalScope.launch {
			// Update variables
			updateStatus(true, "Sending data...")

			connection?.let {
				// According to the 'active' boolean flag, send the information to the bluetooth device
				val result = it.write(deviceCharacteristic, if (command) "0" else "1")

				// If the write operation was successful, toggle it
				if (result) {
					// Update variables
					updateStatus(false, "Sent!")
					command = !command
				} else {
					// Update variables
					updateStatus(false, "Information not sent!")
				}
			}
		}
	}

	private fun onButtonConnectClick(v: View) {
		GlobalScope.launch {
			try {
				// Update variables
				updateStatus(true, "Connecting...")

				// Tries to connect with the provided mac address
				ble?.scanFor(macAddress = deviceMacAddress, timeout = 20000)?.let {
					// Set the connection listeners
					connection = it.apply {
						onConnect = {
							// Update variables
							isDeviceConnected.postValue(true)
							updateStatus(false, "Conected!")
						}

						onDisconnect = {
							// Update variables
							isDeviceConnected.postValue(false)
							updateStatus(false, "Disconnected!")
						}
					}

					// Update variables
					isDeviceConnected.postValue(true)
					updateStatus(false, "Conected!")
				}
			} catch (e: ScanTimeoutException) {
				// Update variables
				isDeviceConnected.postValue(false)
				updateStatus(false, "No device found!")
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	private fun onButtonDisconnectClick(v: View) {
		GlobalScope.launch {
			// Update variables
			updateStatus(true, "Disconnecting...")

			// Closes the connection
			connection?.close()
			connection = null

			// Update variables
			updateStatus(false, "Disconnected!")
		}
	}
	// endregion

}