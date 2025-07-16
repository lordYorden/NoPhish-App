package dev.lordyorden.as_no_phish_detector.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.models.PermissionRequest
import dev.lordyorden.as_no_phish_detector.databinding.FragmentSettingsBinding
import dev.lordyorden.as_no_phish_detector.utilities.Constants

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    val permsViewModel: PermsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initViews()
        return root
    }

    
    
    private fun initViews() {
        binding.postNotifMs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                requestPerm("android.permission.POST_NOTIFICATIONS", Constants.Perms.POST_NOTIFICATION_CODE)

        }

        binding.smsMs.setOnCheckedChangeListener { _, isChecked ->
            requestPerm("android.permission.READ_SMS", Constants.Perms.READ_SMS_CODE)
        }

        binding.readNotifMs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                requestPerm("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE", Constants.Perms.READ_NOTIFICATION_CODE)
        }

        permsViewModel.permGranted.observe(viewLifecycleOwner) { granted->
            if(granted == null)
                return@observe

            activatePerm(granted)
        }

        permsViewModel.permRejected.observe(viewLifecycleOwner) { reject->
            if(reject == null)
                return@observe

            deniedPerm(reject)
        }

    }

    override fun onResume() {
        super.onResume()
        if (EasyPermissions.hasPermissions(requireActivity(), "android.permission.POST_NOTIFICATIONS")){
            activatePerm(Constants.Perms.POST_NOTIFICATION_CODE)
        }

        if (EasyPermissions.hasPermissions(requireActivity(), "android.permission.READ_SMS")){
            activatePerm(Constants.Perms.READ_SMS_CODE)
        }

        if (EasyPermissions.hasPermissions(requireActivity(), "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")){
            activatePerm(Constants.Perms.READ_NOTIFICATION_CODE)
        }
    }



    private fun requestPerm(perm: String, code: Int){

        if (EasyPermissions.hasPermissions(requireActivity(), perm)){
            activatePerm(code)
            return
        }

        if (EasyPermissions.permissionPermanentlyDenied(requireActivity(), perm)){
            deniedPerm(code)
            return
        }

        val request = PermissionRequest.Builder(requireActivity())
            .code(code)
            .perms(arrayOf(perm))
/*            .theme(R.style.my_fancy_style)
            .rationale(R.string.camera_and_location_rationale)
            .positiveButtonText(R.string.rationale_ask_ok)
            .negativeButtonText(R.string.rationale_ask_cancel)*/
            .build()
        EasyPermissions.requestPermissions(requireActivity(), request)
    }

    fun activatePerm(requestCode: Int) {
        when(requestCode){
            Constants.Perms.POST_NOTIFICATION_CODE -> binding.postNotifMs.isChecked = true
            Constants.Perms.READ_SMS_CODE -> binding.smsMs.isChecked = true
            Constants.Perms.READ_NOTIFICATION_CODE -> binding.readNotifMs.isChecked = true
        }


    }

    fun deniedPerm(requestCode: Int) {
        when(requestCode) {
            Constants.Perms.POST_NOTIFICATION_CODE,Constants.Perms.READ_SMS_CODE -> {
                startActivity(Intent(ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireActivity().packageName, null)
                ))
                binding.postNotifMs.isChecked = false
                binding.smsMs.isChecked = false
            }
            Constants.Perms.READ_NOTIFICATION_CODE -> {
                startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
                binding.readNotifMs.isChecked = true
            }
        }
    }
}