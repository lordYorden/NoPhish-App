package dev.lordyorden.as_no_phish_detector.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.adapters.CircleAdapter
import dev.lordyorden.as_no_phish_detector.adapters.EventPreviewAdapter
import dev.lordyorden.as_no_phish_detector.databinding.FragmentCircleBinding
import dev.lordyorden.as_no_phish_detector.databinding.SectionRecentActivityBinding
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.models.PaginationResult
import dev.lordyorden.as_no_phish_detector.repositories.CircleMembersRepository
import dev.lordyorden.as_no_phish_detector.ui.events.CircleRecentActivityRenderer
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationStore
import kotlinx.coroutines.launch

class CircleFragment : Fragment() {

    private lateinit var binding: FragmentCircleBinding
    private lateinit var recentBinding: SectionRecentActivityBinding
    private lateinit var circleAdapter: CircleAdapter
    private lateinit var eventPreviewAdapter: EventPreviewAdapter
    private lateinit var recentActivityRenderer: CircleRecentActivityRenderer
    private lateinit var localStore: MaliciousNotificationStore
    private var recentEvents: List<Event> = emptyList()

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
        setUpRecentActivity()
        setupBottomSheet()
        binding.btnAdd.setOnClickListener {

            val bundle = Bundle().apply {
                putString(Constants.Circle.CIRCLE_ID_KEY, circleId)
            }

            findNavController().navigate(R.id.action_nev_circle_to_invite_fragment, bundle)
        }


    }

    private fun setUpRecentActivity() {
        recentBinding = binding.sectionRecent
        recentBinding.rvEvents.layoutManager = LinearLayoutManager(requireContext()).apply {
            orientation = RecyclerView.HORIZONTAL
        }

        eventPreviewAdapter = EventPreviewAdapter { event ->
            Log.d(TAG, "event clicked $event")
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                getRecentCircleEvents()
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

        CircleMembersRepository.getInstance().observe(circleId)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                CircleMembersRepository.getInstance().observe(circleId).collect { state ->
                    state.errorMessage?.let { message ->
                        Log.e(TAG, "Failed to fetch members: $message")
                    }
                    circleAdapter.submitList(state.members)
                    renderRecentActivity()
                }
            }
        }
    }

    private fun getCircleId() {
        val extra = requireActivity().intent.extras
        circleId = extra?.getString(Constants.Circle.CIRCLE_ID_KEY, Constants.Circle.CIRCLE_TEMP_ID) ?: Constants.Circle.CIRCLE_TEMP_ID
        Log.d(TAG, "got circleId: $circleId")
    }

    private suspend fun getRecentCircleEvents() {
        val client = ConvexHelper.getInstance().convexClient
        val args = mapOf(
            "circleId" to circleId,
            "paginationOpts" to mapOf(
                "cursor" to null,
                "numItems" to 5f
            )
        )

        client.subscribe<PaginationResult<Event>>("events:get_by_circle", args).collect { result ->
            result.onSuccess { page ->
                recentEvents = page.page
                renderRecentActivity()
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch recent circle events", error)
                recentActivityRenderer.renderError()
            }
        }
    }

    private fun renderRecentActivity() {
        recentActivityRenderer.render(
            events = recentEvents,
            membersState = CircleMembersRepository.getInstance().currentState(circleId),
        )
    }



    companion object {
        const val TAG: String = "CircleFragment"
    }
}
