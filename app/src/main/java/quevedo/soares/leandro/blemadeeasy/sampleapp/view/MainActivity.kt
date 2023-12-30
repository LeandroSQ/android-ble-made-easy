package quevedo.soares.leandro.blemadeeasy.sampleapp.view

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import quevedo.soares.leandro.blemadeeasy.sampleapp.R
import quevedo.soares.leandro.blemadeeasy.sampleapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

	private val navController by lazy { findNavController(R.id.home) }
	private lateinit var binding: ActivityMainBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(this.binding.root)
	}

	override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
		super.onPostCreate(savedInstanceState, persistentState)

		this.binding.amMtToolbar.setupWithNavController(this.navController, AppBarConfiguration(setOf()))
	}

	override fun onSupportNavigateUp(): Boolean = navController.navigateUp()

}