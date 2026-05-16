package dev.lordyorden.as_no_phish_detector.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentCircleEventsBinding
import dev.lordyorden.as_no_phish_detector.ui.events.CircleEventAdapter
import dev.lordyorden.as_no_phish_detector.ui.events.CircleEventsViewModel
import dev.lordyorden.as_no_phish_detector.ui.events.HistoryLoading
import dev.lordyorden.as_no_phish_detector.ui.events.HistoryUiState
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationStore
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class CircleEventsFragment : Fragment() {

    private lateinit var binding: FragmentCircleEventsBinding
    private lateinit var adapter: CircleEventAdapter
    private lateinit var localStore: MaliciousNotificationStore
    private val viewModel: CircleEventsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCircleEventsBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        localStore = MaliciousNotificationStore.getInstance()
        adapter = CircleEventAdapter(
            onDetailsClick = { event ->
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
        )

        binding.rvCircleEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCircleEvents.adapter = adapter

        binding.btnCircleEventsRetry.setOnClickListener {
            viewModel.retry()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { chipGroup, _ ->
            when (chipGroup.checkedChipId) {
                R.id.chip_last_7 -> viewModel.startTime = Clock.System.now() - 7.days
                R.id.chip_last_30 -> viewModel.startTime = Clock.System.now() - 30.days
                R.id.chip_all_time -> viewModel.startTime = null
            }
        }

        binding.rvCircleEvents.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (lastVisibleItem >= totalItemCount - 2) {
                    viewModel.loadNextPage()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }

        val circleId = arguments?.getString(Constants.Circle.CIRCLE_ID_KEY)
            ?: requireActivity().intent.extras?.getString(Constants.Circle.CIRCLE_ID_KEY)
        if (circleId.isNullOrBlank()) {
            Log.e(TAG, "CircleEventsFragment opened without a valid circleId")
            renderContractError()
            return
        }

        viewModel.start(circleId)
    }

    private fun render(state: HistoryUiState) {
        val showInitialLoading = state.loading == HistoryLoading.Initial && state.events.isEmpty()
        val showInitialError = state.events.isEmpty() && state.errorMessage != null
        val showEmpty = state.loading == HistoryLoading.Idle &&
            state.events.isEmpty() &&
            state.errorMessage == null
        val showState = showInitialLoading || showInitialError || showEmpty

        binding.layoutCircleEventsState.isVisible = showState
        binding.rvCircleEvents.isVisible = !showState
        binding.progressCircleEvents.isVisible = showInitialLoading
        binding.btnCircleEventsRetry.isVisible = showInitialError
        binding.tvCircleEventsState.text = when {
            showInitialLoading -> getString(R.string.circle_events_loading)
            showInitialError -> getString(R.string.circle_events_error)
            showEmpty -> getString(R.string.circle_events_empty)
            else -> ""
        }

        binding.tvResultCount.text = resources.getQuantityString(
            R.plurals.history_results_plural,
            state.events.size,
            state.events.size
        )

        adapter.submitList(state.events)
    }

    private fun renderContractError() {
        binding.layoutCircleEventsState.isVisible = true
        binding.rvCircleEvents.isVisible = false
        binding.progressCircleEvents.isVisible = false
        binding.btnCircleEventsRetry.isVisible = false
        binding.tvCircleEventsState.text = getString(R.string.circle_event_contract_error)
        binding.tvResultCount.text = getString(R.string.zero_result)
    }

    companion object {
        private const val TAG = "CircleEventsFragment"
    }
}
