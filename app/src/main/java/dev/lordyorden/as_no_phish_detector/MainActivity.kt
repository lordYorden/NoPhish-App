package dev.lordyorden.as_no_phish_detector

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dev.lordyorden.as_no_phish_detector.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        initViews()
    }


    private fun initViews() {
//        binding.btnSetup.setOnClickListener{
//            val intent = Intent(this, ClientActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
    }

/*    private fun commandToService(action: String) {
        val intent = Intent(this, UploadForegroundService::class.java)
        intent.setAction(action)
        startForegroundService(intent)
    }*/
}