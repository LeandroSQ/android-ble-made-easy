@file:Suppress("MemberVisibilityCanBePrivate")

package quevedo.soares.leandro.blemadeeasy

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresFeature
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import kotlinx.coroutines.*
import quevedo.soares.leandro.blemadeeasy.contracts.BluetoothAdapterContract
import quevedo.soares.leandro.blemadeeasy.enums.Priority
import quevedo.soares.leandro.blemadeeasy.exceptions.*
import quevedo.soares.leandro.blemadeeasy.models.BLEDevice
import quevedo.soares.leandro.blemadeeasy.typealiases.Callback
import quevedo.soares.leandro.blemadeeasy.typealiases.EmptyCallback
import quevedo.soares.leandro.blemadeeasy.typealiases.PermissionRequestCallback
import quevedo.soares.leandro.blemadeeasy.utils.PermissionUtils
import java.util.*
import kotlin.coroutines.resume

internal const val DEFAULT_TIMEOUT = 10000L
internal const val GATT_133_TIMEOUT = 600L

/** https://cs.android.com/android/platform/superproject/+/master:packages/modules/Bluetooth/system/stack/include/gatt_api.h;l=543;drc=6cf6099dcab87865e33439215e7ea0087e60c9f2#:~:text=%23define%20GATT_MAX_MTU_SIZE%20517 */
internal const val GATT_MAX_MTU = 517

@Suppress("unused")
@RequiresFeature(name = PackageManager.FEATURE_BLUETOOTH_LE, enforcement = "android.content.pm.PackageManager#hasSystemFeature")
class BLE {

	/* Context related variables */
	/** For Jetpack Compose activities use*/
	private var componentActivity: ComponentActivity? = null
	/** For regular activities use */
	private var appCompatActivity: AppCompatActivity? = null
	/** For Fragment use */
	private var fragment: Fragment? = null
	/** The provided context, based on [componentActivity], [appCompatActivity] or [fragment] */
	private var context: Context
	/** Coroutine scope based on the given context provider [componentActivity], [appCompatActivity] or [fragment] */
	private val coroutineScope: CoroutineScope get() = componentActivity?.lifecycleScope ?: appCompatActivity?.lifecycleScope ?: fragment?.lifecycleScope ?: GlobalScope

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
		ScanSettings.Builder().apply {
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
				log("[Legacy] ScanSettings: Using aggressive mode")
				setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
				}

