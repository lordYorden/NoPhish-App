package dev.lordyorden.as_no_phish_detector.services

import android.os.Bundle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MessageBridge {
    private val _messageFlow = MutableSharedFlow<Bundle>(
        extraBufferCapacity = 64, replay = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow = _messageFlow.asSharedFlow()

    fun sendMessage(bundle: Bundle) {
        _messageFlow.tryEmit(bundle)
    }
}