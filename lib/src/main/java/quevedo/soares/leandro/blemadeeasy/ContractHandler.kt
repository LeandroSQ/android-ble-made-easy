package quevedo.soares.leandro.blemadeeasy

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import quevedo.soares.leandro.blemadeeasy.typealiases.Callback

class ContractHandler<I, O>(contract: ActivityResultContract<I, O>, componentActivity: ComponentActivity?, activity: AppCompatActivity?, fragment: Fragment?) {

	private var activityResultLauncher: ActivityResultLauncher<I>? = null
	private var callback: Callback<O>? = null

	init {
		when {
			// Jetpack compose
			componentActivity != null -> this.activityResultLauncher = componentActivity.registerForActivityResult(contract, this::onContractResult)

			// AppCompatActivity
			activity != null -> this.activityResultLauncher = activity.registerForActivityResult(contract, this::onContractResult)

			// Fragment
			fragment != null -> this.activityResultLauncher = fragment.registerForActivityResult(contract, this::onContractResult)
		}
	}

	private fun onContractResult(output: O) {
		callback?.invoke(output)
	}

	fun launch(input: I) {
		this.activityResultLauncher?.launch(input)
	}

	fun launch(input: I, callback: Callback<O>) {
		this.callback = callback
		this.activityResultLauncher?.launch(input)
	}

}