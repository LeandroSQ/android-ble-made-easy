package quevedo.soares.leandro.androideasyble

import android.Manifest.permission
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresFeature
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import kotlinx.coroutines.*
import quevedo.soares.leandro.androideasyble.contracts.BluetoothAdapterContract
import quevedo.soares.leandro.androideasyble.exceptions.*
import quevedo.soares.leandro.androideasyble.models.BLEDevice
import quevedo.soares.leandro.androideasyble.typealiases.Callback
import quevedo.soares.leandro.androideasyble.typealiases.EmptyCallback
import quevedo.soares.leandro.androideasyble.typealiases.PermissionRequestCallback
import quevedo.soares.leandro.androideasyble.utils.PermissionUtils
import java.util.*
import kotlin.coroutines.resume

internal const val DEFAULT_TIMEOUT = 10000L
internal const val GATT_133_TIMEOUT = 600L

@Suppress("unused")
@RequiresFeature(name = PackageManager.FEATURE_BLUETOOTH_LE, enforcement = "android.content.pm.PackageManager#hasSystemFeature")
class BLE {

	/* Context related variables */
	private var activity: AppCompatActivity? = null
	private var fragment: Fragment? = null
	private var context: Context

	/* Bluetooth related variables */
	private var manager: BluetoothManager? = null
	private var adapter: BluetoothAdapter? = null
	private var scanner: BluetoothLeScanner? = null

	/* Contracts */
	private lateinit var adapterContract: ContractHandler<Unit, Boolean>
	private lateinit var permissionContract: ContractHandler<Array<String>, Map<String, Boolean>>
	private lateinit var locationContract: ContractHandler<IntentSenderRequest, ActivityResult>

	/* Scan related variables */
	private val defaultScanSettings by lazy {
		val builder = ScanSettings.Builder()
			.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
			.setReportDelay(GATT_133_TIMEOUT)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
				//.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
				.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)


