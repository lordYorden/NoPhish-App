package dev.lordyorden.as_no_phish_detector.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.clerk.UserStateViewModel
import dev.lordyorden.as_no_phish_detector.clerk.UserUiState
import dev.lordyorden.as_no_phish_detector.databinding.FragmentLoginBinding
import dev.lordyorden.as_no_phish_detector.repositories.CircleMembersRepository
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val userState: UserStateViewModel by viewModels()

    private var dialog: AlertDialog? = null

    private var isRunning: Boolean = false
    private var connectionDialogShown: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userState.uiState.collect { currState ->

                    when (currState) {
                        UserUiState.SignedIn -> {
                            fetchAndMoveToClient()
                        }

                        UserUiState.Loading -> {
                            if (!NetworkMonitor.getInstance().isOnline.value) {
                                disableGoogleButton()
                                binding.loading.visibility = View.GONE
                                showConnectionFailureDialog()
                                return@collect
                            }
                            connectionDialogShown = false
                            disableGoogleButton()
                            binding.loading.visibility = View.VISIBLE
                        }

                        UserUiState.ConnectionFailure -> {
                            disableGoogleButton()
                            binding.loading.visibility = View.GONE
                            showConnectionFailureDialog()
                        }

                        UserUiState.SignedOut -> {
                            if (!NetworkMonitor.getInstance().isOnline.value) {
                                disableGoogleButton()
                                binding.loading.visibility = View.GONE
                                showConnectionFailureDialog()
                                return@collect
                            }
                            connectionDialogShown = false
                            enableGoogleButton()
                            binding.loading.visibility = View.GONE
                        }
                    }
                }
            }
        }

        observeConnectionState()

        setupPolicySpan()
        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun observeConnectionState() {
        val networkMonitor = NetworkMonitor.getInstance()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isOnline.collect { isOnline ->
                    if (isOnline) {
                        connectionDialogShown = false
                        dialog?.dismiss()
                        dialog = null

                        if (userState.uiState.value == UserUiState.SignedOut) {
                            enableGoogleButton()
                        }
                    } else {
                        disableGoogleButton()
                        binding.loading.visibility = View.GONE
                        showConnectionFailureDialog()
                    }
                }
            }
        }
    }

    private fun disableGoogleButton() {
        binding.btnGoogle.alpha = 0.65f
        binding.btnGoogle.isEnabled = false
    }

    private fun enableGoogleButton() {
        binding.btnGoogle.alpha = 1f
        binding.btnGoogle.isEnabled = true
    }

    private suspend fun fetchAndMoveToClient() {
        if (isRunning) {
            return
        }
        isRunning = true

        try {
            val result = withContext(Dispatchers.IO) { getMyCircle() }
            if (!isAdded) return

            when (result) {
                is CircleResolution.HasCircle -> {
                    moveToClient(result.circleId)
                }

                CircleResolution.NeedsOnboarding -> {
                    findNavController().navigate(R.id.action_loginFragment_to_welcomeFragment)
                }

                is CircleResolution.Failed -> {
                    Log.e(TAG, "Failed to resolve circle", result.error)
                    if (!NetworkMonitor.getInstance().isOnline.value) {
                        showConnectionFailureDialog()
                    } else {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.connection_failed_title)
                            .setMessage(
                                result.error.message ?: getString(R.string.session_expired)
                            )
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                }
            }
        } finally {
            isRunning = false
        }
    }

    private fun moveToClient(circleId: String) {
        CircleMembersRepository.getInstance().setCurrentCircleId(circleId)

        val intent = Intent(requireActivity(), ClientActivity::class.java).apply {
            putExtra(Constants.Circle.CIRCLE_ID_KEY, circleId)
        }
        requireActivity().startActivity(intent)
    }

    private suspend fun getMyCircle(): CircleResolution {
        val client = ConvexHelper.getInstance().convexClient

        return try {
            val circleId = client.mutation<String>("circles:get_my_circles")
            if (circleId == Constants.Onboarding.ACTION_GENERATE) {
                CircleResolution.NeedsOnboarding
            } else {
                CircleResolution.HasCircle(circleId)
            }
        } catch (e: Exception) {
            CircleResolution.Failed(e)
        }
    }

    private fun showConnectionFailureDialog() {
        if (connectionDialogShown || !isAdded) return
        connectionDialogShown = true

        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.connection_failed_title)
            .setMessage(R.string.connection_failed_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private val privacyClick = object : ClickableSpan() {
        override fun onClick(widget: View) {
            // TODO: Handle Privacy Policy click
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = true
            ds.color = ContextCompat.getColor(requireContext(), R.color.primary)
        }
    }

    private val termsClick = object : ClickableSpan() {
        override fun onClick(widget: View) {
            // TODO: Handle Terms click
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = true
            ds.color = ContextCompat.getColor(requireContext(), R.color.primary)
        }
    }

    private fun setupPolicySpan() {
        val fullText = resources.getString(R.string.policy_subtitle)
        val spannable = SpannableString(fullText)

        val privacyStart = fullText.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length
        val termsStart = fullText.indexOf("Terms of Service")
        val termsEnd = termsStart + "Terms of Service".length

        spannable.setSpan(privacyClick, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(termsClick, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvTerms.text = spannable
        binding.tvTerms.highlightColor = Color.TRANSPARENT
        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()


    }

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
                    } else if (res.signIn?.status == SignIn.Status.NEEDS_CLIENT_TRUST) {
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

    companion object {
        const val TAG = "LoginFragment"
    }

    private sealed interface CircleResolution {
        data class HasCircle(val circleId: String) : CircleResolution
        data object NeedsOnboarding : CircleResolution
        data class Failed(val error: Throwable) : CircleResolution
    }
}
