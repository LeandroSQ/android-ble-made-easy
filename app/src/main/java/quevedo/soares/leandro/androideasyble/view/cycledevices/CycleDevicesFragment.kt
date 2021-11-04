package quevedo.soares.leandro.androideasyble.view.cycledevices

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.*
import quevedo.soares.leandro.androideasyble.BLE
import quevedo.soares.leandro.androideasyble.BluetoothConnection
import quevedo.soares.leandro.androideasyble.R
import quevedo.soares.leandro.androideasyble.databinding.FragmentSingleDeviceBinding
import quevedo.soares.leandro.androideasyble.exceptions.ScanTimeoutException

/**
 * This is intended for testing the library and for testing purposes only
 **/
class CycleDevicesFragment : Fragment() {

	// DEFAULT    -> 7C:9E:BD:F4:18:76
	// FURADEIRA  -> 7C:9E:BD:F4:3F:C2
	// CHAVETEIRA -> 7C:9E:BD:ED:A7:46
	// CHARACTERISTIC -> "4ac8a682-9736-4e5d-932b-e9b31405049c"

	/* Constants */
	private val deviceCharacteristic = "4ac8a682-9736-4e5d-932b-e9b31405049c"
	private val addresses = arrayListOf("7C:9E:BD:F4:18:76", "7C:9E:BD:F4:3F:C2", "7C:9E:BD:ED:A7:46")
	private val delayTime = 150L

	/* Navigation */
	private val navController by lazy { findNavController() }

	/* Binding */
	private lateinit var binding: FragmentSingleDeviceBinding
	private val isDeviceConnected: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
	private val isLoading: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
	private val currentStatusText: MutableLiveData<String> = MutableLiveData<String>()
	private val elapsedTime: MutableLiveData<String> = MutableLiveData<String>()

	/* Bluetooth variables */
	private var ble: BLE? = null
	private var connection: BluetoothConnection? = null

	/* Misc */
	private var addressPointer = 0
	private var scanStartTime = 0L
	private var command: Boolean = false

	// region Fragment creation related methods
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		updateStatus(false, "Starting...")

		// If you intend to use the permission handling, you need to instantiate the library in the onCreate method
		setupBluetooth()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = DataBindingUtil.inflate(inflater, R.layout.fragment_cycle_devices, container, false)
		binding.lifecycleOwner = viewLifecycleOwner
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

		lifecycleScope.launch {
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

		lifecycleScope.launch {
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

	private fun setupBinding() {
		binding.apply {
			// Bind the live data
			isLoading = this@CycleDevicesFragment.isLoading
			isDeviceConnected = this@CycleDevicesFragment.isDeviceConnected
			currentStatusText = this@CycleDevicesFragment.currentStatusText
			elapsedTime = this@CycleDevicesFragment.elapsedTime

			// Set the on click listeners
			fsdBtnToggle.setOnClickListener(this@CycleDevicesFragment::onButtonToggleClick)
			fsdBtnConnect.setOnClickListener(this@CycleDevicesFragment::onButtonConnectClick)
			fsdBtnDisconnect.setOnClickListener(this@CycleDevicesFragment::onButtonDisconnectClick)
		}
	}

	private fun startChronometer() {
		lifecycleScope.launch {
			scanStartTime = System.currentTimeMillis()
			while (isActive) {
				val start = System.currentTimeMillis()

				val elapsed = start - scanStartTime
				val inputSeconds = (elapsed / 1000L)
				val s = inputSeconds % 60
				val m = (inputSeconds / 60) % 60
				val h = (inputSeconds / (60 * 60)) % 24
				elapsedTime.postValue(String.format("%02d:%02d:%02d", h, m, s))

				val calculationTime = System.currentTimeMillis() - start
				delay(1000L - (calculationTime))
			}
		}
	}
	// endregion

	// region Event listeners
	private fun onButtonToggleClick(v: View) {
		lifecycleScope.launch {
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

					delay(delayTime)
					onButtonDisconnectClick(binding.fsdBtnDisconnect)
				} else {
					// Update variables
					updateStatus(false, "Information not sent!")
				}
			}
		}
	}

	private fun onButtonConnectClick(v: View) {
		lifecycleScope.launch {
			try {
				if (scanStartTime == 0L) startChronometer()

				// Update variables
				updateStatus(true, "Connecting...")

				addressPointer++
				if (addressPointer >= addresses.size) addressPointer = 0

				ble?.scanFor(macAddress = addresses[addressPointer], timeout = 20000)?.let {
					connection = it.apply {
						onConnect = {
							isDeviceConnected.postValue(true)
							// Update variables
							updateStatus(false, "Conected!")
						}

						onDisconnect = {
							isDeviceConnected.postValue(false)
							// Update variables
							updateStatus(false, "Disconnected!")
						}
					}

					isDeviceConnected.postValue(true)
					// Update variables
					updateStatus(false, "Conected!")

					delay(delayTime)
					onButtonToggleClick(binding.fsdBtnToggle)
				} ?: run {
					// Update variables
					updateStatus(false, "Could not connect!")
					isDeviceConnected.postValue(false)

					Log.e("LOGGING", "CONNECTION FAILED ON ${addresses[addressPointer]}")
					delay((delayTime * 1.5).toLong())
					onButtonConnectClick(binding.fsdBtnConnect)
				}
			} catch (e: ScanTimeoutException) {
				// Update variables
				updateStatus(false, "No device found!")
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	private fun onButtonDisconnectClick(v: View) {
		lifecycleScope.launch {
			// Update variables
			updateStatus(true, "Disconnecting...")

			connection?.close()
			connection = null

			// Update variables
			updateStatus(false, "Disconnected!")

			delay(delayTime)
			onButtonConnectClick(binding.fsdBtnConnect)
		}
	}
	// endregion

}