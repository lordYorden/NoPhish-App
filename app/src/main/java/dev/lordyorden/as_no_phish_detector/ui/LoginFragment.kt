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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.clerk.UserStateViewModel
import dev.lordyorden.as_no_phish_detector.clerk.UserUiState
import dev.lordyorden.as_no_phish_detector.databinding.FragmentLoginBinding
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val userState: UserStateViewModel by viewModels()

    private var isRunning: Boolean = false

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
        lifecycleScope.launch {
            userState.uiState.collect { currState ->

                when (currState) {
                    UserUiState.SignedIn -> {
                        fetchAndMoveToClient()
                    }

                    UserUiState.Loading -> {
                        binding.btnGoogle.alpha = 0.65f
                        binding.btnGoogle.isEnabled = false
                    }

                    else -> {
                        binding.btnGoogle.alpha = 1f
                        binding.btnGoogle.isEnabled = true
                    }
                }
            }
        }

        setupPolicySpan()
        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private suspend fun fetchAndMoveToClient(){
        if (isRunning){ return }
        isRunning = true

        withContext(Dispatchers.IO){
            val circleId = getMyCircle()
            if (circleId != Constants.Onboarding.ACTION_GENERATE){
                moveToClient(circleId)
            }
            else {
                requireActivity().runOnUiThread {
                    findNavController().navigate(R.id.action_loginFragment_to_welcomeFragment)
                }
            }
        }

        isRunning = false
    }

    private fun moveToClient(circleId: String){
        val intent = Intent(requireActivity(), ClientActivity::class.java).apply {
            putExtra(Constants.Circle.CIRCLE_ID_KEY, circleId)
        }
        requireActivity().startActivity(intent)
    }

    private suspend fun getMyCircle(): String {
        val client = ConvexHelper.getInstance().convexClient

        try {
            val circleId = client.mutation<String>("circles:get_my_circles")
            return circleId
        } catch (e: Exception) {
            val msg = e.message ?: "no msg"
            Log.e(TAG, "error $msg")
        }

        return Constants.Onboarding.ACTION_GENERATE
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
                    }
                    else if (res.signIn?.status == SignIn.Status.NEEDS_CLIENT_TRUST) {
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
}