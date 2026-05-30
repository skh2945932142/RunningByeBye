package com.runningbyebye.app

sealed class RunStatus {
    object Idle : RunStatus()
    data class Running(val fieldName: String) : RunStatus()
    data class Progress(val submitted: Int, val total: Int, val mileage: Double) : RunStatus()
    data class Completed(val mileage: Double) : RunStatus()
    data class Failed(val message: String) : RunStatus()
    data class Stopped(val message: String = "已停止") : RunStatus()
}

fun interface RunStatusObserver {
    fun onChanged(status: RunStatus)
}

object RunServiceState {
    private val lock = Any()
    private val observers = LinkedHashSet<RunStatusObserver>()
    private var latest: RunStatus = RunStatus.Idle

    fun current(): RunStatus = synchronized(lock) { latest }

    fun observe(observer: RunStatusObserver) {
        val snapshot = synchronized(lock) {
            observers += observer
            latest
        }
        observer.onChanged(snapshot)
    }

    fun removeObserver(observer: RunStatusObserver) {
        synchronized(lock) {
            observers -= observer
        }
    }

    fun update(status: RunStatus) {
        val snapshot = synchronized(lock) {
            latest = status
            observers.toList()
        }
        snapshot.forEach { observer -> observer.onChanged(status) }
    }

    fun resetForTest() {
        synchronized(lock) {
            latest = RunStatus.Idle
            observers.clear()
        }
    }
}