		builder.build()
	}
	private var scanCallbackInstance: ScanCallback? = null
	private var discoveredDeviceList: ArrayList<BLEDevice> = arrayListOf()
	var isScanRunning = false
		private set

	/* Verbose related variables */
	/***
	 * Indicates whether additional information should be logged
	 ***/
	var verbose = false

	// region Constructors
	/***
	 * Instantiates a new Bluetooth scanner instance
	 *
	 * @throws HardwareNotPresentException If no hardware is present on the running device
	 ***/
	constructor(activity: AppCompatActivity) {
		this.log("Setting up on an Activity!")
		this.activity = activity
		this.context = activity
		this.setup()
	}

	/***
	 * Instantiates a new Bluetooth scanner instance
	 *
	 * @throws HardwareNotPresentException If no hardware is present on the running device
	 ***/
	constructor(fragment: Fragment) {
		this.log("Setting up on a Fragment!")
		this.fragment = fragment
		this.context = fragment.requireContext()
		this.setup()
	}

	private fun setup() {
		this.verifyBluetoothHardwareFeature()
		this.registerContracts()

		this.manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		this.adapter = this.manager?.adapter
		this.scanner = this.adapter?.bluetoothLeScanner
	}
	// endregion

	// region Contracts related methods
	private fun registerContracts() {
		this.adapterContract = ContractHandler(BluetoothAdapterContract(), this.activity, this.fragment)
		this.permissionContract = ContractHandler(RequestMultiplePermissions(), this.activity, this.fragment)
		this.locationContract = ContractHandler(ActivityResultContracts.StartIntentSenderForResult(), this.activity, this.fragment)
	}

	private fun launchPermissionRequestContract(callback: PermissionRequestCallback) {
		this.log("Requesting permissions to the user...")

		this.permissionContract.launch(PermissionUtils.permissions) { permissions: Map<String, Boolean> ->
			this.log("Permission request result: $permissions")
			callback(permissions.all { it.value })
		}
	}

	private fun launchBluetoothAdapterActivationContract(callback: PermissionRequestCallback? = null) {
		this.log("Requesting to enable bluetooth adapter to the user...")

		this.adapterContract.launch(Unit) { enabled ->
			this.log("Bluetooth adapter activation request result: $enabled")
			callback?.invoke(enabled)
		}
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
					launchPermissionRequestContract { granted ->
						callback?.invoke(granted)
					}
				}
			} else {
				launchPermissionRequestContract { granted ->
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
			launchBluetoothAdapterActivationContract(callback)
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

	// region Location enabling related methods
	/***
	 * Checks if location services are active
	 *
	 * If not, automatically requests it's activation to the user
	 *
	 * @see verifyLocationState For a variation using coroutines suspended functions
	 *
	 * @param callback Called with a boolean parameter indicating the activation request state
	 ***/
	@RequiresPermission(anyOf = [permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION])
	fun verifyLocationStateAsync(callback: PermissionRequestCallback? = null) {
		this.log("Checking location services state...")

		// Builds a location request
		val locationRequest = LocationRequest.create().apply {
			priority = LocationRequest.PRIORITY_LOW_POWER
		}

		// Builds a location settings request
		val settingsRequest = LocationSettingsRequest.Builder()
			.addLocationRequest(locationRequest)
			.setNeedBle(true)
			.setAlwaysShow(true)
			.build()

		// Execute the location request
		LocationServices.getSettingsClient(context).checkLocationSettings(settingsRequest).apply {
			addOnSuccessListener {
				callback?.invoke(true)
			}

			addOnFailureListener { e ->
				if (e is ResolvableApiException) {
					// If resolution is required from the Google services Api, build an intent to do it and launch the locationContract
					locationContract.launch(IntentSenderRequest.Builder(e.resolution).build()) {
						// Check the contract result
						if (it.resultCode == Activity.RESULT_OK) callback?.invoke(true)
						else callback?.invoke(false)
					}
				} else {
					e.printStackTrace()
					callback?.invoke(false)
				}
			}
		}
	}

	/***
	 * Checks if location services are active
	 *
	 * If not, automatically requests it's activation to the user
	 *
	 * @see verifyLocationStateAsync For a variation using callbacks
	 *
	 * @return True when the location services are active
	 ***/
	@RequiresPermission(anyOf = [permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION])
	suspend fun verifyLocationState(): Boolean {
		return suspendCancellableCoroutine { continuation ->
			verifyLocationStateAsync { status ->
				if (status) continuation.resume(true)
				else continuation.cancel(DisabledAdapterException())
			}
		}
	}
	// endregion

	// region Caching related methods
	private fun fetchCachedDevice(macAddress: String): BluetoothDevice? {
		this.adapter?.getRemoteDevice(macAddress)?.let { device ->
			if (device.type != BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
				log("Fetched device ${device.address} from Android cache!")
				return device
			}
		}

		return null
	}
	// endregion

	// region Device scan related methods
	private fun setupScanCallback(onDiscover: Callback<BLEDevice>? = null, onUpdate: Callback<List<BLEDevice>>? = null, onError: Callback<Int>? = null) {
		scanCallbackInstance = object : ScanCallback() {

			override fun onScanResult(callbackType: Int, result: ScanResult?) {
				super.onScanResult(callbackType, result)

				// Gets the device from the result
				result?.device?.let { device ->
					log("Scan result! ${device.name} (${device.address}) ${result.rssi}dBm")

					// Iterates trough every device already discovered
					var deviceAlreadyInserted = false
					for (i in 0 until discoveredDeviceList.size) {
						val d = discoveredDeviceList[i]

						// If the device was already inserted on the list, update it's rsii value
						if (d.device.address == device.address) {
							d.scanResult = result
							onUpdate?.invoke(discoveredDeviceList.toList())
							deviceAlreadyInserted = true
						}
					}

					// If the device was not inserted before, add to the discovered device list
					if (!deviceAlreadyInserted) {
						val bleDevice = BLEDevice(result)
						discoveredDeviceList.add(bleDevice)
						onDiscover?.invoke(bleDevice)
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
	suspend fun scanAsync(filters: List<ScanFilter>? = null, settings: ScanSettings? = null, duration: Long = DEFAULT_TIMEOUT, onFinish: Callback<Array<BLEDevice>>? = null, onDiscover: Callback<BLEDevice>? = null, onUpdate: Callback<List<BLEDevice>>? = null, onError: Callback<Int>? = null ) {
		GlobalScope.launch {
			log("Starting scan...")

			// Instantiate a new ScanCallback object
			setupScanCallback(onDiscover, onUpdate, onError)

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
	 * @see scan For a variation using callbacks
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
	suspend fun scan(filters: List<ScanFilter>? = null, settings: ScanSettings? = null, duration: Long = DEFAULT_TIMEOUT): Array<BLEDevice> {
		return suspendCancellableCoroutine { continuation ->
			GlobalScope.launch {
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
	 * @see scanFor For a variation using coroutines suspended functions
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
	suspend fun scanForAsync(macAddress: String? = null, service: String? = null, name: String? = null, settings: ScanSettings? = null, timeout: Long = DEFAULT_TIMEOUT, onFinish: Callback<BluetoothConnection?>? = null, onError: Callback<Int>? = null) {
		GlobalScope.launch {
			try {
				onFinish?.invoke(scanFor(macAddress, service, name, settings, timeout))
			} catch (e: ScanTimeoutException) {
				onFinish?.invoke(null)
			} catch (e: ScanFailureException) {
				onError?.invoke(e.code)
			} catch (e: Exception) {
				onError?.invoke(-1)
			}
		}
	}

	/***
	 * Scans for a single bluetooth device and automatically connects with it
	 * Requires at least one filter being them: [macAddress], [service] and [name]
	 *
	 * @see scanForAsync For a variation using callbacks
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
		return suspendCancellableCoroutine { continuation ->
			// Validates the arguments
			if (macAddress == null && service == null && name == null) throw IllegalArgumentException("You need to specify at least one filter!\nBeing them: macAddress, service and name")

			GlobalScope.launch {
				// Automatically tries to connect with previously cached devices
				if (macAddress != null) {
					fetchCachedDevice(macAddress)?.let { device ->
						// Stops the current running scan, if any
						stopScan()

						// HACK: Adding a delay after stopping a scan and starting a connection request could solve the 133 in some cases
						delay(GATT_133_TIMEOUT)

						// Check if it is able to connect to the device
						withTimeoutOrNull(timeout) {
							connect(device)
						}?.let { connection ->
							continuation.resume(connection)
						}
					}
				}

				// Instantiate a new ScanCallback object
				setupScanCallback(
					onDiscover = { bleDevice ->
						// HACK: On devices lower than Marshmallow, run the filtering manually!
						if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
							macAddress?.let {
								if (bleDevice.device.address != it) return@setupScanCallback
							}

							service?.let {
								val parcel = ParcelUuid(UUID.fromString(it))
								if (bleDevice.device.uuids.any { x -> x == parcel }) return@setupScanCallback
							}

							name?.let {
								if (bleDevice.device.name != it) return@setupScanCallback
							}
						}

						GlobalScope.launch {
							// Stops the current running scan, if any
							stopScan()

							// HACK: Adding a delay after stopping a scan and starting a connection request could solve the 133 in some cases
							delay(GATT_133_TIMEOUT)

							if (continuation.isActive) continuation.resume(connect(bleDevice))
						}
					},
					onError = { errorCode ->
						if (continuation.isActive) continuation.cancel(ScanFailureException(errorCode))
					}
				)

				// Clears the discovered device list
				discoveredDeviceList = arrayListOf()

				// Builds the filters
				val filter = ScanFilter.Builder().run {
					macAddress?.let { setDeviceAddress(it) }
					service?.let { setServiceUuid(ParcelUuid(UUID.fromString(it))) }
					name?.let { setDeviceName(it) }
					build()
				}
				// HACK: Ignore the hardware filters on devices lower than MARSHMALLOW 23 (It doesn't work properly)
				val filters = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) arrayListOf(filter) else null

				// Starts the scan
				isScanRunning = true
				scanner?.startScan(
					filters,
					settings ?: defaultScanSettings,
					scanCallbackInstance
				)

				// Only cancels when a timeout is defined
				if (timeout > 0) {
					delay(timeout)

					if (isScanRunning) {
						log("Timeout! No device found in $timeout")
						stopScan()
						if (continuation.isActive) continuation.cancel(ScanTimeoutException())
					}
				}
			}
		}
	}

	/***
	 * Stops the scan started by [scan]
	 ***/
	fun stopScan() {
		this.log("Stopping scan...")

		if (!isScanRunning) return

		isScanRunning = false

		this.scanCallbackInstance?.let {
			this.scanner?.stopScan(it)
			this.scanCallbackInstance = null
		}
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

	/***
	 * Establishes a connection with the specified bluetooth device
	 *
	 * @param device The device to be connected with
	 *
	 * @return A nullable [BluetoothConnection], null when not successful
	 ***/
	suspend fun connect(device: BLEDevice): BluetoothConnection? = this.connect(device.device)
	// endregion

}