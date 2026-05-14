package com.pulselog.domain

import com.pulselog.data.NotificationSettings

interface NotificationScheduler {
    suspend fun apply(settings: NotificationSettings)
    suspend fun cancelAll()
}

class NoOpNotificationScheduler : NotificationScheduler {
    override suspend fun apply(settings: NotificationSettings) = Unit

    override suspend fun cancelAll() = Unit
}
