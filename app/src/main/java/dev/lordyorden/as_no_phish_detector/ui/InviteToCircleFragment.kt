package dev.lordyorden.as_no_phish_detector.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dev.lordyorden.as_no_phish_detector.databinding.FragmentInviteToCircleBinding
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.HmacOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.HmacOneTimePasswordGenerator
import kotlinx.coroutines.launch
import java.util.UUID


class InviteToCircleFragment : Fragment() {

    private lateinit var binding: FragmentInviteToCircleBinding
    private lateinit var code: String

    private lateinit var passGen: HmacOneTimePasswordGenerator
    private var counter: Long = 0L

    private lateinit var circleId: String


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInviteToCircleBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {

        configOtp()

        getCircleId()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                getOrGenerateCode()
            }
        }

        binding.btnSend.setOnClickListener {
            val msg = buildString {
                append("Please join my Close Circle using the code: ")
                append(code)
                append("\nso we can keep my account secure together.")
            }

            sendMessage(msg)
        }
    }

    private fun updateCodeUI(){
        binding.tvCode.text = code
    }

    private suspend fun getOrGenerateCode(){

        val client = ConvexHelper.getInstance().convexClient

        client.subscribe<String>("otps:needsotp").collect{result ->
            result.onSuccess { otp ->
                if (otp == Constants.Onboarding.ACTION_GENERATE){
                    code = generateNewPinCode()

                    try {
                        client.mutation("otps:issue", mapOf(
                            "code" to code,
                            "circleId" to circleId
                        ))
                    } catch (e: Exception) {
                        val msg = e.message ?: "no msg"
                        Log.e(TAG, "error: $msg")
                    }
                }
                else{
                    code = otp
                }

                requireActivity().runOnUiThread {
                    updateCodeUI()
                }
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to fetch members", error)
            }
        }
    }

    private fun generateNewPinCode(): String {
        return passGen.generate(counter++)
    }

    private fun getCircleId() {
        val extra = requireActivity().intent.extras
        circleId = extra?.getString(Constants.Circle.CIRCLE_ID_KEY, Constants.Circle.CIRCLE_TEMP_ID) ?: Constants.Circle.CIRCLE_TEMP_ID
        Log.d(CircleFragment.TAG, "got circleId: $circleId")
    }

    private fun configOtp(){
        val config = HmacOneTimePasswordConfig(codeDigits = Constants.OTP.OTP_LENGTH,
            hmacAlgorithm = HmacAlgorithm.SHA1)

        val secret = if (Constants.OTP.AUTO_GENERATE_SECRET) {
            UUID.randomUUID().toString()
        } else {
            Constants.OTP.OTP_SECRET
        }

        passGen = HmacOneTimePasswordGenerator(secret.toByteArray(), config)
        counter = 0
    }


    private fun sendMessage(message: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
        findNavController().popBackStack()
    }

    companion object {
        const val TAG = "InviteToCircleFragment"
    }
}