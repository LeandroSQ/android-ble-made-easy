package quevedo.soares.leandro.androideasyble

import android.Manifest.permission
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.launch
import androidx.annotation.RequiresFeature
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import quevedo.soares.leandro.androideasyble.contracts.BluetoothAdapterContract
import quevedo.soares.leandro.androideasyble.exceptions.*
import quevedo.soares.leandro.androideasyble.typealiases.Callback
import quevedo.soares.leandro.androideasyble.typealiases.EmptyCallback
import quevedo.soares.leandro.androideasyble.typealiases.PermissionRequestCallback
import quevedo.soares.leandro.androideasyble.utils.PermissionUtils
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume

internal const val DEFAULT_TIMEOUT = 10000L

@Suppress("unused")
@RequiresFeature(name = PackageManager.FEATURE_BLUETOOTH_LE, enforcement = "android.content.pm.PackageManager#hasSystemFeature")
class BluetoothMadeEasy {

	/* Context related variables */
	private var activity: AppCompatActivity? = null
	private var fragment: Fragment? = null
	private var context: Context

	/* Bluetooth related variables */
	private var manager: BluetoothManager? = null
	private var adapter: BluetoothAdapter? = null
	private var scanner: BluetoothLeScanner? = null

	/* Scan related variables */
	private var isScanRunning = false
	private var scanCallbackInstance: ScanCallback? = null
	private lateinit var discoveredDeviceList: ArrayList<BluetoothDevice>
	private val defaultScanSettings by lazy {
		val builder = ScanSettings.Builder()
			.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
			.setReportDelay(0L)

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
			builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
				.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
				.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)


