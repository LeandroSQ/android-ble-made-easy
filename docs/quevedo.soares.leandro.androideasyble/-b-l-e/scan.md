//[lib](../../index.md)/[quevedo.soares.leandro.androideasyble](../index.md)/[BLE](index.md)/[scan](scan.md)



# scan  
[androidJvm]  
Content  
final [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)<[BLEDevice](../../quevedo.soares.leandro.androideasyble.models/-b-l-e-device/index.md)>[scan](scan.md)([List](https://docs.oracle.com/javase/8/docs/api/java/util/List.html)<[ScanFilter](https://developer.android.com/reference/kotlin/android/bluetooth/le/ScanFilter.html)>[filters](scan.md), [ScanSettings](https://developer.android.com/reference/kotlin/android/bluetooth/le/ScanSettings.html)[settings](scan.md), [Long](https://docs.oracle.com/javase/8/docs/api/java/lang/Long.html)[duration](scan.md))  
  
More info  
<ul><li></li></ul>

Starts a scan for bluetooth devices Only runs with a duration defined



If only one device is required consider using [scanFor](scan-for.md)



#### Return  


An Array of Bluetooth devices found



## See also  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.BLE](scan.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>For a variation using callbacks<br><br>
  


## Parameters  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>filters| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>Used to specify attributes of the devices on the scan<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>settings| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>Native object to specify the scan settings (The default setting is only recommended for really fast scans)<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>duration| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>Scan time limit, when exceeded stops the scan <b>(Ignored when less then 0)</b><br><br>
  


#### Throws  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>[kotlin.IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html)| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>When a duration is not defined<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.exceptions.ScanFailureException](../../quevedo.soares.leandro.androideasyble.exceptions/-scan-failure-exception/index.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/scan/#kotlin.collections.List[android.bluetooth.le.ScanFilter]?#android.bluetooth.le.ScanSettings?#kotlin.Long/PointingToDeclaration/"></a><br><br>When an error occurs<br><br>
  



