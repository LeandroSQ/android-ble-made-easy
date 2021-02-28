//[lib](../../index.md)/[quevedo.soares.leandro.androideasyble](../index.md)/[BLE](index.md)/[scanForAsync](scan-for-async.md)



# scanForAsync  
[androidJvm]  
Content  
final [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)[scanForAsync](scan-for-async.md)([String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)[macAddress](scan-for-async.md), [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)[service](scan-for-async.md), [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)[name](scan-for-async.md), [ScanSettings](https://developer.android.com/reference/kotlin/android/bluetooth/le/ScanSettings.html)[settings](scan-for-async.md), [Long](https://docs.oracle.com/javase/8/docs/api/java/lang/Long.html)[timeout](scan-for-async.md), Function1<[BluetoothConnection](../-bluetooth-connection/index.md), [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)>[onFinish](scan-for-async.md), Function1<[Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html), [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)>[onError](scan-for-async.md))  
  
More info  
<ul><li></li></ul>

Scans for a single bluetooth device and automatically connects with it Requires at least one filter being them: macAddress, service and name



#### Return  


A nullable [BluetoothConnection](../-bluetooth-connection/index.md) instance, when null meaning that the specified device was not found



## See also  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.BLE](scan-for.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>For a variation using coroutines suspended functions<br><br><br><br>Filters:<br><br>
  


## Parameters  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a>macAddress| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>Optional filter, if provided searches for the specified mac address<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a>service| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>Optional filter, if provided searches for the specified service uuid<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a>name| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>Optional filter, if provided searches for the specified device name<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a>settings| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>Native object to specify the scan settings (The default setting is only recommended for really fast scans)<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a>timeout| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>Scan time limit, when exceeded throws an [ScanTimeoutException](../../quevedo.soares.leandro.androideasyble.exceptions/-scan-timeout-exception/index.md)<br><br>
  


#### Throws  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.exceptions.ScanTimeoutException](../../quevedo.soares.leandro.androideasyble.exceptions/-scan-timeout-exception/index.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>When the timeout is reached<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.exceptions.ScanFailureException](../../quevedo.soares.leandro.androideasyble.exceptions/-scan-failure-exception/index.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/scanForAsync/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long#kotlin.Function1[quevedo.soares.leandro.androideasyble.BluetoothConnection?,kotlin.Unit]?#kotlin.Function1[kotlin.Int,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>When an error occurs<br><br>
  



