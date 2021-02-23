# Android BLE Made Easy
An easy to use, kotlin friendly BLE library for Android.

[![](https://jitpack.io/v/LeandroSQ/android-ble-made-easy.svg)](https://jitpack.io/#LeandroSQ/android-ble-made-easy)

## Installing
- **Step 1.** Add the JitPackj repository to your **project gradle file**
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

    implementation 'com.github.LeandroSQ:location-made-easy:<VERSION>'

    ...
}
```

- **Step 3.** Replace `<VERSION>` to the desired version, being the latest [![](https://jitpack.io/v/LeandroSQ/android-ble-made-easy.svg)](https://jitpack.io/#LeandroSQ/android-ble-made-easy)

- **Step 4.** Gradle sync and you're ready to go!

---

## Core features

### Lifecycle

The library accepts being used both in an Activity and a Fragment!

```kotlin
val ble = BluetoothMadeEasy(activity = this)
// or
val ble = BluetoothMadeEasy(fragment = this)
```

### Automatic handle permissions

The library requests the permissions for you.

```kotlin
ble.verifyPermissions(
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

### Automatic turn on the Bluetooth adapter

The library requests the Bluetooth hardware to be activated whenever it is off.

```kotlin
ble.verifyBluetoothAdapterState { active ->
    if (active) {
        // Continue your code...
    } else {
        // Include your code to show an Alert or UI indicating that the Bluetooth adapter is required to be on in order to your project work
    }
}
```

### Asynchronous and Coroutines

You can both use the library with callbacks and with coroutines suspended functions
The callback functions having the 'async' suffix.
And requiring a HOF callback as a parameter .

### JetPack Contracts Ready!

The library uses the new JetPack contracts API to automatically handle permissions and adapter activation for you.

### Compatible with older API levels

Theoretically compatible all the way down to API 18, but made targeting API 21+.

### Well documented!

All the functions and variables you're gonna be using are very well documented with KotlinDOC.
So you can get autocompletion information on Android Studio.
But if you want to take a look without installing it... You can take a look on the [dokka generated documentation](lib/build/dokka/gfm/lib/index.md)

### Both low level bytes and String conversion

The library gives you the option to receive and send raw Bytes if you want.
But also you can let it encode and decode your strings automatically.

## Usage

After instantiating the class `BluetoothMadeEasy`...

### Fast scan for a specific device

If you already know the device you wanna connect to, you could use this:
```kotlin
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

### Connecting to a discovered device

After a successful scan, you'll have your Bluetooth device, now it is time to connect with it!
```kotlin
ble.connect(device)?.let { connection ->
    // Continue with your code
    val value = connection.read("00000000-0000-0000-0000-000000000000")
    connection.write("00000000-0000-0000-0000-000000000000", "0")
    connection.close()
}
```

_Made with ❤️ by Leandro Quevedo_