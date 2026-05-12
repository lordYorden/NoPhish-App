package dev.lordyorden.as_no_phish_detector.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentAttackHistoryBinding
import dev.lordyorden.as_no_phish_detector.ui.events.EventAdapter
import dev.lordyorden.as_no_phish_detector.ui.events.EventViewModel
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationStore
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days


class AttackHistoryFragment : Fragment() {
    private lateinit var binding: FragmentAttackHistoryBinding
    private lateinit var adapter: EventAdapter
    private val viewModel: EventViewModel by viewModels()
    private lateinit var localStore: MaliciousNotificationStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAttackHistoryBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {

        with(binding){
            localStore = MaliciousNotificationStore.getInstance()

            tvResultCount.text = getString(R.string.zero_result)

            chipGroupFilter.setOnCheckedStateChangeListener { chipGroup, _ ->
                when(chipGroup.checkedChipId){
                    R.id.chip_last_7 -> {
                        viewModel.startTime = Clock.System.now() - 7.days
                    }

                    R.id.chip_last_30 ->{
                        viewModel.startTime = Clock.System.now() - 30.days
                    }
                    R.id.chip_all_time -> {
                        viewModel.startTime = null
                    }
                }
            }

            setupRvResult()
            viewModel.loadNextPage()
        }
    }

    private fun setupRvResult() {
        adapter = EventAdapter{ event ->
            Log.d(TAG, "clicked on: $event")

            viewLifecycleOwner.lifecycleScope.launch {
                val details = localStore.getValidated(event.eventId, event.contentHash)
                if (details == null) {
                    Toast.makeText(
                        requireContext(),
                        "Details unavailable on this device",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val client = requireActivity() as ClientActivity
                client.showDetailsBottomSheet(details)
            }
        }

        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter

        lifecycleScope.launch {
            viewModel.events.collect { events ->

                Log.d(TAG, "got events, size: ${events.size}")
                events.forEach { event ->
                    Log.d(TAG, "got event: $event")
                }
                adapter.updateData(events)

                binding.tvResultCount.text = buildString {
                    append(adapter.itemCount)
                    append(" results")
                }
            }
        }


        binding.rvSearchResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Only react when user stops scrolling
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return

                if (!viewModel.isLoading && lastVisibleItem >= totalItemCount - 2) {
                    viewModel.loadNextPage()
                }
            }
        })


    }

    companion object {
        const val TAG = "AttackHistoryFragment"
    }
}
