//[lib](../../index.md)/[quevedo.soares.leandro.androideasyble](../index.md)/[BLE](index.md)/[verifyPermissionsAsync](verify-permissions-async.md)



# verifyPermissionsAsync  
[androidJvm]  
Content  
final [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)[verifyPermissionsAsync](verify-permissions-async.md)(Function1<Function0<[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)>, [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)>[rationaleRequestCallback](verify-permissions-async.md), Function1<[Boolean](https://docs.oracle.com/javase/8/docs/api/java/lang/Boolean.html), [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)>[callback](verify-permissions-async.md))  
  
More info  
<ul><li></li></ul>

Checks if the following permissions are granted: [permission.BLUETOOTH](https://developer.android.com/reference/kotlin/android/Manifest.permission.html#bluetooth), [permission.BLUETOOTH_ADMIN](https://developer.android.com/reference/kotlin/android/Manifest.permission.html#bluetooth_admin) and [permission.ACCESS_FINE_LOCATION](https://developer.android.com/reference/kotlin/android/Manifest.permission.html#access_fine_location)



If any of these isn't granted, automatically requests it to the user



## See also  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissionsAsync/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?#kotlin.Function1[kotlin.Boolean,kotlin.Unit]?/PointingToDeclaration/"></a>[quevedo.soares.leandro.androideasyble.BLE](verify-permissions.md)| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissionsAsync/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?#kotlin.Function1[kotlin.Boolean,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>For a variation using coroutines suspended functions<br><br>
  


## Parameters  
  
androidJvm  
  
|  Name|  Summary| 
|---|---|
| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissionsAsync/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?#kotlin.Function1[kotlin.Boolean,kotlin.Unit]?/PointingToDeclaration/"></a>rationaleRequestCallback| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissionsAsync/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?#kotlin.Function1[kotlin.Boolean,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>Called when rationale permission is required, should explain on the UI why the permissions are needed and then re-call this method<br><br>
| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissionsAsync/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?#kotlin.Function1[kotlin.Boolean,kotlin.Unit]?/PointingToDeclaration/"></a>callback| <a name="quevedo.soares.leandro.androideasyble/BLE/verifyPermissionsAsync/#kotlin.Function1[kotlin.Function0[kotlin.Unit],kotlin.Unit]?#kotlin.Function1[kotlin.Boolean,kotlin.Unit]?/PointingToDeclaration/"></a><br><br>Called with a boolean parameter indicating the permission request state<br><br>
  
  



