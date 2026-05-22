package dev.lordyorden.as_no_phish_detector.repositories

import android.util.Log
import dev.convex.android.ConvexClient
import dev.lordyorden.as_no_phish_detector.models.CircleMember
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CircleMembersRepository private constructor(
    private val client: ConvexClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val states = mutableMapOf<String, MutableStateFlow<CircleMembersState>>()
    private val subscriptionJobs = mutableMapOf<String, Job>()

    fun observe(circleId: String): StateFlow<CircleMembersState> {
        require(circleId.isNotBlank()) { "circleId must not be blank" }

        val state = stateFor(circleId)
        ensureSubscription(circleId, state)
        return state.asStateFlow()
    }

    fun currentState(circleId: String): CircleMembersState {
        require(circleId.isNotBlank()) { "circleId must not be blank" }

        return states[circleId]?.value
            ?: throw IllegalStateException("Circle members must be observed before reading current state for circleId=$circleId")
    }

    fun requireMember(circleId: String, userId: String): CircleMember {
        require(circleId.isNotBlank()) { "circleId must not be blank" }
        require(userId.isNotBlank()) { "userId must not be blank" }

        return stateFor(circleId).value.membersByUserId[userId]
            ?: throw IllegalStateException("Missing circle member for circleId=$circleId")
    }

    suspend fun clearAll() {
        val jobs = subscriptionJobs.values.toList()
        val retainedStates = states.values.toList()

        subscriptionJobs.clear()
        states.clear()

        jobs.forEach { job ->
            job.cancelAndJoin()
        }
        retainedStates.forEach { state ->
            state.value = CircleMembersState(circleId = state.value.circleId)
        }
    }

    private fun stateFor(circleId: String): MutableStateFlow<CircleMembersState> {
        return states.getOrPut(circleId) {
            MutableStateFlow(CircleMembersState(circleId = circleId))
        }
    }

    private fun ensureSubscription(
        circleId: String,
        state: MutableStateFlow<CircleMembersState>,
    ) {
        if (subscriptionJobs[circleId]?.isActive == true) return

        subscriptionJobs[circleId] = scope.launch {
            try {
                client.subscribe<List<CircleMember>>(
                    "members:get",
                    mapOf("circleId" to circleId)
                ).collect { result ->
                    result.onSuccess { members ->
                        state.value = CircleMembersState(
                            circleId = circleId,
                            members = members,
                            membersByUserId = mapMembersByUserId(circleId, members),
                            loaded = true,
                        )
                    }.onFailure { error ->
                        publishError(circleId, state, error)
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                publishError(circleId, state, error)
            }
        }
    }

    private fun mapMembersByUserId(
        circleId: String,
        members: List<CircleMember>,
    ): Map<String, CircleMember> {
        val membersByUserId = members.associateBy { member ->
            require(member.userId.isNotBlank()) { "member userId must not be blank for circleId=$circleId" }
            member.userId
        }

        if (membersByUserId.size != members.size) {
            throw IllegalStateException("Duplicate circle member userId for circleId=$circleId")
        }

        return membersByUserId
    }

    private fun publishError(
        circleId: String,
        state: MutableStateFlow<CircleMembersState>,
        error: Throwable,
    ) {
        val message = error.message ?: error::class.java.simpleName
        Log.e(TAG, "circle members subscription failed for circleId=$circleId", error)
        state.value = state.value.copy(errorMessage = message)
    }

    companion object {
        private const val TAG = "CircleMembersRepository"

        @Volatile
        private var instance: CircleMembersRepository? = null

        fun getInstance(): CircleMembersRepository {
            return instance
                ?: throw IllegalStateException("CircleMembersRepository must be initialized by calling init() before use")
        }

        fun init(): CircleMembersRepository {
            return instance ?: synchronized(this) {
                instance ?: CircleMembersRepository(ConvexHelper.getInstance().convexClient).also {
                    instance = it
                }
            }
        }
    }
}
