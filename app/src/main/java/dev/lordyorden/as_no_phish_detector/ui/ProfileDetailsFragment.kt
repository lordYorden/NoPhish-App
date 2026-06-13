package dev.lordyorden.as_no_phish_detector.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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
import dev.lordyorden.as_no_phish_detector.repositories.CircleMembersRepository
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.ImageLoader
import kotlinx.coroutines.launch

class ProfileDetailsFragment : Fragment() {
    private lateinit var binding: FragmentProfileDetailsBinding

    private val name: String
        get() = binding.etName.text.toString()

    private val familyRole: String
        get() = if (binding.spRole.text.toString() == OTHER_ROLE) {
            binding.etOther.text.toString()
        } else {
            binding.spRole.text.toString()
        }

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
                Toast.makeText(requireContext(), "missing name of family role", Toast.LENGTH_SHORT).show()
            }
        }

        getOtpCode(arguments)
    }

    private fun getOtpCode(args: Bundle?) {
        otpCode = args?.getString(Constants.Circle.CIRCLE_CODE_KEY, Constants.OTP.TEST_VALUE) ?: Constants.OTP.TEST_VALUE
        Log.d(TAG, "got otp: $otpCode")
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
            CircleMembersRepository.getInstance().setCurrentCircleId(circleId)

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
        binding.tiOther.visibility = View.GONE

        val roles = arrayOf("Son", "Daughter", "Nephew", "Niece", "Uncle", "Aunt", "Grandparent", "Grandchild", OTHER_ROLE)
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, roles)
        binding.spRole.setAdapter(adapter)
        binding.spRole.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedRole = roles[position]
            binding.tiOther.visibility = if (selectedRole == OTHER_ROLE) View.VISIBLE else View.GONE
        }
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
        private const val OTHER_ROLE = "Other"
    }

}
