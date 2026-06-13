package dev.lordyorden.as_no_phish_detector.utilities

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

object EncryptedDataStoreFactory {
    private const val MASTER_KEY_URI = "android-keystore://nophish_datastore_master_key"
    private const val KEYSET_NAME = "nophish_datastore_keyset"
    private const val KEYSET_PREFS_NAME = "nophish_datastore_keyset_prefs"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun <T> create(
        context: Context,
        fileName: String,
        serializer: Serializer<T>
    ): DataStore<T> {
        val appContext = context.applicationContext
        return DataStoreFactory.create(
            serializer = AeadSerializer(
                aead = createAead(appContext),
                wrappedSerializer = serializer,
                associatedData = fileName.encodeToByteArray()
            ),
            scope = scope,
            produceFile = { File(appContext.filesDir, "datastore/$fileName") }
        )
    }

    private fun createAead(context: Context): Aead {
        AeadConfig.register()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_NAME)
            .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(
            RegistryConfiguration.get(),
            Aead::class.java
        )
    }
}
