package dev.lordyorden.as_no_phish_detector.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.adapters.CircleAdapter
import dev.lordyorden.as_no_phish_detector.databinding.FragmentCircleBinding
import dev.lordyorden.as_no_phish_detector.databinding.SectionCloseCircleBinding
import dev.lordyorden.as_no_phish_detector.models.CircleMember
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import kotlinx.coroutines.launch

class CircleFragment : Fragment() {

    private lateinit var binding: FragmentCircleBinding
    private lateinit var circleBinding: SectionCloseCircleBinding
    private val circleMembers: MutableList<CircleMember> = mutableListOf(
    )

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
        circleBinding = binding.llcCircle

        circleBinding.rvCircle.layoutManager = LinearLayoutManager(requireActivity())

        val adapter = CircleAdapter(circleMembers) { member ->
            Log.d("CircleFragment", "member clicked $member")
        }

        circleBinding.rvCircle.adapter = adapter

        circleBinding.btnAdd.setOnClickListener {

            val bundle = Bundle().apply {
                putString("circleId", circleId)
            }

            findNavController().navigate(R.id.action_nev_circle_to_invite_fragment, bundle)
        }

        getCircleId()

        lifecycleScope.launch {
            getCircleMembers()
        }
    }

    private fun getCircleId() {
        val extra = requireActivity().intent.extras
        circleId = extra?.getString("circleId", "test_circle") ?: "test_circle"
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
                    circleBinding.rvCircle.adapter?.notifyDataSetChanged()
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