package dev.lordyorden.as_no_phish_detector.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.lordyorden.as_no_phish_detector.databinding.FragmentInviteToCircleBinding
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.HmacOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.HmacOneTimePasswordGenerator
import java.util.UUID


class InviteToCircleFragment : Fragment() {

    private lateinit var binding: FragmentInviteToCircleBinding
    private lateinit var code: String

    private lateinit var passGen: HmacOneTimePasswordGenerator
    private var counter: Long = 0L

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

        code = getOrGenerateCode()

        binding.tvCode.text = code

        binding.btnSend.setOnClickListener {
            //todo: add a circle message to send
            sendMessage(code)
        }
    }

    private fun getOrGenerateCode(): String {
        return generateNewPinCode()
    }

    private fun generateNewPinCode(): String {
        return passGen.generate(counter++)
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
}