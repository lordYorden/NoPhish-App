package dev.lordyorden.as_no_phish_detector.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clerk.api.Clerk
import com.clerk.api.user.fullName
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentProfileDetailsBinding
import dev.lordyorden.as_no_phish_detector.models.CircleMember
import dev.lordyorden.as_no_phish_detector.models.Task
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.ImageLoader
import kotlinx.coroutines.launch

class ProfileDetailsFragment : Fragment() {
    private lateinit var binding: FragmentProfileDetailsBinding

    private val name: String
        get() = binding.etName.text.toString()

    private val familyRole: String
        get() = binding.spRole.text.toString()

    private lateinit var otpCode: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileDetailsBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        setupDropdown()

        observeUserFlow()

        binding.btnSave.setOnClickListener {
            if(name.isNotEmpty() && familyRole.isNotEmpty()) {
                lifecycleScope.launch {
                    registerMember()
                }
            } else {
                Log.w(TAG, "Save ignored because name or familyRole is empty")
            }
        }

        getOtpCode(arguments)
    }

    private fun getOtpCode(args: Bundle?) {
        otpCode = args?.getString(Constants.Circle.CIRCLE_CODE_KEY, Constants.OTP.TEST_VALUE) ?: Constants.OTP.TEST_VALUE
        Log.d(TAG, "got otp: $otpCode")
    }

    private suspend fun testConvex() {
        val client = ConvexHelper.getInstance().convexClient
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

        client.subscribe<List<CircleMember>>("members:get").collect { result ->
            result.onSuccess { members ->
                Log.d(TAG, "Received ${members.size} members")
                members.forEach{ member->
                    Log.d(TAG, "name: ${member.name}, role: ${member.familyRole}")
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch members", error)
            }
        }
    }

    private suspend fun registerMember() {
        val client = ConvexHelper.getInstance().convexClient
        val avatarUrl = Clerk.activeUser?.imageUrl ?: ""

        try {
            val circleId = client.mutation<String>("members:register", mapOf(
                "code" to otpCode,
                "name" to name,
                "familyRole" to familyRole,
                "avatarUrl" to avatarUrl
            ))

            //move to client activity
            val intent = Intent(requireActivity(), ClientActivity::class.java).apply {
                putExtra(Constants.Circle.CIRCLE_ID_KEY, circleId)
            }
            requireActivity().startActivity(intent)

        } catch (e: Exception) {
            val msg = e.message ?: "no message"
            Log.e(TAG, "error: $msg")
        }
    }

    private fun setupDropdown() {
        val roles = arrayOf("Son", "Daughter", "Nephew", "Niece", "Uncle", "Aunt", "Grandparent", "Grandchild", "Other")
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, roles)
        binding.spRole.setAdapter(adapter)
    }

    private fun observeUserFlow() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Clerk.userFlow.collect { user ->

                    requireActivity().runOnUiThread {
                        binding.etName.setText(user?.fullName())
                    }

                    user?.imageUrl?.let {
                        ImageLoader.getInstance().loadImage(it, binding.ivProfile)
                    }
                    Log.d("Clerk", "user details: $user")
                }
            }
        }
    }

    companion object {
        private const val TAG = "ProfileDetailsFragment"
    }

}