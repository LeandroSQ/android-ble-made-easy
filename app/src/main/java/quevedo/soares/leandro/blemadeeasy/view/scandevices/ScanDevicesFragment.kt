package quevedo.soares.leandro.blemadeeasy.view.scandevices

import android.annotation.SuppressLint
import android.bluetooth.le.ScanSettings
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
import kotlinx.coroutines.launch
import quevedo.soares.leandro.blemadeeasy.BLE
import quevedo.soares.leandro.blemadeeasy.adapter.BLEDeviceAdapter
import quevedo.soares.leandro.blemadeeasy.databinding.FragmentScanDevicesBinding
import quevedo.soares.leandro.blemadeeasy.exceptions.PermissionsDeniedException
import quevedo.soares.leandro.blemadeeasy.models.BLEDevice

/**
 * This is intended to discover nearby devices
 **/
class ScanDevicesFragment : Fragment() {

	/* Constants */
	private val deviceCharacteristic = "4ac8a682-9736-4e5d-932b-e9b31405049c"
	private val minimumUpdateInterval = 100L

	/* Navigation */
	private val navController by lazy { findNavController() }

	/* Binding */
	private lateinit var binding: FragmentScanDevicesBinding

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
		binding = FragmentScanDevicesBinding.inflate(inflater, container, false)
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

		lifecycleScope.launch {
			try {
				// Checks the bluetooth permissions
				val isPermissionsGranted = ble?.verifyPermissions(rationaleRequestCallback = { next ->
					// Shows UI feedback if the bluetooth permissions are denied by user or rationale is required
					showToast("We need the bluetooth permissions!")
					next()
				})
				// Shows UI feedback if the permissions were denied
				if (isPermissionsGranted == false) {
					showToast("Permissions denied!")
					return@launch
				}

				// Checks the bluetooth adapter state
				val isBluetoothActive = ble?.verifyBluetoothAdapterState()
				// Shows UI feedback if the adapter is turned off
				if (isBluetoothActive == false) {
					showToast("Bluetooth adapter off!")
					return@launch
				}

				// Checks the location services state
				val isLocationActive = ble?.verifyLocationState()
				// Shows UI feedback if location services are turned off
				if (isLocationActive == false) {
					showToast("Location services off!")
					return@launch
				}

				startBluetoothScan()
			} catch(e: PermissionsDeniedException) {
				showToast("Permissions were denied!")
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	private fun setupBluetooth() {
		this.ble = BLE(this).apply {
			verbose = true// Optional variable for debugging purposes
		}
	}

	@SuppressLint("MissingPermission")
	private fun startBluetoothScan() {
		lifecycleScope.launch {
			binding.fmdPbLoader.isVisible = true

			ble?.scanAsync(
				onUpdate = this@ScanDevicesFragment::onScanDevicesUpdate,
				onError = { code ->
					binding.fmdPbLoader.isVisible = false
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
		binding.fmdPbLoader.isVisible = list.isEmpty()

		// Set the recycler view items
		this.adapter.submitList(list)
		this.adapter.notifyDataSetChanged()

		// Store the current time as the last update
		this.lastUpdateTime = System.currentTimeMillis()
		/*if (this.adapter.itemCount != list.size || System.currentTimeMillis() - lastUpdateTime >= minimumUpdateInterval)  {

		}*/
	}

	private fun onItemSelected(position: Int, device: BLEDevice) {
		// Stops the bluetooth scan
		this.ble?.stopScan()

		// Navigates to the next fragment
		this.navController.navigate(
			ScanDevicesFragmentDirections.actionScanDevicesFragmentToSingleDeviceFragment(device.macAddress, deviceCharacteristic)
		)
	}
	// endregion

}