package quevedo.soares.leandro.androideasyble

import android.Manifest
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
import quevedo.soares.leandro.androideasyble.typealiases.Callback
import quevedo.soares.leandro.androideasyble.typealiases.EmptyCallback
import quevedo.soares.leandro.androideasyble.typealiases.PermissionRequestCallback
import quevedo.soares.leandro.androideasyble.utils.PermissionUtils
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume

@RequiresFeature(name = PackageManager.FEATURE_BLUETOOTH_LE, enforcement = "android.content.pm.PackageManager#hasSystemFeature")
class BluetoothMadeEasy {

	private var activity: AppCompatActivity? = null
	private var fragment: Fragment? = null
	private var context: Context

	private var manager: BluetoothManager? = null
	private var adapter: BluetoothAdapter? = null
	private var scanner: BluetoothLeScanner? = null

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

	// region Constructors
	constructor(activity: AppCompatActivity) {
		this.activity = activity
		this.context = activity
		this.setup()
	}

	constructor(fragment: Fragment) {
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
		context.packageManager.let {
			if (!PermissionUtils.isBluetoothLowEnergyPresentOnDevice(it) || !PermissionUtils.isBluetoothPresentOnDevice(it)) {
				throw UnsupportedOperationException("Bluetooth and/or Bluetooth Low Energy feature not found!\nDid you forgot to enable it on manifest.xml?")
			}
		}

	}
	// endregion

	// region Permission related methods
	private fun requestPermissions(callback: PermissionRequestCallback) {
		if (this.activity != null) {
			// Registers the contract for the activity
			this.activity?.registerForActivityResult(RequestMultiplePermissions()) { permissions ->
				callback(permissions.all { it.value })
			}?.launch(PermissionUtils.permissions)
		} else {
			// Registers the contract for the fragment
			this.fragment?.registerForActivityResult(RequestMultiplePermissions()) { permissions ->
				callback(permissions.all { it.value })
			}?.launch(PermissionUtils.permissions)
		}
	}

	@RequiresPermission(allOf = [Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN])
	fun verifyPermissions(rationaleRequestCallback: EmptyCallback? = null, callback: PermissionRequestCallback? = null) {
		if (PermissionUtils.isEveryBluetoothPermissionsGranted(this.context)) {
			callback?.invoke(true)
		} else {
			if (PermissionUtils.isPermissionRationaleNeeded(this.activity ?: this.fragment?.requireActivity()!!) && rationaleRequestCallback != null) {
				rationaleRequestCallback()
			} else {
				requestPermissions { granted ->
					callback?.invoke(granted)
				}
			}
		}
	}
	// endregion

	// region Adapter enabling related methods
	private fun requestBluetoothAdapterEnable(callback: PermissionRequestCallback? = null) {
		if (this.activity != null) {
			// Registers the contract for the activity
			this.activity?.registerForActivityResult(BluetoothAdapterContract()) { enabled ->
				callback?.invoke(enabled)
			}?.launch()
		} else {
			// Registers the contract for the fragment
			this.fragment?.registerForActivityResult(BluetoothAdapterContract()) { enabled ->
				callback?.invoke(enabled)
			}?.launch()
		}
	}

	@RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
	fun verifyBluetoothAdapterState(callback: PermissionRequestCallback? = null) {
		if (this.adapter == null || this.adapter?.isEnabled != true) {
			requestBluetoothAdapterEnable(callback)
		} else callback?.invoke(true)
	}
	// endregion

	// region Device scan related methods
	private fun setupScanCallback(onDiscover: Callback<BluetoothDevice>? = null, onError: Callback<Int>? = null) {
		scanCallbackInstance = object : ScanCallback() {

			override fun onScanResult(callbackType: Int, result: ScanResult?) {
				super.onScanResult(callbackType, result)

				// Gets the device from the result
				result?.device?.let { device ->
					// Skip duplicates
					if (discoveredDeviceList.all { it.address != device.address }) {
						discoveredDeviceList.add(device)
						onDiscover?.invoke(device)
					}
				}
			}

			override fun onScanFailed(errorCode: Int) {
				super.onScanFailed(errorCode)
				// Calls the error callback
				onError?.invoke(errorCode)
			}

		}
	}

	@RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
	suspend fun scanSync(filters: List<ScanFilter>? = null, settings: ScanSettings? = null, duration: Long = 10000): Array<BluetoothDevice> {
		return suspendCancellableCoroutine { continuation ->
			runBlocking {
				// Validates the duration
				if (duration <= 0) continuation.cancel(Exception("In order to run a synchronous scan you'll need to specify a duration greater than 0ms!"))

				scanAsync(
					filters,
					settings,
					duration,
					onFinish = {
						continuation.resume(it)
					},
					onDiscover = {
						Log.d("BluetoothMadeEasy", "I found a new device ${it.name} - ${it.address}")
					},
					onError = { errorCode ->
						Log.e("BluetoothMadeEasy", "Error code: $errorCode")
						continuation.cancel(Exception("Error code: $errorCode"))
					}
				)
			}
		}
	}

	@RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
	suspend fun scanAsync(filters: List<ScanFilter>? = null, settings: ScanSettings? = null, duration: Long = 10000, onFinish: Callback<Array<BluetoothDevice>>? = null, onDiscover: Callback<BluetoothDevice>? = null, onError: Callback<Int>? = null) {
		GlobalScope.launch {
			// Instantiate a new ScanCallback object
			setupScanCallback(onDiscover, onError)

			// Clears the discovered device list
			discoveredDeviceList = arrayListOf()

			// Starts the scan
			scanner?.startScan(
				filters,
				settings ?: defaultScanSettings,
				scanCallbackInstance
			)

			// Automatically stops the scan if a duration is specified
			if (duration > 0) {
				// Waits for the specified timeout
				delay(duration)

				// Stops the scan
				stopScan()
				onFinish?.invoke(discoveredDeviceList.toTypedArray())
			}
		}
	}

	suspend fun scanFor(macAddress: String? = null, service: String? = null, name: String? = null, settings: ScanSettings? = null, timeout: Long = 10000): BluetoothConnection? {
		// Validates the arguments
		if (macAddress == null && service == null && name == null) throw IllegalArgumentException("You need to specify at least one filter!")

		return suspendCancellableCoroutine { continuation ->
			// Instantiate a new ScanCallback object
			setupScanCallback(
				onDiscover = {
					stopScan()

					runBlocking {
						continuation.resume(connect(it))
					}
				},
				onError = {
					continuation.cancel(Exception("Scan failure!\nError code: $it"))
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
			scanner?.startScan(
				arrayListOf(filter.build()),
				settings ?: defaultScanSettings,
				scanCallbackInstance
			)

			// Only cancels when a timeout is defined
			if (timeout > 0) {
				runBlocking {
					delay(timeout)

					if (continuation.isActive) continuation.cancel(TimeoutException())
				}
			}
		}
	}

	fun stopScan() {
		this.scanCallbackInstance?.let {
			this.scanner?.stopScan(it)
			this.scanCallbackInstance = null
		}
	}
	// endregion

	suspend fun connect(device: BluetoothDevice): BluetoothConnection? {
		return suspendCancellableCoroutine { continuation ->
			// Establishes a bluetooth connection to the specified device
			val connection = BluetoothConnection(device)
			connection.establish(this.context) { successful ->
				if (successful)
					continuation.resume(connection)
				else
					continuation.resume(null)
//					continuation.cancel(Exception("Unable to connect to device!"))
			}
		}
	}

}