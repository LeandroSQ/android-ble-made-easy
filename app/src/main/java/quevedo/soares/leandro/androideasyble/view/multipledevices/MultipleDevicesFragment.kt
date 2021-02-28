package quevedo.soares.leandro.androideasyble.view.multipledevices

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import quevedo.soares.leandro.androideasyble.BLE
import quevedo.soares.leandro.androideasyble.R
import quevedo.soares.leandro.androideasyble.adapter.BLEDeviceAdapter
import quevedo.soares.leandro.androideasyble.databinding.FragmentMultipleDevicesBinding
import quevedo.soares.leandro.androideasyble.models.BLEDevice

class MultipleDevicesFragment : Fragment() {

	/* Constants */
	private val deviceCharacteristic = "4ac8a682-9736-4e5d-932b-e9b31405049c"
	private val minimumUpdateInterval = 100L

	/* Navigation */
	private val navController by lazy { findNavController() }

	/* Binding */
	private lateinit var binding: FragmentMultipleDevicesBinding

	/* RecyclerView */
	private lateinit var adapter: BLEDeviceAdapter
	private var lastUpdateTime: Long = -1L

	/* Bluetooth */
	private var ble: BLE? = null

	// region Fragment creation related methods
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.setupBluetooth()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = DataBindingUtil.inflate(inflater, R.layout.fragment_multiple_devices, container, false)
		binding.lifecycleOwner = viewLifecycleOwner
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Initialize the adapter
		this.adapter = BLEDeviceAdapter(this.binding.fmdRvItems, this::onItemSelected)
	}

	override fun onStart() {
		super.onStart()

		if (this.ble?.isScanRunning != true) requestPermissions()
	}
	// endregion

	// region Fragment destruction related methods
	override fun onDestroy() {
		super.onDestroy()

		this.ble?.stopScan()
		this.ble = null
	}
	// endregion

	// region Private utility methods
	private fun showToast(message: String) {
		Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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

			startBluetoothScan()
		}
	}

	private fun setupBluetooth() {
		this.ble = BLE(this).apply {
			verbose = true// Optional variable for debugging purposes
		}
	}

	private fun startBluetoothScan() {
		GlobalScope.launch {
			ble?.scanAsync(
				onUpdate = this@MultipleDevicesFragment::onScanDevicesUpdate,
				onError = { code ->
					showToast("Error code ${code}!")
				},
				duration = 0
			)
		}
	}
	// endregion

	// region Event handlers
	private fun onScanDevicesUpdate(list: List<BLEDevice>) {
		// Only updates the recycler view if the amount of items has changed
		// Or if the minimum update threshold has been elapsed
		if (this.adapter.itemCount != list.size || System.currentTimeMillis() - lastUpdateTime >= minimumUpdateInterval)  {
			// Set the recycler view items
			this.adapter.setItems(list)
			// Store the current time as the last update
			this.lastUpdateTime = System.currentTimeMillis()
		}
	}

	private fun onItemSelected(position: Int, device: BLEDevice) {
		// Stops the bluetooth scan
		this.ble?.stopScan()

		// Navigates to the next fragment
		this.navController.navigate(R.id.action_multipleDevicesFragment_to_singleDeviceFragment, Bundle().apply {
			putString("deviceMacAddress", device.macAddress)
			putString("deviceCharacteristic", deviceCharacteristic)
		})
	}
	// endregion

}