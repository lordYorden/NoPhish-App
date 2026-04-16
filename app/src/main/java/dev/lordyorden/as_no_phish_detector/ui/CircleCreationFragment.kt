package dev.lordyorden.as_no_phish_detector.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentCircleCreationBinding
import dev.lordyorden.as_no_phish_detector.databinding.FragmentWelcomeBinding
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import kotlinx.coroutines.launch

class CircleCreationFragment : Fragment() {

    private lateinit var binding: FragmentCircleCreationBinding

    private val circleName: String
        get() = binding.etName.text.toString()

    private val description: String
        get() = binding.etDescription.text.toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCircleCreationBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        binding.btnCreate.setOnClickListener {
            if(circleName.isNotEmpty()){
                lifecycleScope.launch {
                    registerCircle()
                }
            }
        }
    }

    private suspend fun registerCircle() {
        val client = ConvexHelper.getInstance().convexClient
        try {
            val circleId = client.mutation<String>("circles:create", mapOf(
                "name" to circleName,
                "description" to description
            ))

            client.mutation("otps:issue", mapOf(
                "code" to Constants.OTP.OWNER_OTP,
                "circleId" to circleId
            ))

            val extra = Bundle().apply {
                putString(Constants.Circle.CIRCLE_CODE_KEY, Constants.OTP.OWNER_OTP)
            }

            findNavController().navigate(R.id.action_circleCreationFragment_to_profileDetailsFragment, extra)
        } catch (e: Exception) {
            val msg = e.message ?: "no msg"
            Log.e(TAG, "error: $msg")
        }
    }

    companion object {
        const val TAG = "FragmentCircleCreationBinding"
    }

}