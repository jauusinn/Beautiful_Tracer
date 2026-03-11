package com.beautifultracer.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beautifultracer.app.data.local.AppDatabase
import com.beautifultracer.app.data.local.entity.SearchHistoryEntity
import com.beautifultracer.app.data.remote.IpApiService
import com.beautifultracer.app.domain.model.HopNode
import com.beautifultracer.app.domain.model.WhoisInfo
import com.beautifultracer.app.domain.repository.TracerouteRepository
import com.beautifultracer.app.network.GeoIpResolver
import com.beautifultracer.app.network.PingExecutor
import com.beautifultracer.app.network.RootChecker
import com.beautifultracer.app.network.TracerouteExecutor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.InetAddress

/**
 * UI State for the Traceroute screen.
 * Contains all data needed to render the screen, including the current target,
 * discovered hops, and the state of the bottom WHOIS sheet.
 */
data class TracerouteUiState(
    val target: String = "",
    val hops: List<HopNode> = emptyList(),
    val isTracing: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val useRoot: Boolean = false,
    val isRootAvailable: Boolean = false,
    val resolvedIp: String? = null,

    // Whois bottom sheet state
    val selectedHopIp: String? = null,
    val whoisInfo: WhoisInfo? = null,
    val isLoadingWhois: Boolean = false,
    val whoisError: String? = null,
    val showWhoisSheet: Boolean = false,
    
    // Search history
    val isSearchFocused: Boolean = false
)

/**
 * ViewModel for the Traceroute feature.
 * Manages the lifecycle of traceroute and ping jobs, and handles data persistence
 * for search history and WHOIS caching.
 */
class TracerouteViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "TracerouteViewModel"

    private val _uiState = MutableStateFlow(TracerouteUiState())
    val uiState: StateFlow<TracerouteUiState> = _uiState.asStateFlow()

    // Database and API Dependencies
    private val database = AppDatabase.getInstance(application)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(IpApiService.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    private val ipApiService = retrofit.create(IpApiService::class.java)
    private val repository = TracerouteRepository(database.whoisCacheDao(), ipApiService)
    private val searchHistoryDao = database.searchHistoryDao()

    // Job management for long-running network tasks
    private var tracerouteJob: Job? = null
    private val pingJobs = mutableMapOf<String, Job>()
    
    // Special scope for continuous pings that survives individual traceroute restarts
    private val pingScope = viewModelScope + SupervisorJob()

    /**
     * Exposes the recent search history from the database as a StateFlow.
     * Automatically updates when the history changes.
     */
    val searchHistory: StateFlow<List<String>> = searchHistoryDao.getRecentSearchHistory()
        .map { history -> history.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        /**
         * Check if the device has root access to enable raw ICMP pings.
         */
        viewModelScope.launch {
            val rootAvailable = RootChecker.isRootAvailable()
            _uiState.update { it.copy(isRootAvailable = rootAvailable) }
        }

        /**
         * Clean up any expired WHOIS records from the local cache.
         */
        viewModelScope.launch {
            repository.cleanExpiredCache()
        }
    }

    /**
     * Updates the target IP or hostname in the UI state.
     * @param target The new target string entered by the user.
     */
    fun updateTarget(target: String) {
        _uiState.update { it.copy(target = target, error = null) }
    }

    /**
     * Toggles whether to use root access for the traceroute.
     */
    fun toggleRoot() {
        _uiState.update { it.copy(useRoot = !it.useRoot) }
    }

    /**
     * Handles focus changes on the search input to show/hide the history dropdown.
     * @param isFocused True if the input field gained focus.
     */
    fun onSearchFocusChange(isFocused: Boolean) {
        _uiState.update { it.copy(isSearchFocused = isFocused) }
    }

    /**
     * Initiates a new traceroute session.
     * Cleans up previous jobs, resolves the target hostname, and collects hops
     * from the TracerouteExecutor. Also initiates WHOIS lookups for discovered IPs.
     */
    fun startTraceroute() {
        val targetInput = _uiState.value.target.trim()
        if (targetInput.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a URL or IP address") }
            return
        }

        // Save valid queries to history
        viewModelScope.launch {
            searchHistoryDao.insertSearchQuery(SearchHistoryEntity(targetInput))
        }

        // Ensure any existing trace is stopped
        stopTraceroute()

        _uiState.update {
            it.copy(
                hops = emptyList(),
                isTracing = true,
                isCompleted = false,
                error = null,
                resolvedIp = null,
                isSearchFocused = false
            )
        }

        tracerouteJob = viewModelScope.launch {
            try {
                // Perform DNS resolution in a background thread
                val resolvedTarget = resolveTarget(targetInput)
                _uiState.update { it.copy(resolvedIp = resolvedTarget) }

                // Collect discovery events from the traceroute executor
                TracerouteExecutor.trace(
                    target = resolvedTarget,
                    useRoot = _uiState.value.useRoot
                ).collect { hop ->
                    // Enrich hop data with geolocation/country info if IP is valid
                    var hopWithFlag = if (!hop.isTimeout && GeoIpResolver.isValidIp(hop.ipAddress)) {
                        if (!GeoIpResolver.isPrivateIp(hop.ipAddress)) {
                            try {
                                val whois = repository.getWhoisInfo(hop.ipAddress).getOrNull()
                                val countryCode = whois?.countryCode
                                hop.copy(
                                    countryCode = countryCode,
                                    flagEmoji = GeoIpResolver.countryCodeToFlag(countryCode)
                                )
                            } catch (_: Exception) {
                                hop.copy(flagEmoji = "🌐")
                            }
                        } else {
                            hop.copy(flagEmoji = "🏠")
                        }
                    } else {
                        hop
                    }
                    
                    // If this is the destination hop, attach the original search target for display
                    if (hopWithFlag.isDestination) {
                        hopWithFlag = hopWithFlag.copy(displayAddress = targetInput)
                    }

                    // Append the discovered hop to the UI list
                    _uiState.update { state ->
                        state.copy(hops = state.hops + hopWithFlag)
                    }

                    // For non-timeout hops, start a continuous background ping to monitor latency
                    if (!hop.isTimeout && hop.ipAddress != "*" && hop.ipAddress != "X----X----X") {
                        startContinuousPing(hop.ipAddress, _uiState.value.hops.size - 1)
                    }
                }

                _uiState.update { it.copy(isTracing = false, isCompleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Traceroute failed", e)
                _uiState.update {
                    it.copy(
                        isTracing = false,
                        error = "Traceroute failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Cancels the active traceroute job and stops all continuous background pings.
     */
    fun stopTraceroute() {
        tracerouteJob?.cancel()
        tracerouteJob = null
        pingJobs.values.forEach { it.cancel() }
        pingJobs.clear()
        _uiState.update {
            it.copy(
                isTracing = false,
                hops = it.hops.map { hop -> hop.copy(isPinging = false) }
            )
        }
    }

    /**
     * Starts an indefinite ping loop for a specific discovered hop.
     * Updates the hop's latency and packet loss stats in real-time.
     * @param ipAddress The IP to monitor.
     * @param hopIndex The index of the hop in the current results list.
     */
    private fun startContinuousPing(ipAddress: String, hopIndex: Int) {
        if (pingJobs.containsKey(ipAddress)) return

        val job = pingScope.launch {
            var sent = 0
            var received = 0

            updateHopAtIndex(hopIndex) { it.copy(isPinging = true) }

            while (isActive) {
                try {
                    val result = PingExecutor.ping(ipAddress)
                    sent++
                    if (result.isReachable) received++

                    val loss = if (sent > 0) ((sent - received).toFloat() / sent) * 100f else 0f

                    // Update UI state with latest statistics
                    updateHopAtIndex(hopIndex) { hop ->
                        hop.copy(
                            latencyMs = if (result.isReachable) result.latencyMs else hop.latencyMs,
                            packetsSent = sent,
                            packetsReceived = received,
                            packetLoss = loss
                        )
                    }

                    delay(1000) // 1 second interval between pings
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    sent++
                    val loss = if (sent > 0) ((sent - received).toFloat() / sent) * 100f else 0f
                    updateHopAtIndex(hopIndex) { it.copy(packetsSent = sent, packetLoss = loss) }
                    delay(1000)
                }
            }
        }
        pingJobs[ipAddress] = job
    }

    /**
     * Helper function to perform a thread-safe update of a single hop in the state list.
     * @param index The position of the hop to update.
     * @param transform A function that takes the current hop and returns the updated one.
     */
    private fun updateHopAtIndex(index: Int, transform: (HopNode) -> HopNode) {
        _uiState.update { state ->
            if (index < state.hops.size) {
                val mutableHops = state.hops.toMutableList()
                mutableHops[index] = transform(mutableHops[index])
                state.copy(hops = mutableHops)
            } else {
                state
            }
        }
    }

    /**
     * Triggers a WHOIS data fetch for a specific IP.
     * Results are shown in the modal bottom sheet.
     * @param ipAddress The IP for which to retrieve detailed information.
     */
    fun fetchWhoisInfo(ipAddress: String) {
        if (ipAddress == "X----X----X") return

        _uiState.update {
            it.copy(
                selectedHopIp = ipAddress,
                showWhoisSheet = true,
                isLoadingWhois = true,
                whoisInfo = null,
                whoisError = null
            )
        }

        viewModelScope.launch {
            val result = repository.getWhoisInfo(ipAddress)
            result.fold(
                onSuccess = { info ->
                    _uiState.update {
                        it.copy(whoisInfo = info, isLoadingWhois = false)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            whoisError = error.message ?: "Failed to fetch IP details",
                            isLoadingWhois = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Closes the WHOIS bottom sheet and clears its state.
     */
    fun dismissWhoisSheet() {
        _uiState.update {
            it.copy(
                showWhoisSheet = false,
                selectedHopIp = null,
                whoisInfo = null,
                whoisError = null
            )
        }
    }

    /**
     * Deletes a specific item from the search history database.
     * @param query The search query to remove.
     */
    fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            searchHistoryDao.deleteSearchQuery(query)
        }
    }

    /**
     * Clears all search history from the database.
     */
    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryDao.clearHistory()
        }
    }

    /**
     * Resolves a user-provided hostname or URL to an IPv4 address.
     * Handles protocols, paths, and ports cleanup before resolution.
     * @param target The raw string input from the user.
     * @return The resolved IPv4 address string.
     */
    private suspend fun resolveTarget(target: String): String {
        if (isValidIpFormat(target)) return target

        // Cleanup: remove http, ports, and paths
        val cleaned = target
            .lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .split("/").first()
            .split(":").first()
            .trim()

        return withContext(Dispatchers.IO) {
            try {
                val addresses = InetAddress.getAllByName(cleaned)
                // Prefer IPv4 addresses for better compatibility with native ping
                val firstIpv4 = addresses.firstOrNull { it.hostAddress?.contains(".") == true }
                firstIpv4?.hostAddress ?: addresses.first().hostAddress ?: throw Exception("Host address is null")
            } catch (e: Exception) {
                Log.e(TAG, "DNS resolution failed for '$cleaned'", e)
                throw Exception("Could not resolve '$cleaned'. Check your connection.")
            }
        }
    }

    /**
     * Checks if a string matches the standard IPv4 format.
     * @param ip The string to check.
     */
    private fun isValidIpFormat(ip: String): Boolean {
        return ip.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
    }

    /**
     * Lifecycle hook: ensures all background jobs are stopped when the ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        stopTraceroute()
    }
}
