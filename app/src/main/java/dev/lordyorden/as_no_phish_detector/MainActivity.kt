package dev.lordyorden.as_no_phish_detector

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import dev.lordyorden.as_no_phish_detector.clerk.UserUiState
import dev.lordyorden.as_no_phish_detector.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    //private val userState: UserStateViewModel by viewModels()
    //private var isLoggedIn: Boolean = false

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

/*        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                getUserState()
            }
        }*/

/*        binding.toolbar.setNavigationOnClickListener {
            if (isLoggedIn) {
                lifecycleScope.launch {
                    Clerk.auth.signOut()
                }
            }else{
                signInWithGoogle()
            }
        }*/

    }

    private fun updateActionIcon(userState: UserUiState) {
        when (userState) {
            UserUiState.SignedIn -> {
                binding.toolbar.navigationIcon =
                    ContextCompat.getDrawable(this, R.drawable.ic_logout)
                binding.toolbar.navigationIcon?.alpha = 255
            }

            UserUiState.SignedOut -> {
                binding.toolbar.navigationIcon =
                    ContextCompat.getDrawable(this, R.drawable.ic_login)
                binding.toolbar.navigationIcon?.alpha = 255
            }

            UserUiState.Loading -> {
                binding.toolbar.navigationIcon?.alpha = 125
            }

            UserUiState.ConnectionFailure -> {
                binding.toolbar.navigationIcon?.alpha = 125
            }
        }
    }

    /*private suspend fun getUserState() {
        userState.uiState.collect{ newState ->
            isLoggedIn = (newState == UserUiState.SignedIn)
            runOnUiThread {
                updateActionIcon(newState)
            }
        }
    }*/

    private fun signInWithGoogle() {
        lifecycleScope.launch {
            Clerk.auth.signInWithOAuth(OAuthProvider.GOOGLE)
                .onSuccess { res ->
                    val sessionId = res.signIn?.createdSessionId ?: res.signUp?.createdSessionId
                    if (sessionId != null) {
                        // CRITICAL: This persists the session to the device
                        // and stops it from creating new ones every time.
                        Clerk.auth.setActive(sessionId)
                    }

                    if (res.signIn?.status != SignIn.Status.COMPLETE) {
                        // User might need to provide extra info (e.g. missing phone number)
                        Log.d("Clerk", "Missing requirements: ${res.signUp?.requiredFields}")
                    }

                    if (res.signIn?.status == SignIn.Status.NEEDS_CLIENT_TRUST) {
                        // You must now show a UI for the user to enter a code
                        // and call res.prepareFirstFactor() then res.attemptFirstFactor()
                        Log.d("Clerk", "Device is new. Verification code sent to email.")
                    }

                }
                .onFailure { error ->
                    Log.e("Clerk", "OAuth failed: ${error.errorMessage}")
                }
        }
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

    /*    private fun subscribeToTasks() {
            if (tasksSubscriptionJob != null) return

            tasksSubscriptionJob = lifecycleScope.launch {
                client.subscribe<List<Task>>("tasks:get").collect { result ->
                    result.onSuccess { tasks ->
                        Log.d(TAG, "Received ${tasks.size} tasks")
                        tasks.forEach{ task->
                            Log.d(TAG, "text: ${task.text}, isComplete: ${task.isCompleted}")
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to fetch tasks", error)
                    }
                }
            }
        }*/

    /*    private fun commandToService(action: String) {
            val intent = Intent(this, UploadForegroundService::class.java)
            intent.setAction(action)
            startForegroundService(intent)
        }*/

    companion object {
        private const val TAG = "MainActivity"
    }
}
