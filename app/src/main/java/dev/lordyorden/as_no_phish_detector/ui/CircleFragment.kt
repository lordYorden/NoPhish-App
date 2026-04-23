package dev.lordyorden.as_no_phish_detector.ui

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.adapters.CircleAdapter
import dev.lordyorden.as_no_phish_detector.adapters.EventAdapter
import dev.lordyorden.as_no_phish_detector.databinding.FragmentCircleBinding
import dev.lordyorden.as_no_phish_detector.databinding.SectionRecentActivityBinding
import dev.lordyorden.as_no_phish_detector.models.CircleMember
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import kotlinx.coroutines.launch

class CircleFragment : Fragment() {

    private lateinit var binding: FragmentCircleBinding
    private lateinit var recentBinding: SectionRecentActivityBinding
    private val circleMembers: MutableList<CircleMember> = mutableListOf()

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

        getCircleId()
        setupBottomSheet()
        setUpRecentActivity()
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

        val recentList: List<Event> = listOf(
            Event("yarden", 54556, "test"),
            Event("itay", 54556, "test"),
            Event("shay", 54556, "test")
        )

        val adapter = EventAdapter(recentList) { event ->
            Log.d("CircleFragment", "event clicked $event")
        }

        recentBinding.rvEvents.adapter = adapter
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

        val adapter = CircleAdapter(circleMembers) { member ->
            Log.d("CircleFragment", "member clicked $member")
        }

        binding.rvCircle.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                getCircleMembers()
            }
        }
    }

    private fun getCircleId() {
        val extra = requireActivity().intent.extras
        circleId = extra?.getString(Constants.Circle.CIRCLE_ID_KEY, Constants.Circle.CIRCLE_TEMP_ID) ?: Constants.Circle.CIRCLE_TEMP_ID
        Log.d(TAG, "got circleId: $circleId")
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun getCircleMembers() {
        val client = ConvexHelper.getInstance().convexClient
        client.subscribe<List<CircleMember>>("members:get", mapOf(
            "circleId" to circleId
        )).collect { result ->
            result.onSuccess { members ->
                Log.d(TAG, "Received ${members.size} members")
                circleMembers.clear()
                members.forEach { member ->
                    circleMembers.add(member)
                    Log.d(TAG, "name: ${member.name}, role: ${member.familyRole}")
                }
                requireActivity().runOnUiThread {
                    binding.rvCircle.adapter?.notifyDataSetChanged()
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch members", error)
            }
        }
    }



    companion object {
        const val TAG: String = "CircleFragment"
    }
}