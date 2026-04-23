package dev.lordyorden.as_no_phish_detector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentAttackHistoryBinding


class AttackHistoryFragment : Fragment() {
    private lateinit var binding: FragmentAttackHistoryBinding

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
            searchView.setupWithSearchBar(searchBar)

            // Listen for query text changes
            searchView.editText.setOnEditorActionListener { _, _, _ ->
                val query = searchView.text.toString()
                searchBar.setText(query)
                searchView.hide()
                // Perform filtering logic here
                false
            }

            tvResultCount.text = getString(R.string.zero_result)
        }
    }
}