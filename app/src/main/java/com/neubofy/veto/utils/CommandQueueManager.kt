package com.neubofy.veto.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CommandQueueManager {
    private const val TAG = "CommandQueueManager"
    private val mediaMutex = Mutex()

    /**
     * Executes a media action (photo, video, audio capture) under a global lock
     * to guarantee that commands execute sequentially and never conflict over camera/microphone hardware.
     */
    suspend fun <T> runMediaCommandInQueue(action: suspend () -> T): T {
        return mediaMutex.withLock {
            action()
        }
    }
}
