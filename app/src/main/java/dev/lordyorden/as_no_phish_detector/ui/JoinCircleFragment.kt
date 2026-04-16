package dev.lordyorden.as_no_phish_detector.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentJoinCircleBinding
import dev.lordyorden.as_no_phish_detector.utilities.Constants

class JoinCircleFragment : Fragment() {

    private lateinit var binding: FragmentJoinCircleBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentJoinCircleBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.otpInput.focusOtpInput()
    }

    private fun initViews() {
        binding.otpInput.onInputFinishedListener { otpText ->
            Log.d("JoinScreen", "otp fin: $otpText")
//            if (otpText == Constants.OTP.TEST_VALUE){
                val bundle = Bundle().apply {
                    putString(Constants.Circle.CIRCLE_CODE_KEY, otpText)
                }
                findNavController().navigate(R.id.action_joinCircleFragment_to_profileDetailsFragment, bundle)
//            }
        }
    }
}