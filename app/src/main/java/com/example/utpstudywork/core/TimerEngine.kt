package com.example.utpstudywork.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var remaining = 0
    private val _ticks = MutableSharedFlow<Int>(replay = 1)
    val ticks: SharedFlow<Int> = _ticks

    fun start(totalSeconds: Int): SharedFlow<Int> {
        stop()
        remaining = totalSeconds
        job = scope.launch {
            while (remaining >= 0 && isActive) {
                _ticks.emit(remaining)
                delay(1000)
                remaining--
            }
        }
        return ticks
    }

    fun pause() { job?.cancel(); job = null }
    fun stop() { job?.cancel(); job = null; remaining = 0 }
}
