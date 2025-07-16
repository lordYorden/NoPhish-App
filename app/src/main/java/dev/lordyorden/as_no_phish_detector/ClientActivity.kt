package dev.lordyorden.as_no_phish_detector

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vmadalin.easypermissions.EasyPermissions
import dev.lordyorden.as_no_phish_detector.databinding.ActivityClientBinding
import dev.lordyorden.as_no_phish_detector.services.UploadForegroundService
import dev.lordyorden.as_no_phish_detector.ui.settings.PermsViewModel

@RequiresApi(Build.VERSION_CODES.O)
class ClientActivity : AppCompatActivity(), EasyPermissions.RationaleCallbacks, EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityClientBinding
    val permsViewModel: PermsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
        initViews()
    }

    private fun initViews() {
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_client)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
//            )
//        )
        //setupActionBarWithNavController(navController, appBarConfiguration)

        navView.setupWithNavController(navController)
    }


    private fun commandToService(action: String) {
        val intent = Intent(this, UploadForegroundService::class.java)
        intent.setAction(action)
        startForegroundService(intent)
    }

    override fun onStart() {
        super.onStart()
        commandToService(UploadForegroundService.ACTION_START)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(
        requestCode: Int,
        perms: List<String>
    ) {
        Toast.makeText(this, "perms denied $requestCode", Toast.LENGTH_SHORT).show()
        permsViewModel.setRejected(requestCode)
    }

    override fun onPermissionsGranted(
        requestCode: Int,
        perms: List<String>
    ) {
        Toast.makeText(this, "perms granted $requestCode", Toast.LENGTH_SHORT).show()
        permsViewModel.setGranted(requestCode)
    }

    override fun onRationaleAccepted(requestCode: Int) {
        Toast.makeText(this, "rel granted $requestCode", Toast.LENGTH_SHORT).show()
        //permsViewModel.setGranted(requestCode)
    }

    override fun onRationaleDenied(requestCode: Int) {
        Toast.makeText(this, "rel denied $requestCode", Toast.LENGTH_SHORT).show()
        //permsViewModel.setRejected(requestCode)
    }

}