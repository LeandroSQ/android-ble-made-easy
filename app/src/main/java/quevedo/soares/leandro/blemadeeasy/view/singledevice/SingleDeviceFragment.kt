package quevedo.soares.leandro.blemadeeasy.view.singledevice

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.*
import quevedo.soares.leandro.blemadeeasy.BLE
import quevedo.soares.leandro.blemadeeasy.BluetoothConnection
import quevedo.soares.leandro.blemadeeasy.R
import quevedo.soares.leandro.blemadeeasy.databinding.FragmentSingleDeviceBinding
import quevedo.soares.leandro.blemadeeasy.exceptions.ScanTimeoutException
import java.util.*

/**
 * This is intended to control a single device
 **/
class SingleDeviceFragment : Fragment() {

	/* Constants */
	private val deviceMacAddress by lazy { this.navArguments.deviceMacAddress }
	private val deviceCharacteristic by lazy { this.navArguments.deviceCharacteristic }

	/* Navigation */
	private val navController by lazy { findNavController() }
	private val navArguments by navArgs<SingleDeviceFragmentArgs>()

	/* Binding */
	private lateinit var binding: FragmentSingleDeviceBinding

	/* Bluetooth variables */
	private var ble: BLE? = null
	private var connection: BluetoothConnection? = null

	/* Misc */
	private var command = false
	private var isObserving: Boolean = false

	// region Fragment creation related methods
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

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
		updateStatus(false, "Starting...")
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
		binding.fsdCurrentStatus.text = "Current status: $text"
		binding.fsdClLoader.isVisible = loading
	}

	private fun setDeviceConnectionStatus(isConnected: Boolean) {
		binding.root.post {
			binding.fsdBtnToggle.isVisible = isConnected
			binding.fsdBtnRead.isVisible = isConnected
			binding.fsdBtnDisconnect.isVisible = isConnected
			binding.fsdBtnObserve.isVisible = isConnected
			binding.fsdBtnConnect.isVisible = !isConnected
		}
	}

	@SuppressLint("MissingPermission")
	private fun requestPermissions() = lifecycleScope.launch {
		Log.d("MainActivity", "Setting bluetooth manager up...")

		// Checks the bluetooth permissions
		val permissionsGranted = ble?.verifyPermissions(rationaleRequestCallback = { next ->
			showToast("We need the bluetooth permissions!")
			next()
		})
		if (permissionsGranted == false) {
			// Shows UI feedback if the permissions were denied
			showToast("Permissions denied!")
			return@launch
		}

		// Checks the bluetooth adapter state
		if (ble?.verifyBluetoothAdapterState() == false) {
			// Shows UI feedback if the adapter is turned off
			showToast("Bluetooth adapter off!")
			return@launch
		}

		// Checks the location services state
		if (ble?.verifyLocationState() == false) {
			// Shows UI feedback if location services are turned off
			showToast("Location services off!")
			return@launch
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
			// Set the on click listeners
			fsdBtnRead.setOnClickListener { onButtonReadClick() }
			fsdBtnToggle.setOnClickListener { onButtonToggleClick() }
			fsdBtnObserve.setOnClickListener { onButtonObserveClick() }
			fsdBtnConnect.setOnClickListener { onButtonConnectClick() }
			fsdBtnDisconnect.setOnClickListener { onButtonDisconnectClick() }
		}
	}
	// endregion

	// region Event listeners
	private fun onButtonToggleClick() {
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
				} else {
					// Update variables
					updateStatus(false, "Information not sent!")
				}
			}
		}
	}

	private fun onButtonConnectClick() {
		lifecycleScope.launch {
			try {
				// Update variables
				updateStatus(true, "Connecting...")

				// Tries to connect with the provided mac address
				ble?.scanFor(macAddress = deviceMacAddress, timeout = 20000)?.let {
					onDeviceConnected(it)
				}

				//enable this to request new MTU
				//connection?.requestMTU(512)

			} catch (e: ScanTimeoutException) {
				// Update variables
				setDeviceConnectionStatus(false)
				updateStatus(false, "No device found!")
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	private fun onButtonDisconnectClick() {
		lifecycleScope.launch {
			// Update variables
			updateStatus(true, "Disconnecting...")

			// Closes the connection
			connection?.close()
			connection = null

			// Update variables
			updateStatus(false, "Disconnected!")
			setDeviceConnectionStatus(false)
		}
	}

	private fun onButtonObserveClick() {
		this.connection?.let {
			// If the desired characteristic is not available to be observed, pick the first available one
			// This is not really needed, it is just that this would be nice to have for when using generic BLE devices with this sample app
			var candidates = it.notifiableCharacteristics
			if (candidates.isEmpty()) candidates = it.readableCharacteristics
			if (candidates.contains(deviceCharacteristic)) candidates = arrayListOf(deviceCharacteristic)
			val characteristic = candidates.first()

			// Observe
			if (isObserving) {
				it.stopObserving(characteristic)
			} else {
				it.observeString(characteristic, owner = this.viewLifecycleOwner, interval = 5000L) { new ->
					showToast("Value changed to $new")
				}
			}

			// Update the UI
			this.binding.fsdBtnObserve.setText(if (isObserving) R.string.fragment_single_device_observe_off_btn else R.string.fragment_single_device_observe_on_btn)
			this.isObserving = !isObserving
		}
	}

	private fun onButtonReadClick() {
		lifecycleScope.launch {
			// Update variables
			updateStatus(true, "Requesting read...")

			// If the desired characteristic is not available to be read, pick the first available one
			// This is not really needed, it is just that this would be nice to have for when using generic BLE devices with this sample app
			val characteristic = connection?.readableCharacteristics?.firstOrNull { it == deviceCharacteristic } ?: connection?.readableCharacteristics?.first()
			if (characteristic.isNullOrEmpty()) {
				updateStatus(false, "No valid characteristics...")
				return@launch
			}

			connection?.read(characteristic, Charsets.UTF_8)?.let { response ->
				if (response.isEmpty()) {
					updateStatus(false, "Error while reading")
				} else {
					showToast("Read value: $response")
					updateStatus(false, "Read successful")
				}
			}

			//Read remote connection rssi
			connection?.readRSSI()
		}
	}

	private fun onDeviceConnected(connection: BluetoothConnection) {
		connection.apply {
			this@SingleDeviceFragment.connection = connection

			// Define the on re-connect handler
			onConnect = {
				// Update variables
				setDeviceConnectionStatus(true)
				updateStatus(false, "Connected!")
			}

			// Define the on disconnect handler
			onDisconnect = {
				// Update variables
				setDeviceConnectionStatus(false)
				updateStatus(false, "Disconnected!")
			}

			// Update variables
			setDeviceConnectionStatus(true)
			updateStatus(false, "Connected!")
		}
	}
	// endregion

}