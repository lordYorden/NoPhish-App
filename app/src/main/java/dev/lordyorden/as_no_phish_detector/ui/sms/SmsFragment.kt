package dev.lordyorden.as_no_phish_detector.ui.sms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.lordyorden.as_no_phish_detector.adapters.SmsAdapter
import dev.lordyorden.as_no_phish_detector.databinding.FragmentSmsBinding
import dev.lordyorden.as_no_phish_detector.models.SmsMessage

class SmsFragment : Fragment() {

    private lateinit var binding: FragmentSmsBinding

    val smsViewModel: SMSViewModel by activityViewModels()
    private lateinit var adapter: SmsAdapter
    private val smsList: MutableList<SmsMessage> = mutableListOf()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSmsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        smsViewModel.lifecycle = lifecycleScope
        initViews()
        return root
    }

    private fun initViews() {

        binding.btnFetch.setOnClickListener {
            smsViewModel.checkNewNotifications(smsList)
        }

        smsViewModel.newNotif.observe(viewLifecycleOwner) { notifications->
            if(notifications == null)
                return@observe

            notifications.forEach { sms ->
                addSMS(sms)
            }
        }

        smsViewModel.removeNotif.observe(viewLifecycleOwner) { notifications->
            if(notifications == null)
                return@observe

            notifications.forEach { sms ->
                removeSMS(sms)
            }
        }


        binding.rvSms.layoutManager = LinearLayoutManager(requireActivity())

        adapter = SmsAdapter(smsList) { notif ->

            //Toast.makeText(this, "title: ${notif.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvSms.adapter = adapter
    }

    private fun removeSMS(sms: SmsMessage) {
        val notifIdx = smsList.indexOfFirst { n -> (n.id == sms.id) }
        if(notifIdx != -1) {
            smsList.remove(sms)
            adapter.notifyItemRemoved(notifIdx)
        }
    }

    private fun addSMS(sms: SmsMessage) {
        val notifIdx = smsList.size
        smsList.add(sms)
        adapter.notifyItemChanged(notifIdx)
    }
}