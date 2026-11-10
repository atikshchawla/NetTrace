package com.example.data.telemetry

import android.net.TrafficStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ThroughputSample(
  val rxBytesPerSec: Long = 0L,
  val txBytesPerSec: Long = 0L
) {
  val totalBytesPerSec: Long get() = rxBytesPerSec + txBytesPerSec
}

/**
 * Manages foreground live network throughput sampling using standard TrafficStats counters.
 * Samples every 1 second while collected; immediately pauses when flow is cancelled.
 */
class LiveThroughputManager {

  private val _throughputFlow = MutableStateFlow(ThroughputSample())
  val throughputFlow: StateFlow<ThroughputSample> = _throughputFlow.asStateFlow()

  private var job: Job? = null

  fun start(scope: CoroutineScope, intervalMs: Long = 1000L) {
    if (job?.isActive == true) return
    job = scope.launch {
      liveThroughputFlow(intervalMs).collect {
        _throughputFlow.value = it
      }
    }
  }

  fun stop() {
    job?.cancel()
    job = null
    _throughputFlow.value = ThroughputSample()
  }

  fun liveThroughputFlow(intervalMs: Long = 1000L): Flow<ThroughputSample> = flow {
    var lastRx = TrafficStats.getTotalRxBytes()
    var lastTx = TrafficStats.getTotalTxBytes()
    var lastTime = System.currentTimeMillis()

    while (currentCoroutineContext().isActive) {
      delay(intervalMs)

      val nowTime = System.currentTimeMillis()
      val nowRx = TrafficStats.getTotalRxBytes()
      val nowTx = TrafficStats.getTotalTxBytes()

      val elapsedSec = ((nowTime - lastTime) / 1000.0).coerceAtLeast(0.5)

      val deltaRx = if (nowRx >= lastRx && lastRx != TrafficStats.UNSUPPORTED.toLong()) {
        ((nowRx - lastRx) / elapsedSec).toLong()
      } else {
        0L
      }

      val deltaTx = if (nowTx >= lastTx && lastTx != TrafficStats.UNSUPPORTED.toLong()) {
        ((nowTx - lastTx) / elapsedSec).toLong()
      } else {
        0L
      }

      lastRx = nowRx
      lastTx = nowTx
      lastTime = nowTime

      emit(ThroughputSample(rxBytesPerSec = deltaRx, txBytesPerSec = deltaTx))
    }
  }
}
