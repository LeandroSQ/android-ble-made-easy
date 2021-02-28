package quevedo.soares.leandro.androideasyble.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import quevedo.soares.leandro.androideasyble.R

class MainActivity : AppCompatActivity() {

	private val navController by lazy { findNavController(R.id.am_fcv_host) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
	}

	override fun onSupportNavigateUp(): Boolean = navController.navigateUp()

}