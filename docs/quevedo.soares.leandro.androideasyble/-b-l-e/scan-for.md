//[lib](../../index.md)/[quevedo.soares.leandro.androideasyble](../index.md)/[BLE](index.md)/[scanFor](scan-for.md)



# scanFor  
[androidJvm]  
Content  
final [BluetoothConnection](../-bluetooth-connection/index.md)[scanFor](scan-for.md)([String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)[macAddress](scan-for.md), [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)[service](scan-for.md), [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)[name](scan-for.md), [ScanSettings](https://developer.android.com/reference/kotlin/android/bluetooth/le/ScanSettings.html)[settings](scan-for.md), [Long](https://docs.oracle.com/javase/8/docs/api/java/lang/Long.html)[timeout](scan-for.md))  
  
More info  
<ul><li></li></ul>

Scans for a single bluetooth device and automatically connects with it Requires at least one filter being them: macAddress, service and name



#### Return  


A nullable [BluetoothConnection](../-bluetooth-connection/index.md) instance, when null meaning that the specified device was not found



## See also  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.BLE](scan-for-async.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>For a variation using callbacks<br><br><br><br>Filters:<br><br>
  


## Parameters  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>macAddress| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>Optional filter, if provided searches for the specified mac address<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>service| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>Optional filter, if provided searches for the specified service uuid<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>name| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>Optional filter, if provided searches for the specified device name<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>settings| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>Native object to specify the scan settings (The default setting is only recommended for really fast scans)<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>timeout| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>Scan time limit, when exceeded throws an [ScanTimeoutException](../../quevedo.soares.leandro.androideasyble.exceptions/-scan-timeout-exception/index.md)<br><br>
  


#### Throws  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.exceptions.ScanTimeoutException](../../quevedo.soares.leandro.androideasyble.exceptions/-scan-timeout-exception/index.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>When the timeout is reached<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.exceptions.ScanFailureException](../../quevedo.soares.leandro.androideasyble.exceptions/-scan-failure-exception/index.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/scanFor/#kotlin.String?#kotlin.String?#kotlin.String?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>When an error occurs<br><br>
  



