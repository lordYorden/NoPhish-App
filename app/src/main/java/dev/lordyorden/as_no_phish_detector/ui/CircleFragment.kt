package dev.lordyorden.as_no_phish_detector.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.models.PermissionRequest
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.adapters.CircleAdapter
import dev.lordyorden.as_no_phish_detector.adapters.EventPreviewAdapter
import dev.lordyorden.as_no_phish_detector.databinding.FragmentCircleBinding
import dev.lordyorden.as_no_phish_detector.databinding.SectionRecentActivityBinding
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.repositories.CircleMembersRepository
import dev.lordyorden.as_no_phish_detector.services.UploadForegroundService
import dev.lordyorden.as_no_phish_detector.ui.events.CircleRecentActivityRenderer
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CircleFragment : Fragment() {

    private lateinit var binding: FragmentCircleBinding
    private lateinit var recentBinding: SectionRecentActivityBinding
    private lateinit var circleAdapter: CircleAdapter
    private lateinit var eventPreviewAdapter: EventPreviewAdapter
    private lateinit var recentActivityRenderer: CircleRecentActivityRenderer
    private lateinit var localStore: MaliciousNotificationStore
    private val recentEvents = MutableStateFlow<List<Event>>(emptyList())

    private lateinit var circleId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCircleBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {

        localStore = MaliciousNotificationStore.getInstance()
        getCircleId()
        setupProtectionStatus()
        setUpRecentActivity()
        setupBottomSheet()
        binding.btnAdd.setOnClickListener {

            val bundle = Bundle().apply {
                putString(Constants.Circle.CIRCLE_ID_KEY, circleId)
            }

            findNavController().navigate(R.id.action_nev_circle_to_invite_fragment, bundle)
        }


    }

    override fun onResume() {
        super.onResume()
        renderProtectionStatus()
    }

    private fun setupProtectionStatus() {
        binding.sectionProtectionOffStatus.btnTurnOnProtection.setOnClickListener {
            when {
                !isNotificationListenerEnabled() -> {
                    startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }

                !isPostNotificationGranted() -> {
                    requestPostNotificationPermission()
                }
            }
        }

        renderProtectionStatus()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UploadForegroundService.isRunning.collect {
                    renderProtectionStatus()
                }
            }
        }
    }

    private fun renderProtectionStatus() {
        val protected = hasProtectionRequirements()
        binding.sectionProtectedStatus.root.visibility = if (protected) View.VISIBLE else View.GONE
        binding.sectionProtectionOffStatus.root.visibility = if (protected) View.GONE else View.VISIBLE
    }

    private fun hasProtectionRequirements(): Boolean {
        return UploadForegroundService.isRunning.value &&
            isNotificationListenerEnabled() &&
            isPostNotificationGranted()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.split(':').any { flattenedComponent ->
            val componentName = ComponentName.unflattenFromString(flattenedComponent)
            componentName?.packageName == requireContext().packageName
        }
    }

    private fun isPostNotificationGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val request = PermissionRequest.Builder(requireActivity())
            .code(Constants.Perms.POST_NOTIFICATION_CODE)
            .perms(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            .build()
        EasyPermissions.requestPermissions(requireActivity(), request)
    }

    private fun setUpRecentActivity() {
        recentBinding = binding.sectionRecent
        recentBinding.rvEvents.layoutManager = LinearLayoutManager(requireContext()).apply {
            orientation = RecyclerView.HORIZONTAL
        }

        eventPreviewAdapter = EventPreviewAdapter { event ->
            Log.d(TAG, "event clicked with id: ${event.eventId}")
            viewLifecycleOwner.lifecycleScope.launch {
                val details = localStore.getValidated(event.eventId, event.contentHash)
                if (details == null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.msg_unavailable_event),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val client = requireActivity() as ClientActivity
                client.showDetailsBottomSheet(details)
            }
        }

        recentBinding.rvEvents.adapter = eventPreviewAdapter
        recentActivityRenderer = CircleRecentActivityRenderer(
            binding = recentBinding,
            adapter = eventPreviewAdapter,
            context = requireContext(),
        )
        recentBinding.tvViewAll.setOnClickListener {
            val bundle = Bundle().apply {
                putString(Constants.Circle.CIRCLE_ID_KEY, circleId)
            }

            findNavController().navigate(R.id.action_nev_circle_to_circle_events, bundle)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    getRecentCircleEvents()
                }

                launch {
                    combine(
                        recentEvents,
                        CircleMembersRepository.getInstance().observe(circleId),
                    ) { events, membersState ->
                        events to membersState
                    }.collect { (events, membersState) ->
                        recentActivityRenderer.render(
                            events = events,
                            membersState = membersState,
                        )
                    }
                }
            }
        }
    }

    private fun setupBottomSheet() {
        BottomSheetBehavior.from(binding.bsCircle).apply {
            isFitToContents = false
            halfExpandedRatio = 0.5f
            isHideable = false
            peekHeight = 250 // visible portion when collapsed (in px)
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.rvCircle.layoutManager = LinearLayoutManager(requireContext())

        circleAdapter = CircleAdapter { member ->
            Log.d("CircleFragment", "member clicked $member")
        }

        binding.rvCircle.adapter = circleAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CircleMembersRepository.getInstance().observe(circleId).collect { state ->
                    state.errorMessage?.let { message ->
                        Log.e(TAG, "Failed to fetch members: $message")
                    }
                    circleAdapter.submitList(state.members)
                }
            }
        }
    }

    private fun getCircleId() {
        circleId = CircleMembersRepository.getInstance().requireCurrentCircleId()
        Log.d(TAG, "got circleId: $circleId")
    }

    private suspend fun getRecentCircleEvents() {
        val client = ConvexHelper.getInstance().convexClient
        val args = mapOf(
            "circleId" to circleId,
            "limit" to Constants.Circle.RECENT_EVENT_LIMIT
        )

        client.subscribe<List<Event>>("events:get_recent_by_circle", args).collect { result ->
            result.onSuccess { events ->
                recentEvents.value = events
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch recent circle events", error)
                recentActivityRenderer.renderError()
            }
        }
    }

    companion object {
        const val TAG: String = "CircleFragment"
    }
}
