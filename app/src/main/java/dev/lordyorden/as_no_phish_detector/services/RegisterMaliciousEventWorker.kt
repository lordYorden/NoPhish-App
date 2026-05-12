package dev.lordyorden.as_no_phish_detector.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clerk.api.Clerk
import dev.lordyorden.as_no_phish_detector.models.RelentNotificationInfo
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationPayloadParser

class RegisterMaliciousEventWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val payloadJson = inputData.getString(KEY_PAYLOAD_JSON)
            ?: run {
                Log.e(TAG, "Missing required worker input: $KEY_PAYLOAD_JSON")
                return Result.failure()
            }

        val payload = runCatching {
            MaliciousNotificationPayloadParser.parse(payloadJson)
        }.getOrElse { error ->
            Log.e(TAG, "Failed to parse malicious notification worker payload", error)
            return Result.failure()
        }

        return registerMaliciousEventIfSource(payload)
    }

    private suspend fun registerMaliciousEventIfSource(payload: RelentNotificationInfo): Result {
        val currentUserId = Clerk.activeUser?.id
        if (currentUserId.isNullOrBlank()) {
            Log.w(TAG, "Skipping Convex malicious event upload: missing active user for eventId=${payload.eventId}")
            return Result.success()
        }

        if (currentUserId != payload.sourceUserId) {
            Log.i(TAG, "Skipping Convex malicious event upload on receiving device for eventId=${payload.eventId}")
            return Result.success()
        }

        val circleId = payload.circleId ?: getCurrentCircleId() ?: return Result.retry()
        if (circleId.isBlank() || circleId == Constants.Onboarding.ACTION_GENERATE) {
            Log.w(TAG, "Skipping Convex malicious event upload: missing circleId for eventId=${payload.eventId}")
            return Result.success()
        }

        val args = mutableMapOf<String, Any?>(
            "circleId" to circleId,
            "timestamp" to payload.timestamp.toDouble(),
            "action" to "malicious notification",
            "eventId" to payload.eventId,
            "contentHash" to payload.contentHash,
            "packageName" to payload.packageName,
        )

        return runCatching {
            ConvexHelper.getInstance().convexClient.mutation<String>("events:register", args)
        }.fold(
            onSuccess = {
                Log.i(TAG, "Registered malicious notification metadata in Convex for eventId=${payload.eventId}")
                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to register malicious notification metadata in Convex", error)
                Result.retry()
            }
        )
    }

    private suspend fun getCurrentCircleId(): String? {
        return runCatching {
            ConvexHelper.getInstance().convexClient.mutation<String>("circles:get_my_circles")
        }.getOrElse { error ->
            Log.e(TAG, "Failed to resolve current circle for malicious event upload", error)
            null
        }
    }

    companion object {
        private const val TAG = "RegisterMaliciousEventWorker"
        const val KEY_PAYLOAD_JSON = "payloadJson"
    }
}
