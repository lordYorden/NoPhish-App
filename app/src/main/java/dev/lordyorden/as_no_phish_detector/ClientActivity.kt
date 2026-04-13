package dev.lordyorden.as_no_phish_detector

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.clerk.api.Clerk
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.messaging.FirebaseMessaging
import com.vmadalin.easypermissions.EasyPermissions
import dev.lordyorden.as_no_phish_detector.databinding.ActivityClientBinding
import dev.lordyorden.as_no_phish_detector.databinding.AttackDetailsBottomSheetBinding
import dev.lordyorden.as_no_phish_detector.databinding.UrlItemBinding
import dev.lordyorden.as_no_phish_detector.services.FCMService
import dev.lordyorden.as_no_phish_detector.services.UploadForegroundService
import dev.lordyorden.as_no_phish_detector.ui.settings.PermsViewModel
import dev.lordyorden.as_no_phish_detector.utilities.ImageLoader

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
        handleIntent(intent)
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

        FirebaseMessaging.getInstance().subscribeToTopic("test_topic")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Successfully subscribed to topic!")
                } else {
                    Log.e("FCM", "Subscription failed")
                }
            }
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
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Clerk.auth.handle(intent.data)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Log.e("intent client", "checking action")

        when(intent.action) {
            FCMService.SHOW_DETAILS_ACTION -> {
                intent.extras?.let {
                    showDetailsBottomSheet(it)
                }

            }
        }
    }

    private fun showDetailsBottomSheet(extras: Bundle) {
        val body = extras.getString("body", "Body is empty")
        val packageName = extras.getString("packageName", "none")
        val urls = extras.getStringArrayList("urls")?.toList()
        val sheet = BottomSheetDialog(this)
        val sheetView = AttackDetailsBottomSheetBinding.inflate(layoutInflater)
        sheet.behavior.peekHeight = 1000

        ImageLoader.getInstance().loadAppIcon(packageName, sheetView.ivAppIcon)
        sheetView.tvBody.text = body

        sheetView.tvAppName.text = packageName

        try {
            urls?.forEach { url ->
                val urlItem = UrlItemBinding.inflate(layoutInflater, sheetView.root, false)
                urlItem.tvUrl.text = url
                sheetView.listUrls.addView(urlItem.root)
            }

            if(urls?.isNotEmpty() == true){
                sheetView.tvNoUrl.visibility = View.GONE
            }
        }
        catch(e: NoSuchElementException){
            sheetView.tvNoUrl.visibility = View.VISIBLE
        }

        sheet.setCancelable(true)
        sheet.setContentView(sheetView.root)
        sheet.show()
    }
}

