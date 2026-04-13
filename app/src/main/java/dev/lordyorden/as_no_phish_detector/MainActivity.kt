package dev.lordyorden.as_no_phish_detector

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clerk.api.Clerk
import com.clerk.convex.createClerkConvexClient
import dev.convex.android.ConvexClient
import dev.lordyorden.as_no_phish_detector.databinding.ActivityMainBinding
import dev.lordyorden.as_no_phish_detector.models.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
//    private val client: ConvexClient by lazy { createClerkConvexClient("https://enchanted-mallard-804.convex.cloud", this) }
    private var tasksSubscriptionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        initViews()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Handle redirect data if the activity is already running
        Clerk.auth.handle(intent.data)
    }


    private fun initViews() {
//        binding.btn.setOnClickListener {
//            val intent = Intent(this, ClientActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
    }

/*    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        MainUiState.Loading -> Log.d(TAG, "Auth state: loading")
                        MainUiState.SignedIn -> Log.d(TAG, "Auth state: signed in")
                        MainUiState.SignedOut -> Log.d(TAG, "Auth state: signed out")
                    }
                }
            }
        }
    }*/


//    private fun subscribeToTasks() {
//        if (tasksSubscriptionJob != null) return
//
//        tasksSubscriptionJob = lifecycleScope.launch {
//            client.subscribe<List<Task>>("tasks:get").collect { result ->
//                result.onSuccess { tasks ->
//                    Log.d(TAG, "Received ${tasks.size} tasks")
//                    tasks.forEach{ task->
//                        Log.d(TAG, "text: ${task.text}, isComplete: ${task.isCompleted}")
//                    }
//                }.onFailure { error ->
//                    Log.e(TAG, "Failed to fetch tasks", error)
//                }
//            }
//        }
//    }

/*    private fun commandToService(action: String) {
        val intent = Intent(this, UploadForegroundService::class.java)
        intent.setAction(action)
        startForegroundService(intent)
    }*/

    companion object {
        private const val TAG = "MainActivity"
    }
}