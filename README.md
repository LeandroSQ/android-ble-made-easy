
# Android BLE Made Easy
An easy to use, kotlin friendly BLE library for Android.

[![](https://jitpack.io/v/LeandroSQ/android-ble-made-easy.svg)](https://jitpack.io/#LeandroSQ/android-ble-made-easy) [![API](https://img.shields.io/badge/API-21%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=21)

## Installing
- **Step 1.** Add the JitPack repository to your **project gradle file**
```groovy
allprojects {
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

    implementation 'com.github.LeandroSQ:android-ble-made-easy:v1.5.0'

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

<!-- These 2 bellow, only if you are targeting Android 10+ -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
<uses-feature android:name="android.hardware.bluetooth" android:required="true"/>
```

And you are ready to go!

---

## Core features

### Lifecycle

The library accepts being used both in an Activity and a Fragment!

```kotlin
val ble = BLE(activity = this)
// or
val ble = BLE(fragment = this)
```

### Automatic handle permissions

The library requests the permissions for you.

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

Coroutines:
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

### Automatic turn on the Bluetooth adapter

The library requests the Bluetooth hardware to be activated whenever it is off.

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

Coroutines:
```kotlin
GlobalScope.launch {
    if (ble.verifyBluetoothAdapterState()) {
        // Continue your code...
    } else {
        // Include your code to show an Alert or UI indicating that the Bluetooth adapter is required to be on in order to your project work
    }
}
```

### Automatic turn on Location services

The library requests Location services to be activated whenever it is off.

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

Coroutines:
```kotlin
GlobalScope.launch {
    if (ble.verifyLocationState()) {
        // Continue your code...
    } else {
        // Include your code to show an Alert or UI indicating that Location is required to be on in order to your project work
    }
}
```

### Asynchronous and Coroutines

You can both use the library with callbacks and with coroutines suspended functions
The callback functions having the 'async' suffix.
And requiring a HOF callback as a parameter .

Handling the bluetooth connections with graceful connection shutdown, in another words, waits for current running operations (Read and Write) to be finished before closing the connection

### JetPack Contracts Ready!

The library uses the new JetPack contracts API to automatically handle permissions and adapter activation for you.

### Compatible with older API levels

Theoretically compatible all the way down to API 18, but made targeting API 21+.

### Well documented!

All the functions and variables you're gonna be using are very well documented with KotlinDOC.
So you can get autocompletion information on Android Studio.
But if you want to take a look without installing it... You can take a look on the [dokka generated documentation](lib/build/dokka/gfm/lib/index.md)

[![.github/workflows/wiki.yml](https://github.com/LeandroSQ/android-ble-made-easy/actions/workflows/wiki.yml/badge.svg)](https://github.com/LeandroSQ/android-ble-made-easy/actions/workflows/wiki.yml)

### Both low level bytes and String conversion

The library gives you the option to receive and send raw Bytes if you want.
But also you can let it encode and decode your strings automatically.

### Automatically handles the pain of Known issues

Take for instance [Issue 183108](https://code.google.com/p/android/issues/detail?id=183108) where Lollipop devices will not work properly without a workaround to handle the connection.

Or the well-known [BLE 133](https://github.com/android/connectivity-samples/issues/18) error! The nightmare of everyone that already worked with BLE on Android, this library has a compilation of techniques being used to get around it


## Usage

After instantiating the `BLE` class...

### Fast scan for a specific device

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
Coroutines:
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

### Scanning BLE devices

Asynchronous:
```kotlin
ble.scanAsync(
    duration = 10000,
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

Coroutines:
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
    onFinish = { devices ->
        // Continue with your code handling all the devices found
    },
    onError = { errorCode ->
        // Show an Alert or UI with your preferred error message
    }
)

// Stops your scan manually
ble.stopScan()
```

### Manually connecting to a discovered device

After a successful scan, you'll have your Bluetooth device, now it is time to connect with it!
```kotlin
ble.connect(device)?.let { connection ->
    // Continue with your code
    val value = connection.read("00000000-0000-0000-0000-000000000000")
    connection.write("00000000-0000-0000-0000-000000000000", "0")
    connection.close()
}
```

_Made With <3 by Leandro Quevedo_
