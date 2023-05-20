# BLE Made Easy

BLE on Android is verbose and hard. This library makes it easy to use.

Kotlin-first library providing the simplest way to connect to BLE devices and communicate with them.

[![](https://jitpack.io/v/LeandroSQ/android-ble-made-easy.svg)](https://jitpack.io/#LeandroSQ/android-ble-made-easy) [![API](https://img.shields.io/badge/API-21%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=21) [![](https://img.shields.io/badge/Documentation-online-whitesmoke.svg?style=flat)](https://leandrosq.github.io/android-ble-made-easy/index.html)

## How to install it?

- **Step 1.** Add the JitPack repository to your **project gradle file**

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

- **Step 1.1** Only **if you have the file *settings.gradle*** at your project root folder
  - Add the JitPack repository to your **project settings.gradle file**

    ```groovy
    dependencyResolutionManagement {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
    ```
  - Add the JitPack repository to your **project gradle file**

    ```groovy
    buildscript {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
    ```

- **Step 2.** Add the implementation dependency to your **app gradle file**

```groovy
dependencies {
    ...

    implementation 'com.github.LeandroSQ:android-ble-made-easy:1.8.2'

    ...
}
```

- **Step 3.** Gradle sync

- **Step 4.** Add these permissions to your **manifest.xml file**

```xml
<uses-permission
    android:name="android.permission.BLUETOOTH"
    android:maxSdkVersion="30" />
<uses-permission
    android:name="android.permission.BLUETOOTH_ADMIN"
    android:maxSdkVersion="30" />

<!-- These 2 bellow, only if you are targeting Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"
    tools:targetApi="s" />
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="s" />

<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Only if you are targeting Android 10+ -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<!-- Only if you are targeting Android 10+ and pretend to use BLE in a Foreground or Background Service -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
<uses-feature android:name="android.hardware.bluetooth" android:required="true"/>
```

And you are ready to go!

---

## How to use it?

### Permissions and hardware

The library contains helper functions to handle permission and hardware requirements. You can use them to verify if the user has granted the permissions and if the hardware is available.

#### Permissions request

Asynchronous:

```kotlin
ble.verifyPermissionsAsync(
    rationaleRequestCallback = { next ->
        // Include your code to show an Alert or UI explaining why the permissions are required
        // Calling the function bellow if the user agrees to give the permissions
        next()
    },
    callback = { granted ->
        if (granted) {
            // Continue your code....
        } else {
            // Include your code to show an Alert or UI indicating that the permissions are required
        }
    }
)
```

Or with coroutines:

```kotlin
GlobalScope.launch {
    val granted = ble.verifyPermissions(
        rationaleRequestCallback = { next ->
            // Include your code to show an Alert or UI explaining why the permissions are required
            // Calling the function bellow if the user agrees to give the permissions
            next()
        }
    )

    if (granted) {
        // Continue your code....
    } else {
        // Include your code to show an Alert or UI indicating that the permissions are required
    }
}
```

#### Bluetooth hardware activation

Asynchronous:

```kotlin
ble.verifyBluetoothAdapterStateAsync { active ->
    if (active) {
        // Continue your code...
    } else {
        // Include your code to show an Alert or UI indicating that the Bluetooth adapter is required to be on in order to your project work
    }
}
```

Or with coroutines:

```kotlin
GlobalScope.launch {
    if (ble.verifyBluetoothAdapterState()) {
        // Continue your code...
    } else {
        // Include your code to show an Alert or UI indicating that the Bluetooth adapter is required to be on in order to your project work
    }
}
```

#### Location services activation

Asynchronous:

```kotlin
ble.verifyLocationStateAsync{ active ->
    if (active) {
        // Continue your code...
    } else {
        // Include your code to show an Alert or UI indicating that Location is required to be on in order to your project work
    }
}
```

Or with coroutines:

```kotlin
GlobalScope.launch {
    if (ble.verifyLocationState()) {
        // Continue your code...
    } else {
        // Include your code to show an Alert or UI indicating that Location is required to be on in order to your project work
    }
}
```

### Create a BLE instance

For interacting with the library you need to create a BLE instance. You can do it in 3 different ways:

```kotlin
// For jetpack compose:
val ble = BLE(componentActivity = this)

// Or activities:
val ble = BLE(activity = this)

// Or fragments
val ble = BLE(fragment = this)
```

### Fast scan for specific devices

If you already know the device you wanna connect to, you could use this:

Asynchronous:

```kotlin
ble.scanForAsync(
    // You only need to supply one of these, no need for all of them!
    macAddress = "00:00:00:00",
    name = "ESP32",
    service = "00000000-0000-0000-0000-000000000000",

    onFinish = { connection ->
        if (connection != null) {
            // And you can continue with your code
            it.write("00000000-0000-0000-0000-000000000000", "Testing")
        } else {
            // Show an Alert or UI with your preferred error message about the device not being available
        }
    },

    onError = { errorCode ->
        // Show an Alert or UI with your preferred error message about the error
    }
)

// It is important to keep in mind that every single one of the provided arguments of the function shown above, are optionals! Therefore, you can skip the ones that you don't need.
```

Or with coroutines:

```kotlin
GlobalScope.launch {
    // You can specify filters for your device, being them 'macAddress', 'service' and 'name'
    val connection = ble.scanFor(
        // You only need to supply one of these, no need for all of them!
        macAddress = "00:00:00:00",
        name = "ESP32",
        service = "00000000-0000-0000-0000-000000000000"
    )

    // And it will automatically connect to your device, no need to boilerplate
    if (connection != null) {
        // And you can continue with your code
        it.write("00000000-0000-0000-0000-000000000000", "Testing")
    } else {
        // Show an Alert or UI with your preferred error message about the device not being available
    }
}
```

### Scan for nearby devices

Asynchronous:

```kotlin
ble.scanAsync(
    duration = 10000,

    /* This is optional, if you want to update your interface in realtime */
    onDiscover = { device ->
        // Update your UI with the newest found device, in real time
    },

    onFinish = { devices ->
        // Continue with your code handling all the devices found
    },
    onError = { errorCode ->
        // Show an Alert or UI with your preferred error message
    }
)
```

Or with coroutines:

```kotlin
GlobalScope.launch {
    try {
        // Continue with your code handling all the devices found
        val devices = ble.scan(duration = 10000)
    } catch (e: Exception) {
        // Show an Alert or UI with your preferred error message
    } catch (e: ScanFailureException) {
        // Show an Alert or UI with your preferred error message
    }
}
```

Or you could use the scan method without any timeout, only stopping it manually

```kotlin
ble.scanAsync(
    duration = 0, // Disables the timeout
    onDiscover = { device ->
        // Update your UI with the newest found device, in real time
    },
    onError = { errorCode ->
        // Show an Alert or UI with your preferred error message
    }
)

// Stops your scan manually
ble.stopScan()
```

### Connecting to a discovered device

After a successful scan, you'll have your Bluetooth device to connect to it:

```kotlin
ble.connect(device)?.let { connection ->
    // Continue with your code
    val value = connection.read("00000000-0000-0000-0000-000000000000")
    connection.write("00000000-0000-0000-0000-000000000000", "0")
    connection.close()
}
```

You can also define a priority for the connection, useful for higher priority tasks, to ensure preferential treatment for the connection. The default priority is `Priority.Balanced`, other options are `Priority.High` and `Priority.LowPower`.

```kotlin
ble.connect(device, Priority.High)?.let { connection ->
    // Continue with your code
    val value = connection.read("00000000-0000-0000-0000-000000000000")
    connection.write("00000000-0000-0000-0000-000000000000", "0")
    connection.close()
}
```

### Writing to a device

After a successful scan, you'll have your Bluetooth device

```kotlin
ble.connect(device)?.let { connection ->
connection.write(characteristic = "00000000-0000-0000-0000-000000000000", message = "Hello World", charset = Charsets.UTF_8)
connection.close()
}
```

### Reading from a device

After a successful scan, you'll have your Bluetooth device
There's a catch, reading cannot be done on synchronously, so just like other methods you will have two options read and readAsync

```kotlin
GlobalScope.launch {
    ble.connect(device)?.let { connection ->
        val value = connection.read(characteristic = "00000000-0000-0000-0000-000000000000")
        if (value != null) {
            // Do something with this value
        } else {
            // Show an Alert or UI with your preferred error message
        }
    }
}
```

Or you could use the read method with the 'async' prefix, providing a callback

```kotlin
ble.connect(device)?.let { connection ->
    connection.readAsync(characteristic = "00000000-0000-0000-0000-000000000000") { value
        if (value != null) {
            // Do something with this value
        } else {
            // Show an Alert or UI with your preferred error message
        }
    }
}
```

### Observing changes

There are two ways to observe changes, the first is using the native BLE NOTIFY, which is the preferred option.

```kotlin
// If you want to make use of the NOTIFY functionality
ble.connect(device)?.let { connection ->

    // For watching bytes
    connection.observe(characteristic = "00000000-0000-0000-0000-000000000000") { value: ByteArray ->
        // This will run everytime the characteristic changes it's value
    }

    // For watching strings
    connection.observeString(characteristic = "00000000-0000-0000-0000-000000000000", charset = Charsets.UTF_8) { value: String ->
        // This will run everytime the characteristic changes it's value
    }
}

```

The second way is to manually read the characteristic in a fixed interval and compare with the last value. Which uses more battery, isn't as effective and should only be used when the characteristic doesn't provide the NOTIFY property.
Fortunately the library handles both ways in a similar API.

```kotlin
// If you want to use NOTIFY when available and fallback to the legacy way when it isn't
ble.connect(device)?.let { connection ->
    connection.observe(
        characteristic = "00000000-0000-0000-0000-000000000000",
        owner = viewLifeCycleOwner, // The Lifecycle Owner to attach to
        interval = 1000L // The interval in ms (in this example 1 second)
    ) { value: ByteArray ->
        // This will run everytime the characteristic changes it's value
    }
}
```

### MTU change request

For write operations that require more than the default 23 bytes, you can request a MTU change, by doing the following:
```kotlin
ble.connect(device)?.let { connection ->
    connection.requestMTU(bytes = 64)
    connection.write(characteristic = "00000000-0000-0000-0000-000000000000", message = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)) // Imagine a really long message here :)
    connection.close()
}
```

### Forcing RSSI read

```kotlin
ble.connect(device)?.let { connection ->
    if (connection.readRSSI()) { // This will enqueue a RSSI request read
      // Which will be reflected on 
      Log.d("RSSI", connection.rssi)
    }

}
```

### Sample App

This repository also provides a working [Sample App](https://github.com/LeandroSQ/android-ble-made-easy/tree/master/app/src/main/java/quevedo/soares/leandro/blemadeeasy/view) which you can use as a reference.

You can clone the repo and run it on your device.

---

## Why use it?

### Battle tested

This library is battle tested in production apps, varying from industry, IoT and personal projects.

It uses a compilation of techniques and best practices to handle known issues, for instance the [Issue 183108](https://code.google.com/p/android/issues/detail?id=183108), where Lolipop devices will not work properly without a workaround. Or the well-known [BLE 133](https://github.com/android/connectivity-samples/issues/18) error! **The nightmare of everyone** who has ever worked with BLE on Android.

This library handles all of these issues for you, so you don't have to worry about it.

### Lifecycle

This library is designed to work in Jetpack Compose, AndroidX and Android Support, also on Fragments, Activities and Services.

```kotlin
// For jetpack compose:
val ble = BLE(componentActivity = this)
// For activities:
val ble = BLE(activity = this)
// For fragments
val ble = BLE(fragment = this)
```

### Permissions

This library handles all the permission requests for you, so you don't have to worry about it.

### Hardware activation

The library handles the activation of the Bluetooth adapter hardware and the Location services, when required, so you don't have to worry about it.

### Asynchronous and Coroutines

The library exposes asynchronous and coroutines methods for all the functions, so you can choose the one that fits better to your project.

### Operation queue

All the operations, connections, reads and writes are queued, resulting in a more reliable and predictable behavior. When disconnecting, it will wait for the operations to finish before disconnecting, gracefully.

### Device cache

The library caches the discovered devices, so you can connect to them without having to scan twice.

### Older APIs

The library supports Android 5.0+ (API 21+), so you can use it in your projects.

### Kotlin

From the beginning, this library was designed to be used in Kotlin and for Kotlin projects. Although it is theoretically possible to use it in Java projects, the main focus is on Kotlin.

### Documentation

The library is fully documented, so you can easily understand how it works and how to use it.

You can take a look on the online documentation [here](https://leandrosq.github.io/android-ble-made-easy/index.html).

### Bytes and Strings

The library exposes read/write methods which converts the data to/from bytes and strings, so you don't have to worry about it.

### Observers

The library exposes methods to observe changes in a characteristic, even when the NOTIFY property is not available.

<p align="center">
    <br/>
    <br/>
    <i>
        Made with &nbsp;<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/OOjs_UI_icon_heart-progressive.svg/40px-OOjs_UI_icon_heart-progressive.svg.png?20180609135229" height="12px">&nbsp; by Leandro Quevedo.
    </i>
</p>