				setReportDelay(0L)
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
				setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
			}
		}.build()
	}
	private var scanCallbackInstance: ScanCallback? = null
	private var scanReceiverInstance: BroadcastReceiver? = null
	private var discoveredDeviceList: ArrayList<BLEDevice> = arrayListOf()
	var isScanRunning = false
		private set

	/* Verbose related variables */
	/**
	 * Indicates whether additional information should be logged
	 **/
	var verbose = false

	// region Constructors
	/**
	 * Instantiates a new Bluetooth scanner instance
	 * Support for Jetpack Compose
	 *
	 * @throws HardwareNotPresentException If no hardware is present on the running device
	 **/
	constructor(componentActivity: ComponentActivity) {
		this.log("Setting up on a ComponentActivity!")
		this.componentActivity = componentActivity
		this.context = componentActivity
		this.setup()
	}

	/**
	 * Instantiates a new Bluetooth scanner instance
	 *
	 * @throws HardwareNotPresentException If no hardware is present on the running device
	 **/
	constructor(activity: AppCompatActivity) {
		this.log("Setting up on an AppCompatActivity!")
		this.appCompatActivity = activity
		this.context = activity
		this.setup()
	}

	/**
	 * Instantiates a new Bluetooth scanner instance
	 *
	 * @throws HardwareNotPresentException If no hardware is present on the running device
	 **/
	constructor(fragment: Fragment) {
		this.log("Setting up on a Fragment!")
		this.fragment = fragment
		this.context = fragment.requireContext()
		this.setup()
	}

	private fun setup() {
		this.verifyBluetoothHardwareFeature()
		this.registerContracts()
		this.setupBluetoothService()
	}
	// endregion

	// region Contracts related methods
	private fun registerContracts() {
		this.log("Registering contracts...")

		this.adapterContract = ContractHandler(BluetoothAdapterContract(), this.componentActivity, this.appCompatActivity, this.fragment)
		this.permissionContract = ContractHandler(RequestMultiplePermissions(), this.componentActivity, this.appCompatActivity, this.fragment)
		this.locationContract = ContractHandler(ActivityResultContracts.StartIntentSenderForResult(), this.componentActivity, this.appCompatActivity, this.fragment)
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

	private fun setupBluetoothService() {
		this.log("Setting up bluetooth service...")
		this.manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		this.adapter = this.manager?.adapter
		this.scanner = this.adapter?.bluetoothLeScanner
	}
	// endregion

	// region Permission related methods
	/**
	 * Checks if the following permissions are granted: [permission.BLUETOOTH], [permission.BLUETOOTH_ADMIN] and [permission.ACCESS_FINE_LOCATION]
	 *
	 * If any of these isn't granted, automatically requests it to the user
	 *
	 * @see verifyPermissions For a variation using coroutines suspended functions
	 *
	 * @param rationaleRequestCallback Called when rationale permission is required, should explain on the UI why the permissions are needed and then re-call this method
	 * @param callback Called with a boolean parameter indicating the permission request state
	 **/
	@RequiresPermission(allOf = [permission.BLUETOOTH, permission.BLUETOOTH_ADMIN, permission.ACCESS_FINE_LOCATION])
	fun verifyPermissionsAsync(rationaleRequestCallback: Callback<EmptyCallback>? = null, callback: PermissionRequestCallback? = null) {
		this.log("Checking App bluetooth permissions...")

		if (PermissionUtils.isEveryBluetoothPermissionsGranted(this.context)) {
			this.log("All permissions granted!")
			callback?.invoke(true)
		} else {
			// Fetch an Activity from the given context providers
			val providedActivity = this.componentActivity ?: this.appCompatActivity ?: this.fragment?.requireActivity()!!
			if (PermissionUtils.isPermissionRationaleNeeded(providedActivity) && rationaleRequestCallback != null) {
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

	/**
	 * Checks if the following permissions are granted: [permission.BLUETOOTH], [permission.BLUETOOTH_ADMIN] and [permission.ACCESS_FINE_LOCATION]
	 *
	 * If any of these isn't granted, automatically requests it to the user
	 *
	 * @see verifyPermissionsAsync For a variation using callbacks
	 *
	 * @param rationaleRequestCallback Called when rationale permission is required, should explain on the UI why the permissions are needed and then re-call this method
	 * @return True when all the permissions are granted
	 **/
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
	/**
	 * Checks if the bluetooth adapter is active
	 *
	 * If not, automatically requests it's activation to the user
	 *
	 * @see verifyBluetoothAdapterState For a variation using coroutines suspended functions
	 *
	 * @param callback Called with a boolean parameter indicating the activation request state
	 **/
	@RequiresPermission(permission.BLUETOOTH_ADMIN)
	fun verifyBluetoothAdapterStateAsync(callback: PermissionRequestCallback? = null) {
		this.log("Checking bluetooth adapter state...")

		if (this.adapter == null || this.adapter?.isEnabled != true) {
			this.log("Bluetooth adapter turned off!")
			launchBluetoothAdapterActivationContract(callback)
		} else callback?.invoke(true)
	}

	/**
	 * Checks if the bluetooth adapter is active
	 *
	 * If not, automatically requests it's activation to the user
	 *
	 * @see verifyBluetoothAdapterState For a variation using callbacks
	 *
	 * @return True when the bluetooth adapter is active
	 **/
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
	/**
	 * Checks if location services are active
	 *
	 * If not, automatically requests it's activation to the user
	 *
	 * @see verifyLocationState For a variation using coroutines suspended functions
	 *
	 * @param callback Called with a boolean parameter indicating the activation request state
	 **/
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

	/**
	 * Checks if location services are active
	 *
	 * If not, automatically requests it's activation to the user
	 *
	 * @see verifyLocationStateAsync For a variation using callbacks
	 *
	 * @return True when the location services are active
	 **/
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
	@SuppressLint("MissingPermission")
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
	@SuppressLint("MissingPermission")
	private fun setupScanCallback(onDiscover: Callback<BLEDevice>? = null, onUpdate: Callback<List<BLEDevice>>? = null, onError: Callback<Int>? = null) {
		fun onDeviceFound(device: BluetoothDevice, rssi: Int, advertisingId: Int = -1) {
			log("Scan result! ${device.name} (${device.address}) ${rssi}dBm")

			discoveredDeviceList.find { it.macAddress == device.address }?.let {
				log("Device update from ${it.rsii} to $rssi at ${device.name}")

				// If the device was already inserted on the list, update it's rsii value
				it.device = device
				if (it.rsii != rssi && rssi != 0) it.rsii = rssi
			} ?: run {
				// If the device was not inserted before, add to the discovered device list
				val bleDevice = BLEDevice(device, rssi, advertisingId)
				discoveredDeviceList.add(bleDevice)
				onDiscover?.invoke(bleDevice)
			}

			onUpdate?.invoke(discoveredDeviceList.toList())
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			// region Android 10 callback
			scanReceiverInstance = object : BroadcastReceiver() {
				override fun onReceive(context: Context?, intent: Intent?) {
					// Ignore other events
					if (intent == null) return

					// Fetch information from the intent extras
					val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
					val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()

					// Ignore non LowEnergy devices
					if (!listOf(BluetoothDevice.DEVICE_TYPE_DUAL, BluetoothDevice.DEVICE_TYPE_LE).contains(device.type)) return

					onDeviceFound(device, rssi, -1)
				}
			}
			context.registerReceiver(scanReceiverInstance, IntentFilter().apply {
				addAction(BluetoothDevice.ACTION_FOUND)
				addAction(BluetoothDevice.ACTION_UUID)
				addAction(BluetoothDevice.ACTION_NAME_CHANGED)
				addAction(BluetoothDevice.ACTION_CLASS_CHANGED)
				addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
				addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
				addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
				addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
				addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) addAction(BluetoothDevice.ACTION_ALIAS_CHANGED)
			})
			// endregion
		}

		// region Legacy callback
		scanCallbackInstance = object : ScanCallback() {

			override fun onScanResult(callbackType: Int, result: ScanResult?) {
				super.onScanResult(callbackType, result)

				// Gets the device from the result
				result?.device?.let { device ->
					val advertisingID = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) result.advertisingSid else -1

					onDeviceFound(device, result.rssi, advertisingID)
				}
			}

			override fun onScanFailed(errorCode: Int) {
				super.onScanFailed(errorCode)

				log("Scan failed! $errorCode")

				// Calls the error callback
				onError?.invoke(errorCode)
			}

		}
		// endregion
	}

	/**
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
	 * @param onUpdate Called whenever the list changes (e.g new device discovered, device name changed etc...)
	 * @param onError Called whenever an error occurs on the scan (Of which will be automatically halted in case of errors)
	 **/
	@SuppressLint("InlinedApi")
	@RequiresPermission(anyOf = [permission.BLUETOOTH_ADMIN, permission.BLUETOOTH_SCAN])
	fun scanAsync(
		filters: List<ScanFilter>? = null,
		settings: ScanSettings? = null,
		duration: Long = DEFAULT_TIMEOUT,
		onFinish: Callback<Array<BLEDevice>>? = null,
		onDiscover: Callback<BLEDevice>? = null,
		onUpdate: Callback<List<BLEDevice>>? = null,
		onError: Callback<Int>? = null
	) {
		this.coroutineScope.launch {
			log("Starting scan...")

			// Instantiate a new ScanCallback object
			setupScanCallback(onDiscover, onUpdate, onError)

			// Clears the discovered device list
			discoveredDeviceList = arrayListOf()

			// Starts the scanning
			isScanRunning = true
			adapter?.apply {
				if (isDiscovering) cancelDiscovery()

				startDiscovery()
			}
			scanner?.startScan(filters, settings ?: defaultScanSettings, scanCallbackInstance)
			scanner?.flushPendingScanResults(scanCallbackInstance)

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

	/**
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
	 **/
	@SuppressLint("InlinedApi")
	@RequiresPermission(anyOf = [permission.BLUETOOTH_ADMIN, permission.BLUETOOTH_SCAN])
	suspend fun scan(filters: List<ScanFilter>? = null, settings: ScanSettings? = null, duration: Long = DEFAULT_TIMEOUT): Array<BLEDevice> {

		scan(filters = null, settings = ScanSettings.Builder().build())

		return suspendCancellableCoroutine { continuation ->
			this.coroutineScope.launch {
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

	/**
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
	 **/
	@SuppressLint("InlinedApi")
	@RequiresPermission(anyOf = [permission.BLUETOOTH_ADMIN, permission.BLUETOOTH_SCAN, permission.BLUETOOTH_CONNECT])
	fun scanForAsync(macAddress: String? = null, service: String? = null, name: String? = null, settings: ScanSettings? = null, priority: Priority = Priority.Balanced, timeout: Long = DEFAULT_TIMEOUT, onFinish: Callback<BluetoothConnection?>? = null, onError: Callback<Int>? = null) {
		this.coroutineScope.launch {
			try {
				onFinish?.invoke(scanFor(macAddress, service, name, settings, priority, timeout))
			} catch (e: ScanTimeoutException) {
				onFinish?.invoke(null)
			} catch (e: ScanFailureException) {
				onError?.invoke(e.code)
			} catch (e: Exception) {
				onError?.invoke(-1)
			}
		}
	}

	/**
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
	 **/
	@SuppressLint("MissingPermission")
	suspend fun scanFor(macAddress: String? = null, service: String? = null, name: String? = null, settings: ScanSettings? = null, priority: Priority = Priority.Balanced, timeout: Long = DEFAULT_TIMEOUT): BluetoothConnection? {
		return suspendCancellableCoroutine { continuation ->
			// Validates the arguments
			if (macAddress == null && service == null && name == null) throw IllegalArgumentException("You need to specify at least one filter!\nBeing them: macAddress, service and name")

			this.coroutineScope.launch {
				// Automatically tries to connect with previously cached devices
				if (macAddress != null) {
					fetchCachedDevice(macAddress)?.let { device ->
						// Stops the current running scan, if any
						stopScan()

						// HACK: Adding a delay after stopping a scan and starting a connection request could solve the 133 in some cases
						delay(GATT_133_TIMEOUT)

						// Check if it is able to connect to the device
						withTimeoutOrNull(timeout) {
							connect(device, priority)
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

						coroutineScope.launch {
							// Stops the current running scan, if any
							stopScan()

							// HACK: Adding a delay after stopping a scan and starting a connection request could solve the 133 in some cases
							delay(GATT_133_TIMEOUT)

							if (continuation.isActive) continuation.resume(connect(bleDevice, priority))
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
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					adapter?.apply {
						if (isDiscovering) cancelDiscovery()

						startDiscovery()
					}
				} else {
					scanner?.startScan(
						filters,
						settings ?: defaultScanSettings,
						scanCallbackInstance
					)
				}

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

	/**
	 * Stops the scan started by [scan]
	 **/
	@SuppressLint("MissingPermission")
	fun stopScan() {
		this.log("Stopping scan...")

		if (!isScanRunning) return

		isScanRunning = false

		// Legacy
		this.scanCallbackInstance?.let {
			this.scanner?.flushPendingScanResults(scanCallbackInstance)
			this.scanner?.stopScan(it)
			this.scanCallbackInstance = null
		}

		// Android 11+
		this.scanReceiverInstance?.let {
			this.adapter?.cancelDiscovery()
			this.context.unregisterReceiver(it)
			this.scanReceiverInstance = null
		}
	}
	// endregion

	// region Utility methods
	private fun log(message: String) {
		if (this.verbose) Log.d("BluetoothMadeEasy", message)
	}
	// endregion

	// region Connection related methods
	/**
	 * Establishes a connection with the specified bluetooth device
	 *
	 * @param device The device to be connected with
	 *
	 * @return A nullable [BluetoothConnection], null when not successful
	 **/
	@RequiresPermission(permission.BLUETOOTH_CONNECT)
	suspend fun connect(device: BluetoothDevice, priority: Priority = Priority.Balanced): BluetoothConnection? {
		return suspendCancellableCoroutine { continuation ->
			this.log("Trying to establish a connection with device ${device.address}...")

			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
				this.log("[Legacy] Waiting ${GATT_133_TIMEOUT}ms before connecting, to prevent GATT_133")
				runBlocking {
					delay(GATT_133_TIMEOUT)
				}
			}

			// Establishes a bluetooth connection to the specified device
			BluetoothConnection(device).also {
				it.verbose = this.verbose
				it.coroutineScope = this.coroutineScope
				it.establish(this.context, priority) { successful ->
					if (successful) {
						log("Connected successfully with ${device.address}!")
						continuation.resume(it)
					} else {
						log("Could not connect with ${device.address}")
						continuation.resume(null)
					}
				}
			}
		}
	}

	/**
	 * Establishes a connection with the specified bluetooth device
	 *
	 * @param device The device to be connected with
	 *
	 * @return A nullable [BluetoothConnection], null when not successful
	 **/
	@SuppressLint("InlinedApi")
	@RequiresPermission(permission.BLUETOOTH_CONNECT)
	suspend fun connect(device: BLEDevice, priority: Priority = Priority.Balanced): BluetoothConnection? = this.connect(device.device, priority)
	// endregion

}