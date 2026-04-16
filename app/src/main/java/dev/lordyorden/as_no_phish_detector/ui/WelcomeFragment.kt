package dev.lordyorden.as_no_phish_detector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentWelcomeBinding


class WelcomeFragment : Fragment() {
    private lateinit var binding: FragmentWelcomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }




    private fun initViews() {

        binding.btnSetup.setOnClickListener{
            findNavController().navigate(R.id.action_welcomeFragment_to_circleCreationFragment)
        }

        binding.btnJoin.setOnClickListener{
            findNavController().navigate(R.id.action_welcomeFragment_to_joinCircleFragment)
        }
    }

}