		builder.build()
	}

	/* Verbose related variables */
	var verbose = false

	// region Constructors
	constructor(activity: AppCompatActivity) {
		this.log("Setting up on an Activity!")
		this.activity = activity
		this.context = activity
		this.setup()
	}

	constructor(fragment: Fragment) {
		this.log("Setting up on a Fragment!")
		this.fragment = fragment
		this.context = fragment.requireContext()
		this.setup()
	}

	private fun setup() {
		this.verifyBluetoothHardwareFeature()

		this.manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		this.adapter = this.manager?.adapter
		this.scanner = this.adapter?.bluetoothLeScanner
	}
	// endregion

	// region Hardware feature related methods
	private fun verifyBluetoothHardwareFeature() {
		this.log("Checking bluetooth hardware on device...")

		context.packageManager.let {
			if (!PermissionUtils.isBluetoothLowEnergyPresentOnDevice(it) || !PermissionUtils.isBluetoothPresentOnDevice(it)) {
				this.log("No bluetooth hardware detected on this device!")
				throw HardwareNotPresentException()
			} else {
				this.log("Detected bluetooth hardware on this device!")
			}
		}
	}
	// endregion

	// region Permission related methods
	private fun requestPermissions(callback: PermissionRequestCallback) {
		this.log("Requesting permissions to the user...")

		if (this.activity != null) {
			// Registers the contract for the activity
			this.activity?.registerForActivityResult(RequestMultiplePermissions()) { permissions ->
				this.log("Permission request result: $permissions")
				callback(permissions.all { it.value })
			}?.launch(PermissionUtils.permissions)
		} else {
			// Registers the contract for the fragment
			this.fragment?.registerForActivityResult(RequestMultiplePermissions()) { permissions ->
				this.log("Permission request result: $permissions")
				callback(permissions.all { it.value })
			}?.launch(PermissionUtils.permissions)
		}
	}

	/***
	 * Checks if the following permissions are granted: [permission.BLUETOOTH], [permission.BLUETOOTH_ADMIN] and [permission.ACCESS_FINE_LOCATION]
	 *
	 * If any of these isn't granted, automatically requests it to the user
	 *
	 * @see verifyPermissions For a variation using coroutines suspended functions
	 *
	 * @param rationaleRequestCallback Called when rationale permission is required, should explain on the UI why the permissions are needed and then re-call this method
	 * @param callback Called with a boolean parameter indicating the permission request state
	 ***/
	@RequiresPermission(allOf = [permission.BLUETOOTH, permission.BLUETOOTH_ADMIN, permission.ACCESS_FINE_LOCATION])
	fun verifyPermissionsAsync(rationaleRequestCallback: Callback<EmptyCallback>? = null, callback: PermissionRequestCallback? = null) {
		this.log("Checking App bluetooth permissions...")

		if (PermissionUtils.isEveryBluetoothPermissionsGranted(this.context)) {
			this.log("All permissions granted!")
			callback?.invoke(true)
		} else {
			if (PermissionUtils.isPermissionRationaleNeeded(this.activity ?: this.fragment?.requireActivity()!!) && rationaleRequestCallback != null) {
				this.log("Permissions denied, requesting permission rationale callback...")
				rationaleRequestCallback {
					requestPermissions { granted ->
						callback?.invoke(granted)
					}
				}
			} else {
				requestPermissions { granted ->
					callback?.invoke(granted)
				}
			}
		}
	}

	/***
	 * Checks if the following permissions are granted: [permission.BLUETOOTH], [permission.BLUETOOTH_ADMIN] and [permission.ACCESS_FINE_LOCATION]
	 *
	 * If any of these isn't granted, automatically requests it to the user
	 *
	 * @see verifyPermissionsAsync For a variation using callbacks
	 *
	 * @param rationaleRequestCallback Called when rationale permission is required, should explain on the UI why the permissions are needed and then re-call this method
	 * @return True when all the permissions are granted
	 ***/
	@RequiresPermission(allOf = [permission.BLUETOOTH, permission.BLUETOOTH_ADMIN, permission.ACCESS_FINE_LOCATION])
	suspend fun verifyPermissions(rationaleRequestCallback: Callback<EmptyCallback>? = null): Boolean {
		return suspendCancellableCoroutine { continuation ->
			this.verifyPermissionsAsync(rationaleRequestCallback) { status ->
				if (status) continuation.resume(true)
				else continuation.cancel(PermissionsDeniedException())
			}
		}
	}
	// endregion

	// region Adapter enabling related methods
	private fun requestBluetoothAdapterEnable(callback: PermissionRequestCallback? = null) {
		this.log("Requesting to enable bluetooth adapter to the user...")

		if (this.activity != null) {
			// Registers the contract for the activity
			this.activity?.registerForActivityResult(BluetoothAdapterContract()) { enabled ->
				this.log("Bluetooth adapter activation request result: $enabled")
				callback?.invoke(enabled)
			}?.launch()
		} else {
			// Registers the contract for the fragment
			this.fragment?.registerForActivityResult(BluetoothAdapterContract()) { enabled ->
				this.log("Bluetooth adapter activation request result: $enabled")
				callback?.invoke(enabled)
			}?.launch()
		}
	}

	/***
	 * Checks if the bluetooth adapter is active
	 *
	 * If not, automatically requests it's activation to the user
	 *
	 * @see verifyBluetoothAdapterState For a variation using coroutines suspended functions
	 *
	 * @param callback Called with a boolean parameter indicating the activation request state
	 ***/
	@RequiresPermission(permission.BLUETOOTH_ADMIN)
	fun verifyBluetoothAdapterStateAsync(callback: PermissionRequestCallback? = null) {
		this.log("Checking bluetooth adapter state...")

		if (this.adapter == null || this.adapter?.isEnabled != true) {
			this.log("Bluetooth adapter turned off!")
			requestBluetoothAdapterEnable(callback)
		} else callback?.invoke(true)
	}

	/***
	 * Checks if the bluetooth adapter is active
	 *
	 * If not, automatically requests it's activation to the user
	 *
	 * @see verifyBluetoothAdapterState For a variation using callbacks
	 *
	 * @return True when the bluetooth adapter is active
	 ***/
	@RequiresPermission(permission.BLUETOOTH_ADMIN)
	suspend fun verifyBluetoothAdapterState(): Boolean {
		return suspendCancellableCoroutine { continuation ->
			this.verifyBluetoothAdapterStateAsync { status ->
				if (status) continuation.resume(true)
				else continuation.cancel(DisabledAdapterException())
			}
		}
	}
	// endregion

	// region Device scan related methods
	private fun setupScanCallback(onDiscover: Callback<BluetoothDevice>? = null, onError: Callback<Int>? = null) {
		scanCallbackInstance = object : ScanCallback() {

			override fun onScanResult(callbackType: Int, result: ScanResult?) {
				super.onScanResult(callbackType, result)

				// Gets the device from the result
				result?.device?.let { device ->
					log("Scan result! ${device.name} (${device.address}) ${result.rssi}dBm")

					// Skip duplicates
					if (discoveredDeviceList.all { it.address != device.address }) {
						discoveredDeviceList.add(device)
						onDiscover?.invoke(device)
					}
				}
			}

			override fun onScanFailed(errorCode: Int) {
				super.onScanFailed(errorCode)

				log("Scan failed! $errorCode")

				// Calls the error callback
				onError?.invoke(errorCode)
			}

		}
	}

	/***
	 * Starts a scan for bluetooth devices
	 * Can be used without [duration] running until [stopScan] is called.
	 *
	 * If only one device is required consider using [scanFor]
	 *
	 * @see scan For a variation using coroutines suspended functions
	 *
	 * @param filters Used to specify attributes of the devices on the scan
	 * @param settings Native object to specify the scan settings (The default setting is only recommended for really fast scans)
	 * @param duration Scan time limit, when exceeded stops the scan <b>(Ignored when less then 0)</b>
	 *
	 * Callbacks:
	 * @param onFinish Called when the scan is finished with an Array of Bluetooth devices found
	 * @param onDiscover Called whenever a new bluetooth device if found (Useful on realtime scans)
	 * @param onError Called whenever an error occurs on the scan (Of which will be automatically halted in case of errors)
	 ***/
	@RequiresPermission(permission.BLUETOOTH_ADMIN)
	suspend fun scanAsync(filters: List<ScanFilter>? = null, settings: ScanSettings? = null, duration: Long = DEFAULT_TIMEOUT, onFinish: Callback<Array<BluetoothDevice>>? = null, onDiscover: Callback<BluetoothDevice>? = null, onError: Callback<Int>? = null) {
		GlobalScope.launch {
			log("Starting scan...")

			// Instantiate a new ScanCallback object
			setupScanCallback(onDiscover, onError)

			// Clears the discovered device list
			discoveredDeviceList = arrayListOf()

			// Starts the scan
			isScanRunning = true
			scanner?.startScan(
				filters,
				settings ?: defaultScanSettings,
				scanCallbackInstance
			)

			// Automatically stops the scan if a duration is specified
			if (duration > 0) {
				log("Scan timeout reached!")

				// Waits for the specified timeout
				delay(duration)

				if (isScanRunning) {
					// Stops the scan
					stopScan()

					// Calls the onFinished callback
					log("Scan finished! ${discoveredDeviceList.size} devices found!")
					onFinish?.invoke(discoveredDeviceList.toTypedArray())
				}
			} else {
				log("Skipped timeout definition on scan!")
			}
		}
	}

	/***
	 * Starts a scan for bluetooth devices
	 * Only runs with a [duration] defined
	 *
	 * If only one device is required consider using [scanFor]
	 *
	 * @see scan For a variation using using callbacks
	 *
	 * @param filters Used to specify attributes of the devices on the scan
	 * @param settings Native object to specify the scan settings (The default setting is only recommended for really fast scans)
	 * @param duration Scan time limit, when exceeded stops the scan <b>(Ignored when less then 0)</b>
	 *
	 * @throws IllegalArgumentException When a duration is not defined
	 * @throws ScanFailureException When an error occurs
	 *
	 * @return An Array of Bluetooth devices found
	 ***/
	@RequiresPermission(permission.BLUETOOTH_ADMIN)
	suspend fun scan(filters: List<ScanFilter>? = null, settings: ScanSettings? = null, duration: Long = DEFAULT_TIMEOUT): Array<BluetoothDevice> {
		return suspendCancellableCoroutine { continuation ->
			runBlocking {
				// Validates the duration
				if (duration <= 0) continuation.cancel(IllegalArgumentException("In order to run a synchronous scan you'll need to specify a duration greater than 0ms!"))

				scanAsync(
					filters,
					settings,
					duration,
					onFinish = {
						continuation.resume(it)
					},
					onError = { errorCode ->
						continuation.cancel(ScanFailureException(errorCode))
					}
				)
			}
		}
	}

	/***
	 * Scans for a single bluetooth device and automatically connects with it
	 * Requires at least one filter being them: [macAddress], [service] and [name]
	 *
	 * Filters:
	 * @param macAddress Optional filter, if provided searches for the specified mac address
	 * @param service Optional filter, if provided searches for the specified service uuid
	 * @param name Optional filter, if provided searches for the specified device name
	 *
	 * @param settings Native object to specify the scan settings (The default setting is only recommended for really fast scans)
	 * @param timeout Scan time limit, when exceeded throws an [ScanTimeoutException]
	 *
	 * @throws ScanTimeoutException When the [timeout] is reached
	 * @throws ScanFailureException When an error occurs
	 *
	 * @return A nullable [BluetoothConnection] instance, when null meaning that the specified device was not found
	 ***/
	suspend fun scanFor(macAddress: String? = null, service: String? = null, name: String? = null, settings: ScanSettings? = null, timeout: Long = DEFAULT_TIMEOUT): BluetoothConnection? {
		// Validates the arguments
		if (macAddress == null && service == null && name == null) throw IllegalArgumentException("You need to specify at least one filter!\nBeing them: macAddress, service and name")

		return suspendCancellableCoroutine { continuation ->
			// Instantiate a new ScanCallback object
			setupScanCallback(
				onDiscover = {
					stopScan()

					runBlocking {
						continuation.resume(connect(it))
					}
				},
				onError = { errorCode ->
					continuation.cancel(ScanFailureException(errorCode))
				}
			)

			// Clears the discovered device list
			discoveredDeviceList = arrayListOf()

			// Builds the filters
			val filter = ScanFilter.Builder()
			macAddress?.let { filter.setDeviceAddress(it) }
			service?.let { filter.setServiceUuid(ParcelUuid(UUID.fromString(it))) }
			name?.let { filter.setDeviceName(it) }

			// Starts the scan
			isScanRunning = true
			scanner?.startScan(
				arrayListOf(filter.build()),
				settings ?: defaultScanSettings,
				scanCallbackInstance
			)

			// Only cancels when a timeout is defined
			if (timeout > 0) {
				runBlocking {
					delay(timeout)

					if (continuation.isActive && isScanRunning) continuation.cancel(ScanTimeoutException())
				}
			}
		}
	}

	/***
	 * Stops the scan started by [scan]
	 ***/
	fun stopScan() {
		this.log("Stopping scan...")

		this.scanCallbackInstance?.let {
			this.scanner?.stopScan(it)
			this.scanCallbackInstance = null
		}

		isScanRunning = false
	}
	// endregion

	// region Utility methods
	private fun log(message: String) {
		if (this.verbose) Log.d("BluetoothMadeEasy", message)
	}
	// endregion

	// region Connection related methods
	/***
	 * Establishes a connection with the specified bluetooth device
	 *
	 * @param device The device to be connected with
	 *
	 * @return A nullable [BluetoothConnection], null when not successful
	 ***/
	suspend fun connect(device: BluetoothDevice): BluetoothConnection? {
		return suspendCancellableCoroutine { continuation ->
			this.log("Trying to establish a conecttion with device ${device.address}...")

			// Establishes a bluetooth connection to the specified device
			val connection = BluetoothConnection(device)
			connection.verbose = this.verbose
			connection.establish(this.context) { successful ->
				if (successful) {
					log("Connected successfully with ${device.address}!")
					continuation.resume(connection)
				} else {
					log("Could not connect with ${device.address}")
					continuation.resume(null)
				}
			}
		}
	}
	// endregion

}