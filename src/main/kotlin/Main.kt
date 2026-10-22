package com.riokartus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ServerMonitor {
    private val _currentCpu = MutableStateFlow(0)
    val currentCpu: StateFlow<Int> = _currentCpu
    fun updateCpu(value: Int) {
        _currentCpu.value = value
    }
}

class AlertSystem {
    private val _alerts = MutableSharedFlow<String>()
    val alerts: SharedFlow<String> = _alerts
    suspend fun sendAlert(message: String) {
        _alerts.emit(message)
    }
}

fun cpuMetrics(): Flow<Int> = flow {
    for (i in 1..10) {
        delay(200)
        emit(i * 20)
        println(Thread.currentThread().name)
    }

}.flowOn(Dispatchers.Default)

fun highCpuMetrics(): Flow<Int> {
    return cpuMetrics()
        .map { it + 5 }
        .filter { it > 50 }
        .flowOn(Dispatchers.Default)
}

fun main() = runBlocking {
    val alertSystem = AlertSystem()
    val server = ServerMonitor()
    launch { alertSystem.alerts.collect { println("ALERT: $it") }
    }
    delay(100)
    alertSystem.sendAlert("CPU > 80%")
    delay(100)
    alertSystem.sendAlert("Memory > 90%")
    launch { server.currentCpu.collect { println("CPU: $it") }
    }
    delay(100)
    server.updateCpu(30)
    delay(100)
    server.updateCpu(100)
    delay(100)
    server.updateCpu(70)
    delay(100)
    server.updateCpu(100)
    delay(100)
    server.updateCpu(90)
    delay(500)
    highCpuMetrics().collect { println(it) }
    delay(100)
    val job = launch { highCpuMetrics().collect {
            server.updateCpu(it)
            if (it > 80) {
                alertSystem.sendAlert("High CPU: $it%")
            }
        }
    }
    job.join()
}










