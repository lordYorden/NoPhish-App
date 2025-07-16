package dev.lordyorden.as_no_phish_detector.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PermsViewModel : ViewModel() {

    private val _granted = MutableLiveData<Int?>(null)
    val permGranted: LiveData<Int?> = _granted

    private val _rejected = MutableLiveData<Int?>(null)
    val permRejected: LiveData<Int?> = _rejected

    fun setGranted(permCode: Int) {
        _granted.postValue(permCode)
    }

    fun setRejected(permCode: Int) {
        _rejected.value = permCode
    }
}