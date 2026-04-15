package dev.lordyorden.as_no_phish_detector.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.adapters.CircleAdapter
import dev.lordyorden.as_no_phish_detector.databinding.FragmentCircleBinding
import dev.lordyorden.as_no_phish_detector.databinding.SectionCloseCircleBinding
import dev.lordyorden.as_no_phish_detector.models.CircleMember
import dev.lordyorden.as_no_phish_detector.models.Member
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper

class CircleFragment : Fragment() {

    private lateinit var binding: FragmentCircleBinding
    private lateinit var circleBinding: SectionCloseCircleBinding
    private val circleMembers: List<CircleMember> = listOf(
        CircleMember("itay", "nephew", false, "none", "hello"),
        CircleMember("yarden", "nephew", true, "none", "hello1"),
    )

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
            findNavController().navigate(R.id.action_nev_circle_to_invite_fragment)
        }

    }

    suspend fun getCircleMembers(){
        val client = ConvexHelper.getInstance().convexClient
        client.subscribe<List<Member>>("members:get").collect { result ->
            result.onSuccess { members ->
                Log.d(TAG, "Received ${members.size} members")
                members.forEach{ member->
                    requireActivity().runOnUiThread {
                        //TODO fin function
                    }
                    Log.d(TAG, "name: ${member.name}, role: ${member.familyRole}")
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to fetch members", error)
            }
        }
    }

    companion object{
        const val TAG: String = "CircleFragment"
    }
}