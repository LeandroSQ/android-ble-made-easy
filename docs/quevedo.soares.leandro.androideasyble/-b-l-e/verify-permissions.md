//[lib](../../index.md)/[quevedo.soares.leandro.androideasyble](../index.md)/[BLE](index.md)/[verifyPermissions](verify-permissions.md)



# verifyPermissions  
[androidJvm]  
Content  
final [Boolean](https://docs.oracle.com/javase/8/docs/api/java/lang/Boolean.html)[verifyPermissions](verify-permissions.md)(Function1<Function0<[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)>, [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)>[rationaleRequestCallback](verify-permissions.md))  
  
More info  
<ul><li></li></ul>

Checks if the following permissions are granted: [permission.BLUETOOTH](https://developer.android.com/reference/kotlin/android/Manifest.permission.html#bluetooth), [permission.BLUETOOTH_ADMIN](https://developer.android.com/reference/kotlin/android/Manifest.permission.html#bluetooth_admin) and [permission.ACCESS_FINE_LOCATION](https://developer.android.com/reference/kotlin/android/Manifest.permission.html#access_fine_location)



If any of these isn't granted, automatically requests it to the user



#### Return  


True when all the permissions are granted



## See also  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissions/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.BLE](verify-permissions-async.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissions/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?/PointingToDeclaration/"></a><br><br>For a variation using callbacks<br><br>
  


## Parameters  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissions/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?/PointingToDeclaration/"></a>rationaleRequestCallback| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissions/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?/PointingToDeclaration/"></a><br><br>Called when rationale permission is required, should explain on the UI why the permissions are needed and then re-call this method<br><br>
  
  



